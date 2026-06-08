import { describe, expect, it, vi } from 'vitest'
import { readJsonSseStream, SseStreamSizeError } from './sse'

const encoder = new TextEncoder()

function streamingResponse(chunk: string, onCancel: (reason: unknown) => void): Response {
  return new Response(new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(encoder.encode(chunk))
    },
    cancel: onCancel
  }), {
    status: 200,
    headers: { 'Content-Type': 'text/event-stream' }
  })
}

describe('SSE stream parser limits', () => {
  it('throws and cancels a malformed stream when the buffer grows past the limit', async () => {
    const onCancel = vi.fn()
    const response = streamingResponse(`data: ${'x'.repeat(128)}`, onCancel)

    await expect(readJsonSseStream(response, {
      maxBufferBytes: 64,
      maxFrameBytes: 1024,
      onMessage: vi.fn()
    })).rejects.toBeInstanceOf(SseStreamSizeError)

    expect(onCancel).toHaveBeenCalledWith(expect.any(SseStreamSizeError))
  })

  it('throws and cancels a delimited frame that exceeds the frame limit', async () => {
    const onCancel = vi.fn()
    const response = streamingResponse(`data: "${'x'.repeat(128)}"\n\n`, onCancel)

    await expect(readJsonSseStream(response, {
      maxBufferBytes: 1024,
      maxFrameBytes: 64,
      onMessage: vi.fn()
    })).rejects.toBeInstanceOf(SseStreamSizeError)

    expect(onCancel).toHaveBeenCalledWith(expect.any(SseStreamSizeError))
  })
})
