import 'dart:async';

import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/features/expenses/domain/entities/expense_import_window.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

enum ExpenseFilter { all, today, week, month }

final expenseFilterProvider =
    StateProvider<ExpenseFilter>((_) => ExpenseFilter.month);

final expensesSnapshotProvider = StreamProvider<ExpensesSnapshot>(
  (ref) => ref.watch(expensesRepositoryProvider).watchSnapshot(),
);

class ExpenseWriteController extends AutoDisposeAsyncNotifier<void> {
  @override
  FutureOr<void> build() {}

  Future<void> addQuickExpense() async {
    await addExpense(
      title: 'Manual Expense',
      category: 'Other',
      amountKes: 120,
    );
  }

  Future<void> addExpense({
    required String title,
    required String category,
    required double amountKes,
    DateTime? occurredAt,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(expensesRepositoryProvider).addManualTransaction(
            title: title,
            category: category,
            amountKes: amountKes,
            occurredAt: occurredAt,
          );
    });
  }

  Future<void> updateExpense({
    required int transactionId,
    required String title,
    required String category,
    required double amountKes,
    required DateTime occurredAt,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(expensesRepositoryProvider).updateTransaction(
            transactionId: transactionId,
            title: title,
            category: category,
            amountKes: amountKes,
            occurredAt: occurredAt,
          );
    });
  }

  Future<void> deleteExpense(int transactionId) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref
          .read(expensesRepositoryProvider)
          .deleteTransaction(transactionId);
    });
  }

  Future<int> importSmsPayload(
    String payload, {
    required ExpenseImportWindow window,
  }) async {
    final lines = payload
        .split(RegExp(r'\r?\n'))
        .map((line) => line.trim())
        .where((line) => line.isNotEmpty)
        .toList();
    if (lines.isEmpty) {
      return 0;
    }
    final from = fromWindow(window);
    state = const AsyncLoading();
    final result = await AsyncValue.guard(
      () => ref
          .read(expensesRepositoryProvider)
          .importSmsMessages(lines, from: from),
    );
    if (result.hasError) {
      state =
          AsyncError(result.error!, result.stackTrace ?? StackTrace.current);
      throw result.error!;
    }
    state = const AsyncData(null);
    return result.valueOrNull ?? 0;
  }

  Future<int> importFromDevice({
    required ExpenseImportWindow window,
  }) async {
    state = const AsyncLoading();
    final result = await AsyncValue.guard(
      () => ref.read(expensesRepositoryProvider).importFromDevice(
            from: fromWindow(window),
          ),
    );
    if (result.hasError) {
      state =
          AsyncError(result.error!, result.stackTrace ?? StackTrace.current);
      throw result.error!;
    }
    state = const AsyncData(null);
    return result.valueOrNull ?? 0;
  }
}

DateTime fromWindow(ExpenseImportWindow window) {
  final now = DateTime.now();
  return switch (window) {
    ExpenseImportWindow.last24Hours => now.subtract(const Duration(hours: 24)),
    ExpenseImportWindow.last7Days => now.subtract(const Duration(days: 7)),
    ExpenseImportWindow.last30Days => now.subtract(const Duration(days: 30)),
    ExpenseImportWindow.last90Days => now.subtract(const Duration(days: 90)),
  };
}

final expenseWriteControllerProvider =
    AutoDisposeAsyncNotifierProvider<ExpenseWriteController, void>(
  ExpenseWriteController.new,
);
