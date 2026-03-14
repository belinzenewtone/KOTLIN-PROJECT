part of 'data_mode_migration_service.dart';

class _MigrationCodec {
  Map<String, int> emptyScopeCounts(List<String> orderedScopes) {
    return {
      for (final scope in orderedScopes) scope: 0,
    };
  }

  List<Map<String, dynamic>> toMapList(dynamic rows) {
    if (rows is! List) {
      return const [];
    }
    final normalized = <Map<String, dynamic>>[];
    for (final row in rows) {
      if (row is Map<String, dynamic>) {
        normalized.add(Map<String, dynamic>.from(row));
        continue;
      }
      if (row is Map) {
        normalized.add(row.map((key, value) => MapEntry('$key', value)));
      }
    }
    return normalized;
  }

  String transactionSignature(Map<String, dynamic> row) {
    final sourceHash = nullableText(row['source_hash']);
    if (sourceHash != null && sourceHash.isNotEmpty) {
      return 'hash:${normalized(sourceHash)}';
    }
    return [
      normalized(row['title']),
      normalized(row['category']),
      doubleSignature(row['amount']),
      asEpochMs(row['occurred_at']),
      normalized(row['source']),
    ].join('|');
  }

  String incomeSignature(Map<String, dynamic> row) {
    return [
      normalized(row['title']),
      doubleSignature(row['amount']),
      asEpochMs(row['received_at']),
      normalized(row['source']),
    ].join('|');
  }

  String taskSignature(Map<String, dynamic> row) {
    return [
      normalized(row['title']),
      normalized(row['description']),
      asBool(row['completed']) ? '1' : '0',
      nullableEpochMs(row['due_at']) ?? 0,
      normalized(row['priority']),
    ].join('|');
  }

  String eventSignature(Map<String, dynamic> row) {
    return [
      normalized(row['title']),
      asEpochMs(row['start_at']),
      nullableEpochMs(row['end_at']) ?? 0,
      normalized(row['note']),
      asBool(row['completed']) ? '1' : '0',
      normalized(row['priority']),
      normalized(row['event_type']),
    ].join('|');
  }

  String recurringSignature(Map<String, dynamic> row) {
    return [
      normalized(row['kind']),
      normalized(row['title']),
      normalized(row['description']),
      normalized(row['category']),
      optionalDoubleSignature(row['amount']),
      normalized(row['priority']),
      normalized(row['cadence']),
      asEpochMs(row['next_run_at']),
      asBool(row['enabled']) ? '1' : '0',
    ].join('|');
  }

  int asInt(Object? value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    return int.tryParse('${value ?? ''}') ?? 0;
  }

  double asDouble(Object? value) {
    if (value is double) {
      return value;
    }
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse('${value ?? ''}') ?? 0;
  }

  double? nullableDouble(Object? value) {
    if (value == null) {
      return null;
    }
    final text = '$value'.trim();
    if (text.isEmpty) {
      return null;
    }
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(text);
  }

  bool asBool(Object? value) {
    if (value is bool) {
      return value;
    }
    if (value is num) {
      return value != 0;
    }
    final text = '${value ?? ''}'.trim().toLowerCase();
    return text == 'true' || text == '1' || text == 'yes';
  }

  int asEpochMs(Object? value) {
    if (value == null) {
      return 0;
    }
    if (value is DateTime) {
      return value.toUtc().millisecondsSinceEpoch;
    }
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    final text = '$value'.trim();
    if (text.isEmpty) {
      return 0;
    }
    final parsedTime = DateTime.tryParse(text);
    if (parsedTime != null) {
      return parsedTime.toUtc().millisecondsSinceEpoch;
    }
    return int.tryParse(text) ?? 0;
  }

  int? nullableEpochMs(Object? value) {
    if (value == null) {
      return null;
    }
    final text = '$value'.trim();
    if (text.isEmpty) {
      return null;
    }
    return asEpochMs(value);
  }

  String requiredIsoUtc(Object? value) {
    final ms = asEpochMs(value);
    final safeMs = ms > 0 ? ms : DateTime.now().toUtc().millisecondsSinceEpoch;
    return DateTime.fromMillisecondsSinceEpoch(safeMs, isUtc: true)
        .toIso8601String();
  }

  String? nullableIsoUtc(Object? value) {
    final ms = nullableEpochMs(value);
    if (ms == null) {
      return null;
    }
    return DateTime.fromMillisecondsSinceEpoch(ms, isUtc: true)
        .toIso8601String();
  }

  String requiredText(Object? value, {required String fallback}) {
    final text = '${value ?? ''}'.trim();
    return text.isEmpty ? fallback : text;
  }

  String? nullableText(Object? value) {
    if (value == null) {
      return null;
    }
    final text = '$value'.trim();
    return text.isEmpty ? null : text;
  }

  String normalized(Object? value) {
    return '${value ?? ''}'.trim().toLowerCase();
  }

  String doubleSignature(Object? value) {
    return asDouble(value).toStringAsFixed(4);
  }

  String optionalDoubleSignature(Object? value) {
    final normalizedValue = '${value ?? ''}'.trim();
    if (normalizedValue.isEmpty) {
      return '';
    }
    return asDouble(value).toStringAsFixed(4);
  }

  bool doubleEquals(double left, double right) {
    return (left - right).abs() < 0.0001;
  }
}
