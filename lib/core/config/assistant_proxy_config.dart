import 'package:beltech/core/config/supabase_config.dart';

class AssistantProxyConfig {
  AssistantProxyConfig._();

  static const String explicitUrl =
      String.fromEnvironment('ASSISTANT_PROXY_URL');

  static String get endpoint {
    if (explicitUrl.isNotEmpty) {
      return explicitUrl;
    }
    if (SupabaseConfig.url.isNotEmpty) {
      return '${SupabaseConfig.url}/functions/v1/assistant-proxy';
    }
    return '';
  }

  static bool get isConfigured => endpoint.isNotEmpty;
}
