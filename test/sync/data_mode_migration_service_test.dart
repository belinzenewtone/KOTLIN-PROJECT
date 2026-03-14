import 'package:beltech/core/sync/data_mode_migration_service.dart';
import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late AppDriftStore store;
  late DataModeMigrationService service;

  setUp(() {
    store = AppDriftStore();
    service = DataModeMigrationService(
      localStore: store,
      supabaseClient: null,
    );
  });

  tearDown(() async {
    await store.dispose();
  });

  test('preview blocks when switching to cloud and cloud is unavailable',
      () async {
    final preview = await service.preview(
      fromMode: DataStoreMode.local,
      toMode: DataStoreMode.cloud,
      cloudAvailable: false,
    );

    expect(preview.canMigrate, isFalse);
    expect(preview.blockerMessage, contains('Cloud mode is unavailable'));
    expect(preview.sourceTotal, greaterThan(0));
    expect(preview.destinationTotal, equals(0));
  });

  test('preview blocks when source and destination modes are the same',
      () async {
    final preview = await service.preview(
      fromMode: DataStoreMode.local,
      toMode: DataStoreMode.local,
      cloudAvailable: true,
    );

    expect(preview.canMigrate, isFalse);
    expect(preview.blockerMessage, contains('same'));
  });

  test('migrate throws when attempting cloud migration without setup',
      () async {
    expect(
      () => service.migrate(
        fromMode: DataStoreMode.local,
        toMode: DataStoreMode.cloud,
        cloudAvailable: false,
      ),
      throwsException,
    );
  });
}
