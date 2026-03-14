import 'package:beltech/features/auth/presentation/auth_gate.dart';
import 'package:beltech/features/analytics/presentation/analytics_screen.dart';
import 'package:beltech/features/budget/presentation/budget_screen.dart';
import 'package:beltech/features/export/presentation/export_screen.dart';
import 'package:beltech/features/income/presentation/income_screen.dart';
import 'package:beltech/features/recurring/presentation/recurring_screen.dart';
import 'package:beltech/features/review/presentation/week_review_screen.dart';
import 'package:beltech/features/search/presentation/global_search_screen.dart';
import 'package:beltech/features/settings/presentation/settings_screen.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final appRouterProvider = Provider<GoRouter>(
  (ref) => GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        name: 'root',
        builder: (context, state) => const AuthGate(),
      ),
      GoRoute(
        path: '/settings',
        name: 'settings',
        builder: (context, state) => const SettingsScreen(),
      ),
      GoRoute(
        path: '/budget',
        name: 'budget',
        builder: (context, state) => const BudgetScreen(),
      ),
      GoRoute(
        path: '/income',
        name: 'income',
        builder: (context, state) => const IncomeScreen(),
      ),
      GoRoute(
        path: '/recurring',
        name: 'recurring',
        builder: (context, state) => const RecurringScreen(),
      ),
      GoRoute(
        path: '/search',
        name: 'search',
        builder: (context, state) => const GlobalSearchScreen(),
      ),
      GoRoute(
        path: '/export',
        name: 'export',
        builder: (context, state) => const ExportScreen(),
      ),
      GoRoute(
        path: '/analytics',
        name: 'analytics',
        builder: (context, state) => const AnalyticsScreen(),
      ),
      GoRoute(
        path: '/week-review',
        name: 'week-review',
        builder: (context, state) => const WeekReviewScreen(),
      ),
    ],
  ),
);
