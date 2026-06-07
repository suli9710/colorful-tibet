import { afterEach, describe, expect, it, vi } from 'vitest'
import { generateRouteStream, streamRouteGenerationJob } from './stream'

const encoder = new TextEncoder()

function streamResponse(chunks: string[], init?: ResponseInit): Response {
  return new Response(new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk))
      }
      controller.close()
    }
  }), {
    status: 200,
    headers: { 'Content-Type': 'text/event-stream' },
    ...init
  })
}

function stubFetch(response: Response | Promise<Response>) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

describe('route generation streams', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('skips malformed events and completes on a done event', async () => {
    stubFetch(streamResponse([
      'data: not-json\n\n',
      'data: {"type":"delta","text":"hello"}\n\n',
      'event: done\ndata: {"content":"hello final"}\n\n'
    ]))

    const deltas: Array<[string, string]> = []
    const done = vi.fn()
    const errors = vi.fn()

    await generateRouteStream(
      { days: 3, budget: 'comfort', preference: 'natural' },
      {
        onDelta: (text, fullText) => deltas.push([text, fullText]),
        onDone: done,
        onError: errors
      }
    )

    expect(deltas).toEqual([['hello', 'hello']])
    expect(done).toHaveBeenCalledWith('hello final')
    expect(errors).not.toHaveBeenCalled()
  })

  it('handles SSE error events without reading more frames', async () => {
    stubFetch(streamResponse([
      'event: error\ndata: {"message":"provider failed"}\n\n',
      'data: {"type":"delta","text":"ignored"}\n\n'
    ]))

    const deltas = vi.fn()
    const errors = vi.fn()

    await generateRouteStream(
      { days: 3, budget: 'comfort', preference: 'natural' },
      {
        onDelta: deltas,
        onError: errors
      }
    )

    expect(errors).toHaveBeenCalledWith('provider failed')
    expect(deltas).not.toHaveBeenCalled()
  })

  it('reports a readable body boundary error through the stream callback', async () => {
    stubFetch(new Response(null, {
      status: 200,
      headers: { 'Content-Type': 'text/event-stream' }
    }))

    const errors = vi.fn()

    await streamRouteGenerationJob('job-1', { onError: errors })

    expect(errors).toHaveBeenCalledWith(expect.stringContaining('readable body'))
  })

  it('silently returns when the fetch is aborted', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new DOMException('Aborted', 'AbortError')))

    const errors = vi.fn()

    await streamRouteGenerationJob('job-1', { onError: errors })

    expect(errors).not.toHaveBeenCalled()
  })

  it('preserves HTTP 429 as a rejected rate-limit error', async () => {
    stubFetch(new Response('slow down', { status: 429 }))

    await expect(streamRouteGenerationJob('job-1', {})).rejects.toThrow('slow down')
  })

  it('applies route job snapshots and cumulative deltas', async () => {
    stubFetch(streamResponse([
      'data: {"type":"snapshot","payload":{"jobId":"job-1","status":"RUNNING","content":"base","days":3,"budget":"comfort","preference":"natural","cached":false,"createdAt":"2026-06-08T00:00:00Z","updatedAt":"2026-06-08T00:00:00Z"}}\n\n',
      'data: {"type":"delta","text":"base plus"}\n\n',
      'data: {"type":"done"}\n\n'
    ]))

    const snapshots = vi.fn()
    const deltas: Array<[string, string]> = []
    const done = vi.fn()

    await streamRouteGenerationJob('job-1', {
      onSnapshot: snapshots,
      onDelta: (text, fullText) => deltas.push([text, fullText]),
      onDone: done
    })

    expect(snapshots).toHaveBeenCalledWith(expect.objectContaining({
      jobId: 'job-1',
      status: 'RUNNING',
      content: 'base'
    }))
    expect(deltas).toEqual([['base plus', 'base plus']])
    expect(done).toHaveBeenCalledWith('base plus')
  })
})
