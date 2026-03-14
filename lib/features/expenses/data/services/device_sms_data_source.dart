import 'package:flutter/foundation.dart';
import 'package:flutter_sms_inbox/flutter_sms_inbox.dart';
import 'package:permission_handler/permission_handler.dart';

typedef SmsPermissionRequester = Future<bool> Function();
typedef SmsQueryRunner = Future<List<SmsMessage>> Function(SmsQuery query);
typedef PlatformCheck = bool Function();

class DeviceSmsDataSource {
  DeviceSmsDataSource({
    SmsQuery? query,
    SmsPermissionRequester? requestPermission,
    SmsQueryRunner? queryRunner,
    PlatformCheck? isAndroid,
  })  : _query = query ?? SmsQuery(),
        _requestPermission = requestPermission ?? _defaultPermission,
        _queryRunner = queryRunner ?? _defaultQueryRunner,
        _isAndroid = isAndroid ?? _defaultIsAndroid;

  final SmsQuery _query;
  final SmsPermissionRequester _requestPermission;
  final SmsQueryRunner _queryRunner;
  final PlatformCheck _isAndroid;

  Future<List<String>> loadLikelyMpesaMessages({DateTime? from}) async {
    if (!_isAndroid()) {
      return const [];
    }
    if (!await _requestPermission()) {
      return const [];
    }

    final messages = await _queryRunner(_query);

    return messages
        .where((message) {
          final body = message.body?.trim() ?? '';
          if (body.isEmpty) {
            return false;
          }
          if (from != null) {
            final at = message.date;
            if (at == null || at.isBefore(from)) {
              return false;
            }
          }
          final sender = (message.address ?? '').toLowerCase();
          final normalized = body.toLowerCase();
          final mpesaSender = sender.contains('mpesa');
          final mpesaBody =
              normalized.contains('mpesa') || normalized.contains('confirmed');
          return mpesaSender || mpesaBody;
        })
        .map((message) => message.body!.trim())
        .toList();
  }

  static bool _defaultIsAndroid() =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  static Future<bool> _defaultPermission() async {
    final status = await Permission.sms.request();
    return status.isGranted;
  }

  static Future<List<SmsMessage>> _defaultQueryRunner(SmsQuery query) {
    return query.querySms(
      kinds: const [SmsQueryKind.inbox],
      count: 1000,
      sort: true,
    );
  }
}
