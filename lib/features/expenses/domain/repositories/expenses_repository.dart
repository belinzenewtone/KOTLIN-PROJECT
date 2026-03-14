import 'package:beltech/features/expenses/domain/entities/expense_item.dart';

abstract class ExpensesRepository {
  Stream<ExpensesSnapshot> watchSnapshot();

  Future<void> addManualTransaction({
    required String title,
    required String category,
    required double amountKes,
    DateTime? occurredAt,
  });

  Future<void> updateTransaction({
    required int transactionId,
    required String title,
    required String category,
    required double amountKes,
    required DateTime occurredAt,
  });

  Future<void> deleteTransaction(int transactionId);

  Future<int> importSmsMessages(
    List<String> rawMessages, {
    DateTime? from,
  });

  Future<int> importFromDevice({
    DateTime? from,
  });
}
