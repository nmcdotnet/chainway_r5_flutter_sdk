import 'dart:convert';
import 'package:flutter/services.dart';

class NativeChannelManager {
  // Tên của MethodChannel (phải trùng với tên trên native side)
  static const String _channelName = 'my_channel';
  static const MethodChannel _channel = MethodChannel(_channelName);

  // Phương thức lấy dữ liệu từ native code
  static Future<List<Map<String, dynamic>>> getDataFromNative(String methodName) async {
    try {
      final String jsonData = await _channel.invokeMethod(methodName);
      return List<Map<String, dynamic>>.from(json.decode(jsonData));
    } catch (e) {
      print("Error fetching data from native: $e");
      return [];
    }
  }

  // Phương thức gọi một lệnh không trả về dữ liệu từ native
  static Future<void> invokeNativeMethod(String methodName, [dynamic arguments]) async {
    try {
      await _channel.invokeMethod(methodName, arguments);
    } catch (e) {
      print("Error invoking native method: $e");
    }
  }
}