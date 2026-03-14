import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:beltech/data/local/drift/app_drift_store_mutations.dart';
import 'package:beltech/features/expenses/data/services/device_sms_data_source.dart';
import 'package:beltech/features/expenses/data/services/merchant_learning_service.dart';
import 'package:beltech/features/expenses/data/services/mpesa_parser_service.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:beltech/features/expenses/domain/repositories/expenses_repository.dart';

class ExpensesRepositoryImpl implements ExpensesRepository {
  ExpensesRepositoryImpl(
    this._store,
    this._parser, [
    MerchantLearningService? merchantLearningService,
    DeviceSmsDataSource? deviceSmsDataSource,
  ])  : _merchantLearningService =
            merchantLearningService ?? MerchantLearningService(),
        _deviceSmsDataSource = deviceSmsDataSource ?? DeviceSmsDataSource();

  final AppDriftStore _store;
  final MpesaParserService _parser;
  final MerchantLearningService _merchantLearningService;
  final DeviceSmsDataSource _deviceSmsDataSource;

  @override
  Stream<ExpensesSnapshot> watchSnapshot() {
    return _store.watchExpensesSnapshot().map(
          (record) => ExpensesSnapshot(
            todayKes: record.todayKes,
            weekKes: record.weekKes,
            categories: record.categories
                .map(
                  (item) => CategoryExpenseTotal(
                    category: item.category,
                    totalKes: item.totalKes,
                  ),
                )
                .toList(),
            transactions: record.transactions
                .map(
                  (tx) => ExpenseItem(
                    id: tx.id,
                    title: tx.title,
                    category: tx.category,
                    amountKes: tx.amountKes,
                    occurredAt: tx.occurredAt,
                  ),
                )
                .toList(),
          ),
        );
  }

  @override
  Future<void> addManualTransaction({
    required String title,
    required String category,
    required double amountKes,
    DateTime? occurredAt,
  }) async {
    await _merchantLearningService.learn(
      merchantTitle: title,
      category: category,
    );
    await _store.addTransaction(
      title: title,
      category: category,
      amountKes: amountKes,
      occurredAt: occurredAt,
    );
  }

  @override
  Future<void> updateTransaction({
    required int transactionId,
    required String title,
    required String category,
    required double amountKes,
    required DateTime occurredAt,
  }) {
    return _merchantLearningService
        .learn(
          merchantTitle: title,
          category: category,
        )
        .then((_) => _store.updateTransaction(
              id: transactionId,
              title: title,
              category: category,
              amountKes: amountKes,
              occurredAt: occurredAt,
            ));
  }

  @override
  Future<void> deleteTransaction(int transactionId) {
    return _store.deleteTransaction(transactionId);
  }

  @override
  Future<int> importSmsMessages(
    List<String> rawMessages, {
    DateTime? from,
  }) async {
    await _store.ensureInitialized();
    final parsedTransactions = _parser.parseMany(rawMessages).where((tx) {
      if (from == null) {
        return true;
      }
      return !tx.occurredAt.isBefore(from);
    }).toList();
    var inserted = 0;
    final seenHashes = <String>{};
    for (final tx in parsedTransactions) {
      final hash = _parser.sourceHash(tx.rawMessage);
      if (seenHashes.contains(hash)) {
        continue;
      }
      seenHashes.add(hash);
      final exists = await _store.executor.runSelect(
        'SELECT id FROM transactions WHERE source_hash = ? LIMIT 1',
        [hash],
      );
      if (exists.isNotEmpty) {
        continue;
      }
      final learnedCategory = await _merchantLearningService.resolveCategory(
        merchantTitle: tx.title,
        fallbackCategory: tx.category,
      );
      await _store.addTransaction(
        title: tx.title,
        category: learnedCategory,
        amountKes: tx.amountKes,
        occurredAt: tx.occurredAt,
        source: 'sms',
        sourceHash: hash,
      );
      inserted += 1;
    }
    return inserted;
  }

  @override
  Future<int> importFromDevice({
    DateTime? from,
  }) async {
    final messages =
        await _deviceSmsDataSource.loadLikelyMpesaMessages(from: from);
    if (messages.isEmpty) {
      return 0;
    }
    return importSmsMessages(messages, from: from);
  }
}
