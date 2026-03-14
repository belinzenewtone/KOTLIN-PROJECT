import 'dart:convert';

import 'package:crypto/crypto.dart';

class PasswordHasher {
  String hash(String password) {
    return sha256.convert(utf8.encode(password)).toString();
  }
}
