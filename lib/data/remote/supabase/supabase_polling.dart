Stream<T> pollStream<T>(
  Future<T> Function() loader, {
  Duration interval = const Duration(seconds: 2),
}) async* {
  while (true) {
    yield await loader();
    await Future<void>.delayed(interval);
  }
}
