/** Minimal EventSource stand-in: tests push events instead of opening a real stream. */
export class FakeEventSource {
  static instances: FakeEventSource[] = [];

  onerror: ((event: Event) => void) | null = null;
  closed = false;
  private readonly listeners = new Map<string, ((event: MessageEvent) => void)[]>();

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this);
  }

  static latest(): FakeEventSource {
    return FakeEventSource.instances[FakeEventSource.instances.length - 1];
  }

  addEventListener(type: string, listener: (event: MessageEvent) => void): void {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener]);
  }

  emit(type: string, data: unknown): void {
    const event = new MessageEvent(type, { data: JSON.stringify(data) });
    this.listeners.get(type)?.forEach((listener) => listener(event));
  }

  fail(): void {
    this.onerror?.(new Event('error'));
  }

  close(): void {
    this.closed = true;
  }
}

export const fakeEventSourceFactory = (url: string) => new FakeEventSource(url) as unknown as EventSource;
