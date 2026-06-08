import { afterEach, describe, expect, it, vi } from 'vitest'
import { generateRouteStream, startRouteGenerationJob, streamRouteGenerationJob } from './stream'

const encoder = new TextEncoder()

class MemoryStorage implements Storage {
  private values = new Map<string, string>()

  get length() {
    return this.values.size
  }

  clear() {
    this.values.clear()
  }

  getItem(key: string) {
    return this.values.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.values.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.values.delete(key)
  }

  setItem(key: string, value: string) {
    this.values.set(key, String(value))
  }
}

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
      'event: error\ndata: {"message":"provider failed with token=stream-secret at /srv/app/AiRouteService.java:42"}\n\n',
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

    expect(errors).toHaveBeenCalledWith('AI route generation failed. Please try again.')
    expect(errors.mock.calls[0][0]).not.toContain('stream-secret')
    expect(errors.mock.calls[0][0]).not.toContain('/srv/app')
    expect(deltas).not.toHaveBeenCalled()
  })

  it('reports a readable body boundary error through the stream callback', async () => {
    stubFetch(new Response(null, {
      status: 200,
      headers: { 'Content-Type': 'text/event-stream' }
    }))

    const errors = vi.fn()

    await streamRouteGenerationJob('job-1', { onError: errors })

    expect(errors).toHaveBeenCalledWith('Stream connection failed. Please try again.')
    expect(errors.mock.calls[0][0]).not.toContain('readable body')
  })

  it('keeps route job SSE error payloads off user-visible callbacks', async () => {
    stubFetch(streamResponse([
      'data: {"type":"error","payload":{"message":"stack trace token=job-secret /var/app/RouteJob.java:7"}}\n\n',
      'data: {"type":"delta","text":"ignored"}\n\n'
    ]))

    const deltas = vi.fn()
    const errors = vi.fn()

    await streamRouteGenerationJob('job-1', {
      onDelta: deltas,
      onError: errors
    })

    expect(errors).toHaveBeenCalledWith('AI route generation failed. Please try again.')
    expect(errors.mock.calls[0][0]).not.toContain('job-secret')
    expect(errors.mock.calls[0][0]).not.toContain('/var/app')
    expect(deltas).not.toHaveBeenCalled()
  })

  it('silently returns when the fetch is aborted', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new DOMException('Aborted', 'AbortError')))

    const errors = vi.fn()

    await streamRouteGenerationJob('job-1', { onError: errors })

    expect(errors).not.toHaveBeenCalled()
  })

  it('keeps HTTP 429 errors on safe local rate-limit text', async () => {
    stubFetch(new Response('slow down', { status: 429 }))

    await expect(streamRouteGenerationJob('job-1', {})).rejects.toThrow('temporarily rate limited')
  })

  it('uses endpoint registry paths for route stream and job requests', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(streamResponse([
        'data: {"type":"done","content":"ok"}\n\n'
      ]))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        jobId: 'job-1',
        status: 'RUNNING',
        content: '',
        days: 3,
        budget: 'comfort',
        preference: 'natural',
        cached: false,
        createdAt: '2026-06-08T00:00:00Z',
        updatedAt: '2026-06-08T00:00:00Z'
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      }))
      .mockResolvedValueOnce(streamResponse([
        'data: {"type":"done","content":"ok"}\n\n'
      ])))

    await generateRouteStream(
      { days: 3, budget: 'comfort', preference: 'natural' },
      {}
    )

    await startRouteGenerationJob(
      { days: 3, budget: 'comfort', preference: 'natural' }
    )

    await streamRouteGenerationJob('job/1', {})

    const fetchMock = fetch as unknown as ReturnType<typeof vi.fn>
    expect(fetchMock.mock.calls[0][0]).toBe('/api/routes/generate/stream')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/routes/generate/jobs')
    expect(fetchMock.mock.calls[2][0]).toBe('/api/routes/generate/jobs/job%2F1/stream')
  })

  it('does not attach legacy localStorage tokens to stream requests', async () => {
    const localStorage = new MemoryStorage()
    localStorage.setItem('locale', 'bo')
    localStorage.setItem('token', 'legacy-jwt-token')
    localStorage.setItem('jwt', 'legacy-jwt-value')
    vi.stubGlobal('window', { localStorage })
    vi.stubGlobal('localStorage', localStorage)
    stubFetch(streamResponse([
      'data: {"type":"done","content":"ok"}\n\n'
    ]))

    await generateRouteStream(
      { days: 3, budget: 'comfort', preference: 'natural' },
      {}
    )

    const fetchMock = fetch as unknown as ReturnType<typeof vi.fn>
    const headers = fetchMock.mock.calls[0][1]?.headers as Record<string, string>
    expect(headers['Accept-Language']).toBe('bo')
    expect(headers.Authorization).toBeUndefined()
    expect(headers.authorization).toBeUndefined()
    expect(JSON.stringify(headers)).not.toContain('legacy-jwt')
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

  it('marks route job streams interrupted on clean EOF without a terminal event', async () => {
    stubFetch(streamResponse([
      'data: {"type":"snapshot","payload":{"jobId":"job-1","status":"RUNNING","content":"base","days":3,"budget":"comfort","preference":"natural","cached":false,"createdAt":"2026-06-08T00:00:00Z","updatedAt":"2026-06-08T00:00:00Z"}}\n\n',
      'data: {"type":"delta","text":"base plus"}\n\n'
    ]))

    const done = vi.fn()
    const interrupted = vi.fn()
    const errors = vi.fn()

    await streamRouteGenerationJob('job-1', {
      onDone: done,
      onInterrupted: interrupted,
      onError: errors
    })

    expect(interrupted).toHaveBeenCalledWith('base plus')
    expect(done).not.toHaveBeenCalled()
    expect(errors).not.toHaveBeenCalled()
  })
})
