import 'dart:async';

import 'package:beltech/features/auth/domain/entities/account_session.dart';
import 'package:beltech/features/auth/domain/repositories/account_repository.dart';

class LocalAccountRepositoryImpl implements AccountRepository {
  LocalAccountRepositoryImpl();

  final StreamController<AccountSession> _sessionController =
      StreamController<AccountSession>.broadcast();
  AccountSession _session = AccountSession.unauthenticated;

  @override
  Stream<AccountSession> watchSession() async* {
    yield _session;
    yield* _sessionController.stream;
  }

  @override
  AccountSession currentSession() {
    return _session;
  }

  @override
  Future<void> signIn({
    required String email,
    required String password,
  }) async {
    final fallbackName = email.trim().isEmpty ? 'Local User' : email.trim();
    _session = AccountSession(
      isAuthenticated: true,
      userId: 'local-user',
      email: email.trim().isEmpty ? 'local@device' : email.trim(),
      displayName: fallbackName,
    );
    _sessionController.add(_session);
  }

  @override
  Future<void> signUp({
    required String name,
    required String email,
    required String phone,
    required String password,
  }) async {
    _session = AccountSession(
      isAuthenticated: true,
      userId: 'local-user',
      email: email.trim().isEmpty ? 'local@device' : email.trim(),
      displayName: name.trim().isEmpty ? 'Local User' : name.trim(),
    );
    _sessionController.add(_session);
  }

  @override
  Future<void> signOut() async {
    _session = AccountSession.unauthenticated;
    _sessionController.add(_session);
  }
}
