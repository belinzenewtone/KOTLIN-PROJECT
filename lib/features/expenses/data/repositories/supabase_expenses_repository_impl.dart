import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/expenses/data/services/device_sms_data_source.dart';
import 'package:beltech/features/expenses/data/services/merchant_learning_service.dart';
import 'package:beltech/features/expenses/data/services/mpesa_parser_service.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:beltech/features/expenses/domain/repositories/expenses_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseExpensesRepositoryImpl implements ExpensesRepository {
  SupabaseExpensesRepositoryImpl(
    this._client,
    this._parser, [
    MerchantLearningService? merchantLearningService,
    DeviceSmsDataSource? deviceSmsDataSource,
  ])  : _merchantLearningService =
            merchantLearningService ?? MerchantLearningService(),
        _deviceSmsDataSource = deviceSmsDataSource ?? DeviceSmsDataSource();

  final SupabaseClient _client;
  final MpesaParserService _parser;
  final MerchantLearningService _merchantLearningService;
  final DeviceSmsDataSource _deviceSmsDataSource;

  @override
  Stream<ExpensesSnapshot> watchSnapshot() => pollStream(_loadSnapshot);

  @override
  Future<void> addManualTransaction({
    required String title,
    required String category,
    required double amountKes,
    DateTime? occurredAt,
  }) {
    final userId = _requireUserId();
    return _merchantLearningService
        .learn(
          merchantTitle: title,
          category: category,
        )
        .then((_) => _client.from('transactions').insert({
              'owner_id': userId,
              'title': title,
              'category': category,
              'amount': amountKes,
              'occurred_at':
                  (occurredAt ?? DateTime.now()).toUtc().toIso8601String(),
              'source': 'manual',
              'source_hash': null,
            }));
  }

  @override
  Future<void> updateTransaction({
    required int transactionId,
    required String title,
    required String category,
    required double amountKes,
    required DateTime occurredAt,
  }) {
    final userId = _requireUserId();
    return _merchantLearningService
        .learn(
          merchantTitle: title,
          category: category,
        )
        .then(
          (_) => _client
              .from('transactions')
              .update({
                'title': title,
                'category': category,
                'amount': amountKes,
                'occurred_at': occurredAt.toUtc().toIso8601String(),
                'source': 'manual',
                'source_hash': null,
              })
              .eq('id', transactionId)
              .eq('owner_id', userId),
        );
  }

  @override
  Future<void> deleteTransaction(int transactionId) {
    final userId = _requireUserId();
    return _client
        .from('transactions')
        .delete()
        .eq('id', transactionId)
        .eq('owner_id', userId);
  }

  @override
  Future<int> importSmsMessages(
    List<String> rawMessages, {
    DateTime? from,
  }) async {
    final userId = _requireUserId();
    final parsed = _parser.parseMany(rawMessages).where((tx) {
      if (from == null) {
        return true;
      }
      return !tx.occurredAt.isBefore(from);
    }).toList();
    if (parsed.isEmpty) {
      return 0;
    }
    final seenHashes = <String>{};
    final payload = <Map<String, dynamic>>[];
    for (final tx in parsed) {
      final hash = _parser.sourceHash(tx.rawMessage);
      if (!seenHashes.add(hash)) {
        continue;
      }
      final existing = await _client
          .from('transactions')
          .select('id')
          .eq('owner_id', userId)
          .eq('source_hash', hash)
          .limit(1);
      if ((existing as List).isNotEmpty) {
        continue;
      }
      final learnedCategory = await _merchantLearningService.resolveCategory(
        merchantTitle: tx.title,
        fallbackCategory: tx.category,
      );
      payload.add({
        'owner_id': userId,
        'title': tx.title,
        'category': learnedCategory,
        'amount': tx.amountKes,
        'occurred_at': tx.occurredAt.toUtc().toIso8601String(),
        'source': 'sms',
        'source_hash': hash,
      });
    }
    if (payload.isEmpty) {
      return 0;
    }
    await _client.from('transactions').insert(payload);
    return payload.length;
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

  Future<ExpensesSnapshot> _loadSnapshot() async {
    final userId = _requireUserId();
    final now = DateTime.now();
    final todayStart = DateTime(now.year, now.month, now.day);
    final tomorrowStart = todayStart.add(const Duration(days: 1));
    final weekStart = todayStart.subtract(Duration(days: now.weekday - 1));
    final weekEnd = weekStart.add(const Duration(days: 7));

    final rows = await _client
        .from('transactions')
        .select('id,title,category,amount,occurred_at')
        .eq('owner_id', userId)
        .order('occurred_at', ascending: false)
        .limit(5000);
    final transactionsRaw = (rows as List).cast<Map<String, dynamic>>();

    final transactions = transactionsRaw.map((row) {
      return ExpenseItem(
        id: parseInt(row['id']),
        title: '${row['title'] ?? ''}',
        category: '${row['category'] ?? 'Other'}',
        amountKes: parseDouble(row['amount']),
        occurredAt: parseTimestamp(row['occurred_at']),
      );
    }).toList();

    final categoryTotals = <String, double>{};
    for (final tx in transactions) {
      categoryTotals[tx.category] =
          (categoryTotals[tx.category] ?? 0) + tx.amountKes;
    }
    final categories = categoryTotals.entries
        .map((entry) =>
            CategoryExpenseTotal(category: entry.key, totalKes: entry.value))
        .toList()
      ..sort((a, b) => b.totalKes.compareTo(a.totalKes));

    double sumBetween(DateTime start, DateTime end) {
      var total = 0.0;
      for (final tx in transactions) {
        if (!tx.occurredAt.isBefore(start) && tx.occurredAt.isBefore(end)) {
          total += tx.amountKes;
        }
      }
      return total;
    }

    return ExpensesSnapshot(
      todayKes: sumBetween(todayStart, tomorrowStart),
      weekKes: sumBetween(weekStart, weekEnd),
      categories: categories,
      transactions: transactions,
    );
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
