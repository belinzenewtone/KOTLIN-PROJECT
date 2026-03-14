import 'package:beltech/core/notifications/notification_insights_service.dart';
import 'package:beltech/core/platform/runtime_env.dart';
import 'package:beltech/core/sync/os_background_sync_scheduler.dart';
import 'package:beltech/core/sync/sms_auto_import_service.dart';
import 'package:beltech/features/recurring/data/services/recurring_materializer_service.dart';
import 'package:flutter/foundation.dart';

class BackgroundSyncCoordinator {
  BackgroundSyncCoordinator(
    this._smsAutoImportService,
    this._recurringMaterializerService,
    this._notificationInsightsService,
    this._osBackgroundSyncScheduler,
  );

  final SmsAutoImportService _smsAutoImportService;
  final RecurringMaterializerService _recurringMaterializerService;
  final NotificationInsightsService _notificationInsightsService;
  final OsBackgroundSyncScheduler _osBackgroundSyncScheduler;

  BackgroundSyncStrategy get _strategy => BackgroundSyncStrategy.forPlatform();

  Future<void> start() async {
    if (hasRuntimeEnv('FLUTTER_TEST')) {
      return;
    }
    await _osBackgroundSyncScheduler.initializeAndSchedule();
    await _smsAutoImportService.start(interval: _strategy.smsInterval);
    await _recurringMaterializerService.start(
      interval: _strategy.recurringInterval,
    );
    await _notificationInsightsService.runSweep();
  }

  Future<void> stop() async {
    await _smsAutoImportService.stop();
    await _recurringMaterializerService.stop();
  }

  Future<void> syncNow() async {
    await _smsAutoImportService.syncNow();
  }

  Future<void> materializeNow() async {
    await _recurringMaterializerService.syncNow();
  }

  Future<void> runNotificationSweep() async {
    await _notificationInsightsService.runSweep();
  }
}

class BackgroundSyncStrategy {
  const BackgroundSyncStrategy({
    required this.smsInterval,
    required this.recurringInterval,
    required this.modeLabel,
  });

  final Duration smsInterval;
  final Duration recurringInterval;
  final String modeLabel;

  static BackgroundSyncStrategy forPlatform() {
    if (defaultTargetPlatform == TargetPlatform.android) {
      // Android-first: tighter cadence for higher reliability.
      return const BackgroundSyncStrategy(
        smsInterval: Duration(minutes: 20),
        recurringInterval: Duration(minutes: 2),
        modeLabel: 'android-first',
      );
    }
    return const BackgroundSyncStrategy(
      smsInterval: Duration(minutes: 45),
      recurringInterval: Duration(minutes: 5),
      modeLabel: 'foreground-fallback',
    );
  }
}
