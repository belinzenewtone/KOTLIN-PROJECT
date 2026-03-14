class SupabaseConfig {
  SupabaseConfig._();

  static const String url = String.fromEnvironment('SUPABASE_URL');
  static const String publishableKey =
      String.fromEnvironment('SUPABASE_PUBLISHABLE_KEY');
  static const String legacyAnonKey =
      String.fromEnvironment('SUPABASE_ANON_KEY');

  static String get publicKey =>
      publishableKey.isNotEmpty ? publishableKey : legacyAnonKey;
  static bool get isConfigured => url.isNotEmpty && publicKey.isNotEmpty;
}
