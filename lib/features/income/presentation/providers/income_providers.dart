import 'dart:async';

import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/features/income/domain/entities/income_item.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final incomesProvider = StreamProvider<List<IncomeItem>>(
  (ref) => ref.watch(incomeRepositoryProvider).watchIncomes(),
);

class IncomeWriteController extends AutoDisposeAsyncNotifier<void> {
  @override
  FutureOr<void> build() {}

  Future<void> addIncome({
    required String title,
    required double amountKes,
    DateTime? receivedAt,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(incomeRepositoryProvider).addIncome(
            title: title,
            amountKes: amountKes,
            receivedAt: receivedAt,
          );
    });
  }

  Future<void> updateIncome({
    required int incomeId,
    required String title,
    required double amountKes,
    required DateTime receivedAt,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(incomeRepositoryProvider).updateIncome(
            incomeId: incomeId,
            title: title,
            amountKes: amountKes,
            receivedAt: receivedAt,
          );
    });
  }

  Future<void> deleteIncome(int incomeId) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(incomeRepositoryProvider).deleteIncome(incomeId);
    });
  }
}

final incomeWriteControllerProvider =
    AutoDisposeAsyncNotifierProvider<IncomeWriteController, void>(
  IncomeWriteController.new,
);
