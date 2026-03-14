import 'package:beltech/features/auth/domain/entities/account_session.dart';
import 'package:beltech/features/auth/domain/repositories/account_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseAccountRepositoryImpl implements AccountRepository {
  SupabaseAccountRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<AccountSession> watchSession() {
    return Stream<AccountSession>.multi((controller) {
      controller.add(currentSession());
      final subscription = _client.auth.onAuthStateChange.listen((event) {
        controller.add(_mapUser(event.session?.user));
      });
      controller.onCancel = subscription.cancel;
    });
  }

  @override
  AccountSession currentSession() {
    return _mapUser(_client.auth.currentUser);
  }

  @override
  Future<void> signIn({
    required String email,
    required String password,
  }) async {
    await _client.auth.signInWithPassword(
      email: email,
      password: password,
    );
  }

  @override
  Future<void> signUp({
    required String name,
    required String email,
    required String phone,
    required String password,
  }) async {
    final response = await _client.auth.signUp(
      email: email,
      password: password,
      data: {
        'name': name,
        'phone': phone,
      },
    );
    if (response.session == null) {
      await _client.auth.signInWithPassword(email: email, password: password);
    }
  }

  @override
  Future<void> signOut() {
    return _client.auth.signOut();
  }

  AccountSession _mapUser(User? user) {
    if (user == null) {
      return AccountSession.unauthenticated;
    }
    return AccountSession(
      isAuthenticated: true,
      userId: user.id,
      email: user.email,
      displayName: '${user.userMetadata?['name'] ?? ''}'.trim().isEmpty
          ? user.email
          : '${user.userMetadata?['name']}',
    );
  }
}
