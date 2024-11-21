import 'package:chainway_r5/uhfManager.dart';
import 'package:flutter/material.dart';
import 'InventoryService.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized(); // Đảm bảo BinaryMessenger đã được khởi tạo
  InventoryService.initialize();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final uhfManager rfidManager = uhfManager();
  List<Map<String, String>> _tags = [];

  @override
  void initState() {
    super.initState();
    _initializeRFID(); // Khởi tạo RFID khi widget được tạo
    // Lắng nghe dữ liệu từ InventoryService
    InventoryService.inventoryStream.listen((tags) {
      setState(() {
        _tags = tags; // Cập nhật UI với danh sách thẻ mới
      });
    });
  }

  Future<void> _initializeRFID() async {
    bool initResult = await rfidManager.init();
    if (!initResult) {
      print("Initialization failed");
    } else {
      print("RFID initialized successfully");
    }
  }

  Future<void> _connectRFID(String mac) async {
    bool connectResult = await rfidManager.connect("D2:69:9B:4E:39:EC");
    if (!connectResult) {
      print("Connection failed");
    } else {
      print("Connected successfully");
    }
  }

  Future<void> _singleReadTag() async {
    try {
      List<Map<String, String>> tags = await rfidManager.singleRead();
      setState(() {
        _tags = tags;
      });

      for (var tag in tags) {
        print(tag);
      }
    } catch (e) {
      print("Error fetching tags: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            ElevatedButton(
              onPressed: () => _connectRFID('YOUR_BLE_MAC_ADDRESS'),
              child: const Text('Connect to RFID'),
            ),
            ElevatedButton(
              onPressed: _singleReadTag,
              child: const Text('Single Read'),
            ),
            Expanded(
              child: _tags.isEmpty
                  ? const Center(child: Text('No tags available'))
                  : ListView.builder(
                itemCount: _tags.length,
                itemBuilder: (context, index) {
                  final tag = _tags[index];
                  return ListTile(
                    title: Text("Tag Data: ${tag['tagData'] ?? ''}"),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("Tag EPC: ${tag['tagEpc'] ?? ''}"),
                        Text("Tag Count: ${tag['tagCount'] ?? ''}"),
                        Text("Tag User: ${tag['tagUser'] ?? ''}"),
                        Text("Tag RSSI: ${tag['tagRssi'] ?? ''}"),
                        Text("Tag TID: ${tag['tagTid'] ?? ''}"),
                      ],
                    ),
                    isThreeLine: true,
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
