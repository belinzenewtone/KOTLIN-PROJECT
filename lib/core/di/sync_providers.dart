import 'package:beltech/core/di/notification_providers.dart';
import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/sync/background_sync_coordinator.dart';
import 'package:beltech/core/sync/os_background_sync_scheduler.dart';
import 'package:beltech/core/sync/sms_auto_import_service.dart';
import 'package:beltech/core/sync/sync_status_notifier.dart';
import 'package:beltech/features/recurring/data/services/recurring_materializer_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final smsAutoImportServiceProvider = Provider<SmsAutoImportService>((ref) {
  final service = SmsAutoImportService(
    ref.watch(expensesRepositoryProvider),
    ref.watch(accountRepositoryProvider),
  );
  ref.onDispose(() {
    service.stop();
  });
  return service;
});

final recurringMaterializerServiceProvider =
    Provider<RecurringMaterializerService>((ref) {
  final service = RecurringMaterializerService(
    ref.watch(recurringRepositoryProvider),
  );
  ref.onDispose(() {
    service.stop();
  });
  return service;
});

final osBackgroundSyncSchedulerProvider = Provider<OsBackgroundSyncScheduler>(
  (_) => OsBackgroundSyncScheduler(),
);

final backgroundSyncCoordinatorProvider =
    Provider<BackgroundSyncCoordinator>((ref) {
  final coordinator = BackgroundSyncCoordinator(
    ref.watch(smsAutoImportServiceProvider),
    ref.watch(recurringMaterializerServiceProvider),
    ref.watch(notificationInsightsServiceProvider),
    ref.watch(osBackgroundSyncSchedulerProvider),
  );
  ref.onDispose(() {
    coordinator.stop();
  });
  return coordinator;
});

/// Observable sync status for UI feedback on foreground sync operations.
///
/// Reads the persisted last-sync timestamp from SharedPreferences on creation
/// so the home screen can display relative time immediately.  Use
/// [SyncStatusNotifier.runSync] to trigger an on-demand sync.
final syncStatusProvider =
    StateNotifierProvider<SyncStatusNotifier, SyncStatus>((ref) {
  return SyncStatusNotifier(ref.watch(backgroundSyncCoordinatorProvider));
});
