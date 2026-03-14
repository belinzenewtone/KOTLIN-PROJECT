import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:beltech/features/expenses/data/repositories/expenses_repository_impl.dart';
import 'package:beltech/features/expenses/data/services/mpesa_parser_service.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  SharedPreferences.setMockInitialValues(const {});

  late AppDriftStore store;
  late ExpensesRepositoryImpl repository;

  setUp(() {
    store = AppDriftStore();
    repository = ExpensesRepositoryImpl(store, const MpesaParserService());
  });

  tearDown(() async {
    await store.dispose();
  });

  test('addManualTransaction updates snapshot totals and transactions',
      () async {
    final initial = await repository.watchSnapshot().first;
    final nextSnapshot = repository.watchSnapshot().firstWhere(
          (snapshot) =>
              snapshot.transactions.any((tx) => tx.title == 'Test Expense'),
        );

    await repository.addManualTransaction(
      title: 'Test Expense',
      category: 'Other',
      amountKes: 250,
    );

    final updated = await nextSnapshot.timeout(const Duration(seconds: 2));
    expect(updated.weekKes, greaterThanOrEqualTo(initial.weekKes));
    expect(
        updated.transactions.any((tx) => tx.title == 'Test Expense'), isTrue);
  });

  test('importSmsMessages parses valid mpesa rows', () async {
    final nextSnapshot = repository.watchSnapshot().firstWhere(
          (snapshot) =>
              snapshot.transactions.any((tx) => tx.title.contains('Sky Cafe')),
        );
    final imported = await repository.importSmsMessages([
      'QW12AB34CD Confirmed. Ksh1,250.00 sent to SKY CAFE on 7/3/26 at 6:24 PM.',
      'invalid row',
    ]);

    final updated = await nextSnapshot.timeout(const Duration(seconds: 2));
    expect(imported, 1);
    expect(updated.transactions.any((tx) => tx.title.contains('Sky Cafe')),
        isTrue);
  });

  test('updateTransaction and deleteTransaction persist expense changes',
      () async {
    await repository.addManualTransaction(
      title: 'Expense CRUD',
      category: 'Other',
      amountKes: 99,
      occurredAt: DateTime.now(),
    );

    final created = await repository.watchSnapshot().firstWhere(
          (snapshot) =>
              snapshot.transactions.any((tx) => tx.title == 'Expense CRUD'),
        );
    final tx =
        created.transactions.firstWhere((item) => item.title == 'Expense CRUD');

    await repository.updateTransaction(
      transactionId: tx.id,
      title: 'Expense CRUD Updated',
      category: 'Food',
      amountKes: 150,
      occurredAt: tx.occurredAt,
    );

    final updated = await repository.watchSnapshot().firstWhere(
          (snapshot) => snapshot.transactions.any((item) =>
              item.id == tx.id && item.title == 'Expense CRUD Updated'),
        );
    expect(
        updated.transactions
            .any((item) => item.id == tx.id && item.category == 'Food'),
        isTrue);

    await repository.deleteTransaction(tx.id);
    final afterDelete = await repository.watchSnapshot().firstWhere(
          (snapshot) => !snapshot.transactions.any((item) => item.id == tx.id),
        );
    expect(afterDelete.transactions.any((item) => item.id == tx.id), isFalse);
  });
}
