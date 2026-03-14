import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:supabase_flutter/supabase_flutter.dart';

class AssistantProxyService {
  AssistantProxyService({
    required this.endpoint,
    SupabaseClient? supabaseClient,
    http.Client? client,
  })  : _supabaseClient = supabaseClient,
        _client = client ?? http.Client();

  final String endpoint;
  final SupabaseClient? _supabaseClient;
  final http.Client _client;

  Future<String?> generateReply({
    required String userPrompt,
    required String analyticsContext,
  }) async {
    if (endpoint.isEmpty) {
      return null;
    }
    try {
      final headers = <String, String>{
        'Content-Type': 'application/json',
      };
      final accessToken = _supabaseClient?.auth.currentSession?.accessToken;
      if (accessToken != null && accessToken.isNotEmpty) {
        headers['Authorization'] = 'Bearer $accessToken';
      }
      final response = await _client.post(
        Uri.parse(endpoint),
        headers: headers,
        body: jsonEncode({
          'prompt': userPrompt,
          'context': analyticsContext,
        }),
      );
      if (response.statusCode < 200 || response.statusCode >= 300) {
        return null;
      }
      final payload = jsonDecode(response.body);
      if (payload is Map<String, dynamic>) {
        final reply = payload['reply'];
        if (reply is String && reply.trim().isNotEmpty) {
          return reply.trim();
        }
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}
