class AccountSession {
  const AccountSession({
    required this.isAuthenticated,
    this.userId,
    this.email,
    this.displayName,
  });

  final bool isAuthenticated;
  final String? userId;
  final String? email;
  final String? displayName;

  static const AccountSession unauthenticated = AccountSession(
    isAuthenticated: false,
  );
}
