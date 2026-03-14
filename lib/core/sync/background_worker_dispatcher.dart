import 'dart:ui' as ui;

import 'package:beltech/core/config/supabase_config.dart';
import 'package:beltech/core/notifications/local_notification_service.dart';
import 'package:beltech/core/notifications/notification_insights_service.dart';
import 'package:beltech/core/sync/sms_auto_import_service.dart';
import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:beltech/features/auth/data/repositories/local_account_repository_impl.dart';
import 'package:beltech/features/auth/data/repositories/supabase_account_repository_impl.dart';
import 'package:beltech/features/budget/data/repositories/budget_repository_impl.dart';
import 'package:beltech/features/budget/data/repositories/supabase_budget_repository_impl.dart';
import 'package:beltech/features/budget/domain/repositories/budget_repository.dart';
import 'package:beltech/features/calendar/data/repositories/calendar_repository_impl.dart';
import 'package:beltech/features/calendar/data/repositories/supabase_calendar_repository_impl.dart';
import 'package:beltech/features/calendar/domain/repositories/calendar_repository.dart';
import 'package:beltech/features/expenses/data/repositories/expenses_repository_impl.dart';
import 'package:beltech/features/expenses/data/repositories/supabase_expenses_repository_impl.dart';
import 'package:beltech/features/expenses/data/services/device_sms_data_source.dart';
import 'package:beltech/features/expenses/data/services/merchant_learning_service.dart';
import 'package:beltech/features/expenses/data/services/mpesa_parser_service.dart';
import 'package:beltech/features/expenses/domain/repositories/expenses_repository.dart';
import 'package:beltech/features/recurring/data/repositories/recurring_repository_impl.dart';
import 'package:beltech/features/recurring/data/repositories/supabase_recurring_repository_impl.dart';
import 'package:beltech/features/recurring/data/services/recurring_materializer_service.dart';
import 'package:beltech/features/recurring/domain/repositories/recurring_repository.dart';
import 'package:beltech/features/tasks/data/repositories/supabase_tasks_repository_impl.dart';
import 'package:beltech/features/tasks/data/repositories/tasks_repository_impl.dart';
import 'package:beltech/features/tasks/domain/repositories/tasks_repository.dart';
import 'package:flutter/widgets.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:workmanager/workmanager.dart';

const String kBackgroundSyncTaskName = 'beltech.background.sync';
const String kBackgroundSyncPeriodicUniqueName = 'com.beltech.app.sync';
const String kBackgroundSyncOneOffUniqueName = 'beltech.background.oneoff';

@pragma('vm:entry-point')
void backgroundSyncDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    WidgetsFlutterBinding.ensureInitialized();
    ui.DartPluginRegistrant.ensureInitialized();
    await BackgroundWorkerRuntime.run();
    return true;
  });
}

class BackgroundWorkerRuntime {
  static Future<void> run() async {
    final useSupabase = await _ensureSupabaseIfConfigured();
    AppDriftStore? localStore;
    try {
      final accountRepository = useSupabase
          ? SupabaseAccountRepositoryImpl(Supabase.instance.client)
          : LocalAccountRepositoryImpl();

      final repositories = _buildRepositories(useSupabase);
      localStore = repositories.localStore;

      final smsService =
          SmsAutoImportService(repositories.expenses, accountRepository);
      final recurringService =
          RecurringMaterializerService(repositories.recurring);
      final notifications = LocalNotificationService();
      final insights = NotificationInsightsService(
        notifications,
        repositories.budget,
        repositories.expenses,
        repositories.tasks,
        repositories.calendar,
        accountRepository,
      );

      await smsService.syncNow();
      await recurringService.syncNow();
      await insights.runSweep();
    } catch (_) {
      // Silent fail: background worker should not crash the process.
    } finally {
      await localStore?.dispose();
    }
  }

  static _WorkerRepositories _buildRepositories(bool useSupabase) {
    final parser = const MpesaParserService();
    final merchantLearning = MerchantLearningService();
    final smsSource = DeviceSmsDataSource();
    if (useSupabase) {
      final client = Supabase.instance.client;
      return _WorkerRepositories(
        expenses: SupabaseExpensesRepositoryImpl(
          client,
          parser,
          merchantLearning,
          smsSource,
        ),
        recurring: SupabaseRecurringRepositoryImpl(client),
        budget: SupabaseBudgetRepositoryImpl(client),
        tasks: SupabaseTasksRepositoryImpl(client),
        calendar: SupabaseCalendarRepositoryImpl(client),
      );
    }

    final store = AppDriftStore();
    return _WorkerRepositories(
      localStore: store,
      expenses: ExpensesRepositoryImpl(
        store,
        parser,
        merchantLearning,
        smsSource,
      ),
      recurring: RecurringRepositoryImpl(store),
      budget: BudgetRepositoryImpl(store),
      tasks: TasksRepositoryImpl(store),
      calendar: CalendarRepositoryImpl(store),
    );
  }

  static Future<bool> _ensureSupabaseIfConfigured() async {
    if (!SupabaseConfig.isConfigured) {
      return false;
    }
    try {
      Supabase.instance.client;
      return true;
    } catch (_) {
      await Supabase.initialize(
        url: SupabaseConfig.url,
        anonKey: SupabaseConfig.publicKey,
      );
      return true;
    }
  }
}

class _WorkerRepositories {
  _WorkerRepositories({
    this.localStore,
    required this.expenses,
    required this.recurring,
    required this.budget,
    required this.tasks,
    required this.calendar,
  });

  final AppDriftStore? localStore;
  final ExpensesRepository expenses;
  final RecurringRepository recurring;
  final BudgetRepository budget;
  final TasksRepository tasks;
  final CalendarRepository calendar;
}
