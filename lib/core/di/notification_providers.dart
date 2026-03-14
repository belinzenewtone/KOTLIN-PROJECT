import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/notifications/local_notification_service.dart';
import 'package:beltech/core/notifications/notification_insights_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final localNotificationServiceProvider = Provider<LocalNotificationService>(
  (_) => LocalNotificationService(),
);

final notificationsEnabledProvider = FutureProvider<bool>(
  (ref) => ref.watch(localNotificationServiceProvider).isNotificationsEnabled(),
);

final notificationInsightsServiceProvider =
    Provider<NotificationInsightsService>(
  (ref) => NotificationInsightsService(
    ref.watch(localNotificationServiceProvider),
    ref.watch(budgetRepositoryProvider),
    ref.watch(expensesRepositoryProvider),
    ref.watch(tasksRepositoryProvider),
    ref.watch(calendarRepositoryProvider),
    ref.watch(accountRepositoryProvider),
  ),
);

final budgetAlertsEnabledProvider = FutureProvider<bool>(
  (ref) =>
      ref.watch(notificationInsightsServiceProvider).isBudgetAlertsEnabled(),
);

final dailyDigestEnabledProvider = FutureProvider<bool>(
  (ref) =>
      ref.watch(notificationInsightsServiceProvider).isDailyDigestEnabled(),
);

class NotificationPreferenceController extends AutoDisposeAsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<void> setEnabled(bool enabled) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref
          .read(localNotificationServiceProvider)
          .setNotificationsEnabled(enabled);
      ref.invalidate(notificationsEnabledProvider);
    });
  }

  Future<void> setBudgetAlertsEnabled(bool enabled) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref
          .read(notificationInsightsServiceProvider)
          .setBudgetAlertsEnabled(enabled);
      ref.invalidate(budgetAlertsEnabledProvider);
    });
  }

  Future<void> setDailyDigestEnabled(bool enabled) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref
          .read(notificationInsightsServiceProvider)
          .setDailyDigestEnabled(enabled);
      ref.invalidate(dailyDigestEnabledProvider);
    });
  }
}

final notificationPreferenceControllerProvider =
    AutoDisposeAsyncNotifierProvider<NotificationPreferenceController, void>(
  NotificationPreferenceController.new,
);
