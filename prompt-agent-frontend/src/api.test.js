import { describe, expect, it } from 'vitest'
import { parseSseFrame } from './api.js'

describe('parseSseFrame', () => {
  it('parses named JSON events without dropping data lines', () => {
    expect(parseSseFrame('event: question\ndata: {"message":"hello"}')).toEqual({
      event: 'question', data: '{"message":"hello"}'
    })
  })
})
