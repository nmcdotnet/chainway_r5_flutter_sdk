import 'package:flutter/services.dart';

class uhfManager {
  // Khai báo MethodChannel với tên kênh
  static const MethodChannel _channel = MethodChannel('my_channel');

  // Phương thức khởi tạo RFID
  Future<bool> init() async {
    final bool result = await _channel.invokeMethod('init');
    return result;
  }

  // Phương thức kết nối đến thiết bị RFID
  Future<bool> connect(String mac) async {
    final bool result = await _channel.invokeMethod('connect', {'mac': mac});
    return result;
  }



  /// inventory Read singleTag (Get tagList from native code)
  Future<List<Map<String, String>>> singleRead() async {
    try {
      final List<dynamic> data =
          await _channel.invokeMethod('inventorySingleTag');

      // Convert each item to Map<String, String> with null checks
      List<Map<String, String>> listMap = data.map((item) {
        // Ensure item is treated as a Map with String keys and dynamic values
        Map<String, dynamic> dynamicMap = Map<String, dynamic>.from(item);

        // Create a new map with non-null String values or empty strings
        Map<String, String> stringMap = {};
        dynamicMap.forEach((key, value) {
          stringMap[key] =
              value?.toString() ?? ""; // Replace null with empty string
        });
        return stringMap;
      }).toList();
      return listMap;
    } on PlatformException catch (e) {
      print("Error fetching data: ${e.message}");
      return [];
    }
  }

  Future<List<Map<String, String>>> singleRead_Bak1() async {
    try {
      //Call native method and get response `List<dynamic>`
      final List<dynamic> data =
          await _channel.invokeMethod('inventorySingleTag');

      //Check null
      if (data == null || data.isEmpty) {
        return [];
      }

      // Cast `List<dynamic>` to `List<Map<String, String>>`
      final List<Map<String, String>> tagList = data.map((e) {
        // Check every element is Map<String, String> ?
        if (e is Map) {
          return Map<String, String>.from(e.map(
              (key, value) => MapEntry(key as String, value as String? ?? "")));
        } else {
          return <String,
              String>{}; // Returns empty Map if not in correct format
        }
      }).toList();

      return tagList;
    } catch (e) {
      print("Error fetching tags: $e");
      return [];
    }
  }

  // Phương thức ngắt kết nối
  Future<void> disconnect() async {
    await _channel.invokeMethod('disconnect');
  }

  // Phương thức bắt đầu quét thẻ
  Future<void> startInventoryTag() async {
    await _channel.invokeMethod('startInventoryTag');
  }

  // Phương thức dừng quét thẻ
  Future<void> stopInventory() async {
    await _channel.invokeMethod('stopInventory');
  }
}
