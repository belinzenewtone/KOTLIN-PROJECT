import 'dart:io';

import 'package:drift/backends.dart';
import 'package:drift/drift.dart' show LazyDatabase;
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

QueryExecutor openDriftExecutor({
  required String name,
  bool inMemory = false,
}) {
  if (inMemory) {
    return NativeDatabase.memory();
  }
  return LazyDatabase(() async {
    final directory = await getApplicationSupportDirectory();
    final path = p.join(directory.path, name);
    return NativeDatabase(File(path));
  });
}
