DateTime parseTimestamp(dynamic value) {
  if (value is DateTime) {
    return value.toLocal();
  }
  return DateTime.parse('$value').toLocal();
}

double parseDouble(dynamic value) {
  if (value is num) {
    return value.toDouble();
  }
  return double.tryParse('$value') ?? 0;
}

int parseInt(dynamic value) {
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse('$value') ?? 0;
}
