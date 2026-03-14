import 'package:beltech/core/config/assistant_proxy_config.dart';
import 'package:beltech/features/onboarding/data/repositories/onboarding_repository_impl.dart';
import 'package:beltech/features/onboarding/domain/repositories/onboarding_repository.dart';
import 'package:beltech/core/config/supabase_config.dart';
import 'package:beltech/core/di/database_providers.dart';
import 'package:beltech/core/di/security_providers.dart';
import 'package:beltech/core/platform/runtime_env.dart';
import 'package:beltech/core/sync/data_mode_migration_service.dart';
import 'package:beltech/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:beltech/features/auth/data/repositories/local_account_repository_impl.dart';
import 'package:beltech/features/auth/data/repositories/supabase_account_repository_impl.dart';
import 'package:beltech/features/auth/domain/repositories/account_repository.dart';
import 'package:beltech/features/auth/domain/repositories/auth_repository.dart';
import 'package:beltech/features/analytics/data/repositories/analytics_repository_impl.dart';
import 'package:beltech/features/analytics/data/repositories/supabase_analytics_repository_impl.dart';
import 'package:beltech/features/analytics/domain/repositories/analytics_repository.dart';
import 'package:beltech/features/assistant/data/repositories/assistant_repository_impl.dart';
import 'package:beltech/features/assistant/data/repositories/supabase_assistant_repository_impl.dart';
import 'package:beltech/features/assistant/data/services/assistant_proxy_service.dart';
import 'package:beltech/features/assistant/domain/repositories/assistant_repository.dart';
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
import 'package:beltech/features/export/data/repositories/export_repository_impl.dart';
import 'package:beltech/features/export/data/repositories/supabase_export_repository_impl.dart';
import 'package:beltech/features/export/domain/repositories/export_repository.dart';
import 'package:beltech/features/home/data/repositories/home_repository_impl.dart';
import 'package:beltech/features/home/data/repositories/supabase_home_repository_impl.dart';
import 'package:beltech/features/home/domain/repositories/home_repository.dart';
import 'package:beltech/features/income/data/repositories/income_repository_impl.dart';
import 'package:beltech/features/income/data/repositories/supabase_income_repository_impl.dart';
import 'package:beltech/features/income/domain/repositories/income_repository.dart';
import 'package:beltech/features/profile/data/repositories/profile_repository_impl.dart';
import 'package:beltech/features/profile/data/repositories/supabase_profile_repository_impl.dart';
import 'package:beltech/features/profile/domain/repositories/profile_repository.dart';
import 'package:beltech/features/recurring/data/repositories/recurring_repository_impl.dart';
import 'package:beltech/features/recurring/data/repositories/supabase_recurring_repository_impl.dart';
import 'package:beltech/features/recurring/domain/repositories/recurring_repository.dart';
import 'package:beltech/features/search/data/repositories/global_search_repository_impl.dart';
import 'package:beltech/features/search/data/repositories/supabase_global_search_repository_impl.dart';
import 'package:beltech/features/search/domain/repositories/global_search_repository.dart';
import 'package:beltech/features/tasks/data/repositories/supabase_tasks_repository_impl.dart';
import 'package:beltech/features/tasks/data/repositories/tasks_repository_impl.dart';
import 'package:beltech/features/tasks/domain/repositories/tasks_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

enum DataModePreference { cloud, local }

const String dataModePreferenceKey = 'app.data_mode_preference';

DataModePreference dataModePreferenceFromRaw({
  required String? raw,
  required bool cloudAvailable,
}) {
  final parsed = switch (raw) {
    'local' => DataModePreference.local,
    'cloud' => DataModePreference.cloud,
    _ => null,
  };
  if (parsed == null) {
    return cloudAvailable ? DataModePreference.cloud : DataModePreference.local;
  }
  if (parsed == DataModePreference.cloud && !cloudAvailable) {
    return DataModePreference.local;
  }
  return parsed;
}

Future<DataModePreference> loadPersistedDataModePreference({
  required bool cloudAvailable,
}) async {
  final prefs = await SharedPreferences.getInstance();
  final raw = prefs.getString(dataModePreferenceKey);
  return dataModePreferenceFromRaw(raw: raw, cloudAvailable: cloudAvailable);
}

Future<void> persistDataModePreference(DataModePreference mode) async {
  final prefs = await SharedPreferences.getInstance();
  await prefs.setString(dataModePreferenceKey, mode.name);
}

final cloudModeAvailableProvider = Provider<bool>(
  (_) => SupabaseConfig.isConfigured && !hasRuntimeEnv('FLUTTER_TEST'),
);

final preferredDataModeProvider = StateProvider<DataModePreference>((ref) {
  final cloudAvailable = ref.watch(cloudModeAvailableProvider);
  return cloudAvailable ? DataModePreference.cloud : DataModePreference.local;
});

final useSupabaseProvider = Provider<bool>((ref) {
  final cloudAvailable = ref.watch(cloudModeAvailableProvider);
  final mode = ref.watch(preferredDataModeProvider);
  return cloudAvailable && mode == DataModePreference.cloud;
});

final dataModeMigrationServiceProvider = Provider<DataModeMigrationService>(
  (ref) {
    final cloudAvailable = ref.watch(cloudModeAvailableProvider);
    final client = cloudAvailable ? ref.watch(supabaseClientProvider) : null;
    return DataModeMigrationService(
      localStore: ref.watch(appDriftStoreProvider),
      supabaseClient: client,
    );
  },
);

final supabaseClientProvider =
    Provider<SupabaseClient>((_) => Supabase.instance.client);
final deviceSmsDataSourceProvider = Provider<DeviceSmsDataSource>(
  (_) => DeviceSmsDataSource(),
);
final merchantLearningServiceProvider = Provider<MerchantLearningService>(
  (_) => MerchantLearningService(),
);
final assistantProxyServiceProvider = Provider<AssistantProxyService?>((ref) {
  if (!AssistantProxyConfig.isConfigured) {
    return null;
  }
  return AssistantProxyService(
    endpoint: AssistantProxyConfig.endpoint,
    supabaseClient: ref.watch(useSupabaseProvider)
        ? ref.watch(supabaseClientProvider)
        : null,
  );
});

final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseHomeRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return HomeRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final calendarRepositoryProvider = Provider<CalendarRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseCalendarRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return CalendarRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final expensesRepositoryProvider = Provider<ExpensesRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseExpensesRepositoryImpl(
      ref.watch(supabaseClientProvider),
      const MpesaParserService(),
      ref.watch(merchantLearningServiceProvider),
      ref.watch(deviceSmsDataSourceProvider),
    );
  }
  return ExpensesRepositoryImpl(
    ref.watch(appDriftStoreProvider),
    const MpesaParserService(),
    ref.watch(merchantLearningServiceProvider),
    ref.watch(deviceSmsDataSourceProvider),
  );
});
final incomeRepositoryProvider = Provider<IncomeRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseIncomeRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return IncomeRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final budgetRepositoryProvider = Provider<BudgetRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseBudgetRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return BudgetRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final recurringRepositoryProvider = Provider<RecurringRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseRecurringRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return RecurringRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final globalSearchRepositoryProvider = Provider<GlobalSearchRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseGlobalSearchRepositoryImpl(
        ref.watch(supabaseClientProvider));
  }
  return GlobalSearchRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final exportRepositoryProvider = Provider<ExportRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseExportRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return ExportRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final tasksRepositoryProvider = Provider<TasksRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseTasksRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return TasksRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepositoryImpl(
    ref.watch(localAuthenticationProvider),
    ref.watch(secureCredentialsStoreProvider),
  ),
);
final accountRepositoryProvider = Provider<AccountRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseAccountRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return LocalAccountRepositoryImpl();
});
final assistantRepositoryProvider = Provider<AssistantRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseAssistantRepositoryImpl(
      ref.watch(supabaseClientProvider),
      proxyService: ref.watch(assistantProxyServiceProvider),
    );
  }
  return AssistantRepositoryImpl(
    ref.watch(assistantProfileStoreProvider),
    ref.watch(appDriftStoreProvider),
    proxyService: ref.watch(assistantProxyServiceProvider),
  );
});
final analyticsRepositoryProvider = Provider<AnalyticsRepository>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return SupabaseAnalyticsRepositoryImpl(ref.watch(supabaseClientProvider));
  }
  return AnalyticsRepositoryImpl(ref.watch(appDriftStoreProvider));
});
final profileRepositoryProvider = Provider<ProfileRepository>(
  (ref) {
    if (ref.watch(useSupabaseProvider)) {
      return SupabaseProfileRepositoryImpl(
        ref.watch(supabaseClientProvider),
      );
    }
    return ProfileRepositoryImpl(
      ref.watch(assistantProfileStoreProvider),
      ref.watch(secureCredentialsStoreProvider),
      ref.watch(passwordHasherProvider),
    );
  },
);

/// Onboarding completion flag — device-local, no cloud sync needed.
final onboardingRepositoryProvider = Provider<OnboardingRepository>(
  (_) => OnboardingRepositoryImpl(),
);
