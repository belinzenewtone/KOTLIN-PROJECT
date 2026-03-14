class UserProfile {
  const UserProfile({
    required this.name,
    required this.email,
    required this.phone,
    required this.memberSinceLabel,
    required this.verified,
    this.avatarUrl,
  });

  final String name;
  final String email;
  final String phone;
  final String memberSinceLabel;
  final bool verified;
  final String? avatarUrl;
}
