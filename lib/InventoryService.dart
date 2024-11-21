import 'dart:async';
import 'package:flutter/services.dart';

class InventoryService {
  static const _channel = MethodChannel('my_channel'); // Đặt tên channel như trong native code

  // Tạo StreamController để truyền dữ liệu cho UI
  static final StreamController<List<Map<String, String>>> _inventoryStreamController =
  StreamController<List<Map<String, String>>>.broadcast();

  // Expose stream để có thể truy cập từ bên ngoài
  static Stream<List<Map<String, String>>> get inventoryStream => _inventoryStreamController.stream;

  // Khởi tạo MethodChannel và lắng nghe dữ liệu từ native
  static void initialize() {
    _channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case "inventorySingleTag":
          List<dynamic> dataList = call.arguments;
          handleInventorySingleTag(dataList);
          break;
        default:
          print("Unknown method ${call.method}");
      }
    });
  }

  // Hàm xử lý dữ liệu nhận được từ native và thêm vào Stream
  static void handleInventorySingleTag(List<dynamic> dataList) {
    // Chuyển đổi dataList thành List<Map<String, String>> và xử lý null
    List<Map<String, String>> formattedDataList = dataList.map((item) {
      Map<String, dynamic> dynamicMap = Map<String, dynamic>.from(item);
      Map<String, String> stringMap = {};
      dynamicMap.forEach((key, value) {
        stringMap[key] = value?.toString() ?? ""; // Thay thế null bằng chuỗi trống
      });
      return stringMap;
    }).toList();

    // Thêm dữ liệu đã xử lý vào stream
    _inventoryStreamController.add(formattedDataList);
  }
}
