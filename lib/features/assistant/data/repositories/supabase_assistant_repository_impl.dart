import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/features/assistant/data/services/assistant_proxy_service.dart';
import 'package:beltech/features/assistant/domain/entities/assistant_message.dart';
import 'package:beltech/features/assistant/domain/repositories/assistant_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseAssistantRepositoryImpl implements AssistantRepository {
  SupabaseAssistantRepositoryImpl(this._client,
      {AssistantProxyService? proxyService})
      : _proxyService = proxyService;

  static const String _introMessage =
      "Hey! I'm your BELTECH assistant. Ask me about spending, tasks, or schedule.";

  final SupabaseClient _client;
  final AssistantProxyService? _proxyService;

  @override
  Stream<List<AssistantMessage>> watchConversation() =>
      pollStream(_loadConversation);

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
    final userId = _requireUserId();
    final normalized = text.trim();
    if (normalized.isEmpty) {
      return;
    }
    await _client.from('assistant_messages').insert({
      'owner_id': userId,
      'text': normalized,
      'is_user': true,
      'created_at': DateTime.now().toUtc().toIso8601String(),
    });
    await _client.from('assistant_messages').insert({
      'owner_id': userId,
      'text': await _buildReply(normalized),
      'is_user': false,
      'created_at': DateTime.now().toUtc().toIso8601String(),
    });
  }

  @override
  Future<void> clearConversation() async {
    final userId = _requireUserId();
    await _client.from('assistant_messages').delete().eq('owner_id', userId);
    await _client.from('assistant_messages').insert({
      'owner_id': userId,
      'text': _introMessage,
      'is_user': false,
      'created_at': DateTime.now().toUtc().toIso8601String(),
    });
  }

  Future<List<AssistantMessage>> _loadConversation() async {
    final userId = _requireUserId();
    final rows = await _client
        .from('assistant_messages')
        .select('id,text,is_user,created_at')
        .eq('owner_id', userId)
        .order('created_at')
        .order('id');
    final messages = (rows as List).cast<Map<String, dynamic>>();
    if (messages.isEmpty) {
      await _client.from('assistant_messages').insert({
        'owner_id': userId,
        'text': _introMessage,
        'is_user': false,
        'created_at': DateTime.now().toUtc().toIso8601String(),
      });
      return _loadConversation();
    }
    return messages
        .map(
          (row) => AssistantMessage(
            id: 'msg-${row['id']}',
            text: '${row['text'] ?? ''}',
            isUser: row['is_user'] == true,
            createdAt: parseTimestamp(row['created_at']),
          ),
        )
        .toList();
  }

  Future<String> _buildReply(String prompt) async {
    final lower = prompt.toLowerCase();
    final now = DateTime.now();
    final dayStart = DateTime(now.year, now.month, now.day);
    final dayEnd = dayStart.add(const Duration(days: 1));
    final todaySpending = await _sumTransactions(dayStart, dayEnd);
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
      final categories = await _topCategoriesInCurrentMonth();
      if (categories.isEmpty) {
        return 'No expenses found this month yet.';
      }
      final top = categories.first;
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
    return 'I can help with spending, tasks, calendar events, and profile activity.';
  }

  Future<double> _sumTransactions(DateTime start, DateTime end) async {
    final userId = _requireUserId();
    final rows = await _client
        .from('transactions')
        .select('amount')
        .eq('owner_id', userId)
        .gte('occurred_at', start.toUtc().toIso8601String())
        .lt('occurred_at', end.toUtc().toIso8601String());
    final items = (rows as List).cast<Map<String, dynamic>>();
    return items.fold<double>(
        0, (sum, row) => sum + parseDouble(row['amount']));
  }

  Future<List<(String, double)>> _topCategoriesInCurrentMonth() async {
    final userId = _requireUserId();
    final now = DateTime.now();
    final monthStart = DateTime(now.year, now.month, 1);
    final monthEnd = DateTime(now.year, now.month + 1, 1);
    final rows = await _client
        .from('transactions')
        .select('category,amount')
        .eq('owner_id', userId)
        .gte('occurred_at', monthStart.toUtc().toIso8601String())
        .lt('occurred_at', monthEnd.toUtc().toIso8601String());
    final items = (rows as List).cast<Map<String, dynamic>>();
    final totals = <String, double>{};
    for (final row in items) {
      final category = '${row['category'] ?? 'Other'}';
      totals[category] = (totals[category] ?? 0) + parseDouble(row['amount']);
    }
    final list = totals.entries
        .map((entry) => (entry.key, entry.value))
        .toList()
      ..sort((a, b) => b.$2.compareTo(a.$2));
    return list.take(3).toList();
  }

  Future<int> _pendingTasks() async {
    final userId = _requireUserId();
    final rows = await _client
        .from('tasks')
        .select('id')
        .eq('owner_id', userId)
        .eq('completed', false);
    return (rows as List).length;
  }

  Future<int> _eventsThisWeek() async {
    final userId = _requireUserId();
    final now = DateTime.now();
    final weekStart = DateTime(now.year, now.month, now.day)
        .subtract(Duration(days: now.weekday - 1));
    final weekEnd = weekStart.add(const Duration(days: 7));
    final rows = await _client
        .from('events')
        .select('id')
        .eq('owner_id', userId)
        .gte('start_at', weekStart.toUtc().toIso8601String())
        .lt('start_at', weekEnd.toUtc().toIso8601String());
    return (rows as List).length;
  }

  bool _containsAny(String source, List<String> markers) {
    for (final marker in markers) {
      if (source.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
