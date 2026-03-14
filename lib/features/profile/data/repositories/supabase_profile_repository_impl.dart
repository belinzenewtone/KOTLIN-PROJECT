import 'dart:typed_data';

import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/profile/domain/entities/user_profile.dart';
import 'package:beltech/features/profile/domain/repositories/profile_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseProfileRepositoryImpl implements ProfileRepository {
  SupabaseProfileRepositoryImpl(this._client);

  final SupabaseClient _client;
  static const String _avatarBucket = 'avatars';

  @override
  Stream<UserProfile> watchProfile() => pollStream(_loadProfile);

  @override
  Future<void> updateProfile({
    required String name,
    required String email,
    required String phone,
  }) {
    final userId = _requireUserId();
    return _client.from('user_profile').upsert({
      'id': userId,
      'name': name,
      'email': email,
      'phone': phone,
      'member_since_label': _memberSinceLabel(),
      'verified': true,
    });
  }

  @override
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    final email = _client.auth.currentUser?.email;
    if (email == null || email.isEmpty) {
      throw Exception('Sign in is required.');
    }
    await _client.auth.signInWithPassword(
      email: email,
      password: currentPassword,
    );
    await _client.auth.updateUser(UserAttributes(password: newPassword));
  }

  @override
  Future<void> updateAvatar({
    required Uint8List bytes,
    required String fileExtension,
  }) async {
    final userId = _requireUserId();
    final ext = _normalizeExtension(fileExtension);
    final path = '$userId/avatar_${DateTime.now().millisecondsSinceEpoch}.$ext';
    await _client.storage.from(_avatarBucket).uploadBinary(
          path,
          bytes,
          fileOptions: FileOptions(
            upsert: true,
            contentType: _contentTypeFor(ext),
          ),
        );
    final url = _client.storage.from(_avatarBucket).getPublicUrl(path);
    await _client
        .from('user_profile')
        .update({'avatar_url': url}).eq('id', userId);
  }

  Future<UserProfile> _loadProfile() async {
    final user = _requireUser();
    final row = await _client
        .from('user_profile')
        .select('name,email,phone,member_since_label,verified,avatar_url')
        .eq('id', user.id)
        .maybeSingle();
    if (row == null) {
      final initialName = '${user.userMetadata?['name'] ?? ''}'.trim();
      final initialPhone = '${user.userMetadata?['phone'] ?? ''}'.trim();
      await updateProfile(
        name: initialName.isEmpty
            ? _defaultNameFromEmail(user.email)
            : initialName,
        email: user.email ?? '',
        phone: initialPhone.isEmpty ? '-' : initialPhone,
      );
      return UserProfile(
        name: initialName.isEmpty
            ? _defaultNameFromEmail(user.email)
            : initialName,
        email: user.email ?? '',
        phone: initialPhone.isEmpty ? '-' : initialPhone,
        memberSinceLabel: _memberSinceLabel(),
        verified: user.emailConfirmedAt != null,
        avatarUrl: null,
      );
    }
    return UserProfile(
      name: '${row['name'] ?? ''}',
      email: '${row['email'] ?? ''}',
      phone: '${row['phone'] ?? ''}',
      memberSinceLabel: '${row['member_since_label'] ?? ''}',
      verified: row['verified'] == true,
      avatarUrl: row['avatar_url'] as String?,
    );
  }

  User _requireUser() {
    final user = _client.auth.currentUser;
    if (user == null) {
      throw Exception('Sign in is required.');
    }
    return user;
  }

  String _requireUserId() => _requireUser().id;

  String _defaultNameFromEmail(String? email) {
    final value = email ?? '';
    if (!value.contains('@')) {
      return 'User';
    }
    return value.split('@').first;
  }

  String _memberSinceLabel() {
    final createdAt = _client.auth.currentUser?.createdAt;
    final parsed = DateTime.tryParse(createdAt ?? '') ?? DateTime.now();
    const weekdays = [
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
      'Sunday'
    ];
    const months = [
      'January',
      'February',
      'March',
      'April',
      'May',
      'June',
      'July',
      'August',
      'September',
      'October',
      'November',
      'December',
    ];
    final weekday = weekdays[parsed.weekday - 1];
    final month = months[parsed.month - 1];
    final day = parsed.day.toString().padLeft(2, '0');
    return '$weekday, $month $day, ${parsed.year}';
  }

  String _normalizeExtension(String extension) {
    final value = extension.toLowerCase().replaceAll('.', '');
    if (value == 'jpg') {
      return 'jpeg';
    }
    return switch (value) {
      'jpeg' || 'png' || 'webp' => value,
      _ => 'jpeg',
    };
  }

  String _contentTypeFor(String extension) {
    return switch (extension) {
      'png' => 'image/png',
      'webp' => 'image/webp',
      _ => 'image/jpeg',
    };
  }
}
