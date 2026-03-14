import 'package:beltech/data/local/drift/assistant_profile_store.dart';
import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/features/assistant/domain/entities/assistant_message.dart';
import 'package:beltech/features/assistant/domain/repositories/assistant_repository.dart';
import 'package:beltech/features/assistant/data/services/assistant_proxy_service.dart';

class AssistantRepositoryImpl implements AssistantRepository {
  AssistantRepositoryImpl(this._store, this._appStore,
      {AssistantProxyService? proxyService})
      : _proxyService = proxyService;

  final AssistantProfileStore _store;
  final AppDriftStore _appStore;
  final AssistantProxyService? _proxyService;

  @override
  Stream<List<AssistantMessage>> watchConversation() {
    return _store.watchMessages().map(
          (rows) => rows
              .map(
                (row) => AssistantMessage(
                  id: row.id,
                  text: row.text,
                  isUser: row.isUser,
                  createdAt: row.createdAt,
                ),
              )
              .toList(),
        );
  }

  @override
  List<AssistantSuggestion> suggestions() {
    return const [
      AssistantSuggestion('How much did I spend today?'),
      AssistantSuggestion("What's my biggest expense this month?"),
      AssistantSuggestion('Show my spending by category'),
      AssistantSuggestion('How many tasks are pending?'),
      AssistantSuggestion('What events do I have this week?'),
      AssistantSuggestion('Am I spending more than last week?'),
    ];
  }

  @override
  Future<void> sendMessage(String text) async {
    final normalized = text.trim();
    if (normalized.isEmpty) {
      return;
    }
    await _store.addAssistantMessage(text: normalized, isUser: true);
    await _store.addAssistantMessage(
      text: await _buildReply(normalized),
      isUser: false,
    );
  }

  @override
  Future<void> clearConversation() async {
    await _store.clearAssistantMessages();
  }

  Future<String> _buildReply(String prompt) async {
    final lower = prompt.toLowerCase();
    final todaySpending = await _sumTransactions(
      DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day),
      DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day)
          .add(const Duration(days: 1)),
    );
    final topCategories = await _topCategoriesInCurrentMonth();
    final pendingCount = await _pendingTasks();
    final weekEvents = await _eventsThisWeek();
    final context = [
      'today_spending_kes: ${todaySpending.toStringAsFixed(2)}',
      'pending_tasks: $pendingCount',
      'events_this_week: $weekEvents',
      'top_categories: ${topCategories.map((item) => '${item.$1}=${item.$2.toStringAsFixed(2)}').join(', ')}',
    ].join('\n');

    final aiReply = await _proxyService?.generateReply(
      userPrompt: prompt,
      analyticsContext: context,
    );
    if (aiReply != null && aiReply.isNotEmpty) {
      return aiReply;
    }

    if (_containsAny(lower, ['how much', 'spend', 'spent', 'today'])) {
      return 'Your spending today is ${CurrencyFormatter.money(todaySpending)}.';
    }
    if (_containsAny(lower, ['biggest', 'largest']) &&
        lower.contains('expense')) {
      final top = await _topCategoryInCurrentMonth();
      if (top == null) {
        return 'No expenses found this month yet.';
      }
      return 'Your biggest expense category this month is ${top.$1} at ${CurrencyFormatter.money(top.$2)}.';
    }
    if (_containsAny(lower, ['category', 'categories']) &&
        _containsAny(lower, ['spend', 'expense'])) {
      final categories = await _topCategoriesInCurrentMonth();
      if (categories.isEmpty) {
        return 'No categorized spending found for this month.';
      }
      final summary = categories
          .map((item) => '${item.$1}: ${CurrencyFormatter.money(item.$2)}')
          .join(', ');
      return 'Your spending by category this month: $summary.';
    }
    if (lower.contains('task')) {
      final pending = await _pendingTasks();
      return 'You currently have $pending pending task${pending == 1 ? '' : 's'}.';
    }
    if (_containsAny(lower, ['event', 'calendar', 'schedule']) &&
        lower.contains('week')) {
      final count = await _eventsThisWeek();
      return count == 0
          ? 'You have no events scheduled this week.'
          : 'You have $count event${count == 1 ? '' : 's'} scheduled this week.';
    }
    if (_containsAny(
        lower, ['more than last week', 'than last week', 'compare'])) {
      final now = DateTime.now();
      final thisWeekStart = DateTime(now.year, now.month, now.day)
          .subtract(Duration(days: now.weekday - 1));
      final thisWeekEnd = thisWeekStart.add(const Duration(days: 7));
      final previousWeekStart = thisWeekStart.subtract(const Duration(days: 7));
      final previousWeekEnd = thisWeekStart;
      final thisWeek = await _sumTransactions(thisWeekStart, thisWeekEnd);
      final lastWeek =
          await _sumTransactions(previousWeekStart, previousWeekEnd);
      if (thisWeek > lastWeek) {
        return 'You are spending more than last week by ${CurrencyFormatter.money(thisWeek - lastWeek)}.';
      }
      if (thisWeek < lastWeek) {
        return 'You are spending less than last week by ${CurrencyFormatter.money(lastWeek - thisWeek)}.';
      }
      return 'Your spending is equal to last week at ${CurrencyFormatter.money(thisWeek)}.';
    }
    if (lower.contains('event') ||
        lower.contains('calendar') ||
        lower.contains('schedule')) {
      final todayEvents = await _eventsForDay(DateTime.now());
      return todayEvents == 0
          ? 'No events for today. You can add one from Calendar.'
          : 'You have $todayEvents event${todayEvents == 1 ? '' : 's'} today.';
    }
    return 'I can help with spending, tasks, calendar events, and profile activity.';
  }

  Future<double> _sumTransactions(DateTime start, DateTime end) async {
    await _appStore.ensureInitialized();
    final rows = await _appStore.executor.runSelect(
      'SELECT COALESCE(SUM(amount), 0) AS total FROM transactions WHERE occurred_at >= ? AND occurred_at < ?',
      [start.millisecondsSinceEpoch, end.millisecondsSinceEpoch],
    );
    final value = rows.first['total'];
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse('$value') ?? 0;
  }

  Future<(String, double)?> _topCategoryInCurrentMonth() async {
    final top = await _topCategoriesInCurrentMonth();
    return top.isEmpty ? null : top.first;
  }

  Future<List<(String, double)>> _topCategoriesInCurrentMonth() async {
    await _appStore.ensureInitialized();
    final now = DateTime.now();
    final monthStart = DateTime(now.year, now.month, 1);
    final monthEnd = DateTime(now.year, now.month + 1, 1);
    final rows = await _appStore.executor.runSelect(
      'SELECT category, COALESCE(SUM(amount), 0) AS total '
      'FROM transactions WHERE occurred_at >= ? AND occurred_at < ? '
      'GROUP BY category ORDER BY total DESC LIMIT 3',
      [monthStart.millisecondsSinceEpoch, monthEnd.millisecondsSinceEpoch],
    );
    return rows.map((row) {
      final category = (row['category'] ?? 'Other') as String;
      final total = row['total'];
      final amount =
          total is num ? total.toDouble() : (double.tryParse('$total') ?? 0);
      return (category, amount);
    }).toList();
  }

  Future<int> _pendingTasks() async {
    await _appStore.ensureInitialized();
    final rows = await _appStore.executor.runSelect(
      'SELECT COUNT(*) AS total FROM tasks WHERE completed = 0',
      const [],
    );
    final value = rows.first['total'];
    if (value is num) {
      return value.toInt();
    }
    return int.tryParse('$value') ?? 0;
  }

  Future<int> _eventsForDay(DateTime day) async {
    final events = await _appStore.watchEventsForDay(day).first;
    return events.length;
  }

  Future<int> _eventsThisWeek() async {
    final now = DateTime.now();
    final weekStart = DateTime(now.year, now.month, now.day)
        .subtract(Duration(days: now.weekday - 1));
    var count = 0;
    for (var i = 0; i < 7; i++) {
      count += await _eventsForDay(weekStart.add(Duration(days: i)));
    }
    return count;
  }

  bool _containsAny(String source, List<String> markers) {
    for (final marker in markers) {
      if (source.contains(marker)) {
        return true;
      }
    }
    return false;
  }
}
