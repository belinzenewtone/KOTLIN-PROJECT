import 'package:beltech/features/auth/data/repositories/local_account_repository_impl.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('starts unauthenticated by default', () async {
    final repository = LocalAccountRepositoryImpl();

    final first = await repository.watchSession().first;
    expect(first.isAuthenticated, isFalse);
    expect(repository.currentSession().isAuthenticated, isFalse);
  });

  test('sign up authenticates local session', () async {
    final repository = LocalAccountRepositoryImpl();

    await repository.signUp(
      name: 'Jane Local',
      email: 'jane@local.dev',
      phone: '0700000000',
      password: 'password123',
    );

    final current = repository.currentSession();
    expect(current.isAuthenticated, isTrue);
    expect(current.email, 'jane@local.dev');
    expect(current.displayName, 'Jane Local');
  });
}
