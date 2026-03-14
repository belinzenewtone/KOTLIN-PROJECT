import 'dart:convert';

import 'package:crypto/crypto.dart';

class ParsedMpesaTransaction {
  const ParsedMpesaTransaction({
    required this.title,
    required this.category,
    required this.amountKes,
    required this.occurredAt,
    required this.rawMessage,
  });

  final String title;
  final String category;
  final double amountKes;
  final DateTime occurredAt;
  final String rawMessage;
}

class MpesaParserService {
  const MpesaParserService();

  static final RegExp _amountPattern = RegExp(
    r'ksh\s*([\d,]+(?:\.\d{1,2})?)',
    caseSensitive: false,
  );
  static final RegExp _dateTimePattern = RegExp(
    r'on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s?(?:am|pm)?)',
    caseSensitive: false,
  );
  static final RegExp _titlePattern = RegExp(
    r'(?:sent to|paid to|received from|for account|at)\s+([a-z0-9 .,&-]{3,})',
    caseSensitive: false,
  );

  List<ParsedMpesaTransaction> parseBulkText(String payload) {
    final chunks = payload
        .split(RegExp(r'(?:\r?\n){2,}'))
        .map((item) => item.trim())
        .where((item) => item.isNotEmpty)
        .toList();
    return parseMany(chunks);
  }

  List<ParsedMpesaTransaction> parseMany(List<String> messages) {
    final transactions = <ParsedMpesaTransaction>[];
    for (final message in messages) {
      final parsed = parseSingle(message);
      if (parsed != null) {
        transactions.add(parsed);
      }
    }
    return transactions;
  }

  String sourceHash(String message) {
    final normalized =
        message.trim().toLowerCase().replaceAll(RegExp(r'\s+'), ' ');
    return sha256.convert(utf8.encode(normalized)).toString();
  }

  ParsedMpesaTransaction? parseSingle(String message) {
    final cleaned = message.trim();
    if (cleaned.isEmpty) {
      return null;
    }
    if (!_looksLikeMpesaMessage(cleaned)) {
      return null;
    }

    final amountMatch = _amountPattern.firstMatch(cleaned);
    if (amountMatch == null) {
      return null;
    }
    final amount = _toAmount(amountMatch.group(1) ?? '');
    if (amount == null || amount <= 0) {
      return null;
    }

    final title = _extractTitle(cleaned);
    final occurredAt = _extractDateTime(cleaned) ?? DateTime.now();
    return ParsedMpesaTransaction(
      title: title,
      category: _categorize(title, cleaned),
      amountKes: amount,
      occurredAt: occurredAt,
      rawMessage: cleaned,
    );
  }

  String _extractTitle(String message) {
    final lower = message.toLowerCase();
    final titleMatch = _titlePattern.firstMatch(lower);
    if (titleMatch != null) {
      final raw = titleMatch.group(1) ?? '';
      return _titleCase(raw.replaceAll(RegExp(r'\s+'), ' ').trim());
    }
    if (lower.contains('airtime')) {
      return 'Airtime Topup';
    }
    if (lower.contains('token')) {
      return 'Electricity Token';
    }
    return 'MPESA Transaction';
  }

  DateTime? _extractDateTime(String message) {
    final match = _dateTimePattern.firstMatch(message);
    if (match == null) {
      return null;
    }
    final dateText = match.group(1);
    final timeText = match.group(2);
    if (dateText == null || timeText == null) {
      return null;
    }

    final dateParts = dateText.split('/');
    if (dateParts.length != 3) {
      return null;
    }
    var day = int.tryParse(dateParts[0]);
    var month = int.tryParse(dateParts[1]);
    var year = int.tryParse(dateParts[2]);
    if (day == null || month == null || year == null) {
      return null;
    }
    if (year < 100) {
      year += 2000;
    }

    final normalizedTime = timeText.trim().toLowerCase();
    final timePattern = RegExp(r'^(\d{1,2}):(\d{2})(?:\s?(am|pm))?$');
    final timeMatch = timePattern.firstMatch(normalizedTime);
    if (timeMatch == null) {
      return null;
    }
    var hour = int.tryParse(timeMatch.group(1) ?? '');
    final minute = int.tryParse(timeMatch.group(2) ?? '');
    final meridiem = timeMatch.group(3);
    if (hour == null || minute == null) {
      return null;
    }
    if (meridiem == 'pm' && hour < 12) {
      hour += 12;
    } else if (meridiem == 'am' && hour == 12) {
      hour = 0;
    }
    return DateTime(year, month, day, hour, minute);
  }

  double? _toAmount(String text) {
    final normalized = text.replaceAll(',', '').trim();
    return double.tryParse(normalized);
  }

  String _categorize(String title, String fullMessage) {
    final value = '$title ${fullMessage.toLowerCase()}';
    if (value.contains('salary') ||
        value.contains('payroll') ||
        value.contains('income')) {
      return 'Income';
    }
    if (value.contains('hotel') ||
        value.contains('restaurant') ||
        value.contains('food') ||
        value.contains('cafe') ||
        value.contains('kitchen')) {
      return 'Food';
    }
    if (value.contains('airtime') || value.contains('bundle')) {
      return 'Airtime';
    }
    if (value.contains('token') ||
        value.contains('bill') ||
        value.contains('electricity') ||
        value.contains('water') ||
        value.contains('utility')) {
      return 'Bills';
    }
    if (value.contains('fuel') ||
        value.contains('transport') ||
        value.contains('uber') ||
        value.contains('bolt') ||
        value.contains('matatu') ||
        value.contains('taxi')) {
      return 'Transport';
    }
    if (value.contains('hospital') ||
        value.contains('clinic') ||
        value.contains('pharmacy') ||
        value.contains('medical')) {
      return 'Health';
    }
    if (value.contains('withdraw') ||
        value.contains('atm') ||
        value.contains('agent')) {
      return 'Cash';
    }
    if (value.contains('fee') ||
        value.contains('charge') ||
        value.contains('cost')) {
      return 'Fees';
    }
    return 'Other';
  }

  String _titleCase(String text) {
    final words = text.split(' ');
    return words.map((word) {
      if (word.isEmpty) {
        return word;
      }
      final first = word.substring(0, 1).toUpperCase();
      final rest = word.substring(1).toLowerCase();
      return '$first$rest';
    }).join(' ');
  }

  bool _looksLikeMpesaMessage(String message) {
    final lower = message.toLowerCase();
    final hasMoney = lower.contains('ksh');
    final hasTransactionVerb = lower.contains('sent to') ||
        lower.contains('paid to') ||
        lower.contains('received from') ||
        lower.contains('airtime') ||
        lower.contains('token') ||
        lower.contains('withdraw') ||
        lower.contains('deposit');
    final hasMpesaMarker = lower.contains('mpesa') ||
        (lower.contains('confirmed') &&
            RegExp(r'^[a-z0-9]{8,}\s+confirmed').hasMatch(lower));
    return hasMoney && hasTransactionVerb && hasMpesaMarker;
  }
}
