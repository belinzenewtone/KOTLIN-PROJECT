import 'dart:async';

import 'package:beltech/core/platform/runtime_env.dart';
import 'package:beltech/features/auth/domain/repositories/account_repository.dart';
import 'package:beltech/features/expenses/domain/repositories/expenses_repository.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SmsAutoImportService {
  SmsAutoImportService(
    this._expensesRepository,
    this._accountRepository,
  );

  static const Duration defaultInterval = Duration(minutes: 30);
  static const Duration initialWindow = Duration(days: 90);
  static const String _keyPrefix = 'mpesa_auto_sync_last_ms';

  final ExpensesRepository _expensesRepository;
  final AccountRepository _accountRepository;

  Timer? _timer;
  bool _running = false;
  bool _syncInFlight = false;

  Future<void> start({Duration interval = defaultInterval}) async {
    if (hasRuntimeEnv('FLUTTER_TEST')) {
      return;
    }
    if (_running) {
      return;
    }
    _running = true;
    await syncNow();
    _timer = Timer.periodic(interval, (_) {
      unawaited(syncNow());
    });
  }

  Future<void> stop() async {
    _running = false;
    _timer?.cancel();
    _timer = null;
  }

  Future<int> syncNow() async {
    if (hasRuntimeEnv('FLUTTER_TEST')) {
      return 0;
    }
    if (_syncInFlight) {
      return 0;
    }
    _syncInFlight = true;
    try {
      final lastSync = await _loadLastSync();
      final from = lastSync == null
          ? DateTime.now().subtract(initialWindow)
          : lastSync.subtract(const Duration(minutes: 2));
      final imported = await _expensesRepository.importFromDevice(from: from);
      await _saveLastSync(DateTime.now());
      return imported;
    } catch (_) {
      return 0;
    } finally {
      _syncInFlight = false;
    }
  }

  Future<DateTime?> _loadLastSync() async {
    final prefs = await SharedPreferences.getInstance();
    final scope = _syncScopeKey();
    final value = prefs.getInt('$_keyPrefix.$scope');
    if (value == null) {
      return null;
    }
    return DateTime.fromMillisecondsSinceEpoch(value);
  }

  Future<void> _saveLastSync(DateTime at) async {
    final prefs = await SharedPreferences.getInstance();
    final scope = _syncScopeKey();
    await prefs.setInt('$_keyPrefix.$scope', at.millisecondsSinceEpoch);
  }

  String _syncScopeKey() {
    final session = _accountRepository.currentSession();
    if (session.userId != null && session.userId!.isNotEmpty) {
      return session.userId!;
    }
    return 'local';
  }
}
