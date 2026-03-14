import 'dart:async';

import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/features/budget/domain/entities/budget_snapshot.dart';
import 'package:beltech/features/budget/domain/entities/budget_target.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final budgetMonthProvider = StateProvider<DateTime>((_) {
  final now = DateTime.now();
  return DateTime(now.year, now.month, 1);
});

final budgetSnapshotProvider = StreamProvider<BudgetSnapshot>((ref) {
  final month = ref.watch(budgetMonthProvider);
  return ref.watch(budgetRepositoryProvider).watchMonthlySnapshot(month);
});

final budgetTargetsProvider = FutureProvider<List<BudgetTarget>>((ref) {
  return ref.watch(budgetRepositoryProvider).loadTargets();
});

class BudgetWriteController extends AutoDisposeAsyncNotifier<void> {
  @override
  FutureOr<void> build() {}

  Future<void> saveTarget({
    required String category,
    required double monthlyLimitKes,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(budgetRepositoryProvider).upsertTarget(
            category: category,
            monthlyLimitKes: monthlyLimitKes,
          );
      ref.invalidate(budgetTargetsProvider);
    });
  }

  Future<void> deleteTarget(int targetId) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(budgetRepositoryProvider).deleteTarget(targetId);
      ref.invalidate(budgetTargetsProvider);
    });
  }
}

final budgetWriteControllerProvider =
    AutoDisposeAsyncNotifierProvider<BudgetWriteController, void>(
  BudgetWriteController.new,
);
