package com.example.chainway_r5;

import android.content.pm.PackageManager;
import android.provider.Settings;
import android.content.Intent;
import android.os.Bundle;
import android.bluetooth.BluetoothDevice;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;
import android.os.CountDownTimer;
import android.util.Log;
import android.text.TextUtils;
import android.os.SystemClock;
import android.os.Message;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;


import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.embedding.engine.FlutterEngine;

import com.example.chainway_r5.tool.CheckUtils;
import com.example.chainway_r5.tool.NumberTool;
import com.rscja.deviceapi.RFIDWithUHFBLE;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.ConnectionStatus;
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback;
import com.rscja.deviceapi.interfaces.KeyEventCallback;
import com.rscja.deviceapi.interfaces.ScanBTCallback;
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "my_channel";
    private RFIDWithUHFBLE uhfble;
    //<editor-fold desc="FLAG">
    final int FLAG_START = 0;
    final int FLAG_STOP = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_ACTION_LOCATION_SETTINGS = 3;
    final int FLAG_UHFINFO_LIST = 5;

    final int FLAG_SUCCESS = 10;


    final int FLAG_FAIL = 11;
    final int FLAG_UPDATE_TIME = 12;
    final int FLAG_UHFINFO = 13;

    final int FLAG_TIME_OVER = 14;
    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 100;
    private static final int PERMISSION_REQUEST_CODE = 101;

    // </editor-fold>
    private Toast toast;

    public void showToast(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private List<UHFTAGInfo> tempDatas = new ArrayList<UHFTAGInfo>();
    public boolean isSupportRssi = false;
    int count = 0;
    private boolean isExit = false;
    public boolean isScanning = false;
    private String rfidSearch = "";

    public BluetoothAdapter mBtAdapter = null;

    private List<MyDevice> deviceList = new ArrayList<>();
    public static boolean isKeyDownUP = false;
    ScanBTCallback callback = new ScanBTCallback() {
        @Override
        public void getDevices(BluetoothDevice bluetoothDevice, final int rssi, byte[] bytes) {
            try {

                if (bluetoothDevice.getAddress() == null || bluetoothDevice.getName() == null) {
                    return; // Skip if address or name is null
                }

                MyDevice myDevice = new MyDevice(bluetoothDevice.getAddress(), bluetoothDevice.getName());
                boolean deviceFound = false;
                for (MyDevice listDev : deviceList) {
                    if (listDev.getAddress().equals(myDevice.getAddress())) {
                        deviceFound = true;
                        break;
                    }
                }
                if (!deviceFound) {
                    android.util.Log.i("MINHCHAULOG", "Device found: " + myDevice.getName());
                    deviceList.add(myDevice);

                }
            } catch (Exception e) {
                android.util.Log.i("MINHCHAULOG", "Error scan bluetooth " + e);
            }
        }
    };
    private MethodChannel channel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestBluetoothPermissions();
        uhfble = RFIDWithUHFBLE.getInstance(); // Lấy instance của RFIDWithUHFBLE

        isExit = false;

        uhfble.setKeyEventCallback(new KeyEventCallback() {
            @Override
            public void onKeyDown(int keycode) {

                if (!isExit && uhfble.getConnectStatus() == ConnectionStatus.CONNECTED) {
                    Log.i("MINHCHAULOG", "Key Down: " + keycode);
                    if (keycode == 3) {
                        isKeyDownUP = true;
                        startThread();
                    } else {
                        if (!isKeyDownUP) {
                            if (keycode == 1) {
                                if (isScanning) {
                                    stop();
                                } else {
                                    startThread();
                                }
                            }
                        }
                    }
                    if (keycode == 2) {
                        if (isScanning) {
                            stop();
                            SystemClock.sleep(100);
                        }
                        //MR20
                        inventory();
                    }
                }
            }

            @Override
            public void onKeyUp(int keycode) {
                if (keycode == 4) {
                    stop();
                }
            }
        });

//        // Thiết lập MethodChannel để lắng nghe các phương thức gọi từ Flutter
//          new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL).setMethodCallHandler(
//                (call, result) -> {
//
//                }
 //       );

//        // Đặt callback để nhận trạng thái kết nối
//        uhfble.setConnectionStatusCallback((status, device) -> {
//            if (status.equals(ConnectionStatus.DISCONNECTED)) {
//                System.out.println("Disconnected");
//            } else if (status.equals(ConnectionStatus.CONNECTED)) {
//                System.out.println("Connected");
//            }
//        });
//
//        // Đặt callback để nhận thông tin tag
//        uhfble.setInventoryCallback(uhftagInfo -> {
//            String epc = uhftagInfo.getEPC(); // Lấy EPC của tag
//            String rssi = uhftagInfo.getRssi(); // Lấy RSSI của tag
//            System.out.println("EPC: " + epc + ", RSSI: " + rssi); // In thông tin tag ra console
//        });
    }


    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        // Initialize the MethodChannel
        channel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);

        // Set up the MethodChannel handler
        channel.setMethodCallHandler((call, result) -> {
            switch (call.method) {
                case "init":
                    boolean initResult = uhfble.init(getApplicationContext()); // Khởi tạo RFID
                    result.success(initResult); // Trả về kết quả khởi tạo
                    android.util.Log.d("MINHCHAULOG", "Complete init");
                    break;

                case "connect":
                    if (count < 1) {
                        searchBluetoothDevices();
                    }
                    result.success(null); // Trả về kết quả thành công
                    break;
                case "inventorySingleTag":
                    inventorySingleTag(); // Call your function if needed
                    ArrayList<HashMap<String, String>> dataList = getTagList();
                    Log.d("MINHCHAULOG", "List data feedback: " + dataList.size());
                    result.success(dataList); // Send the ArrayList of HashMaps to Flutter
                    // Flutter support get List or Map datatype (no need convert JSON)
                    break;

                case "disconnect":
                    uhfble.disconnect(); // Ngắt kết nối
                    result.success(null); // Trả về kết quả thành công
                    break;

                case "startInventoryTag":
                    uhfble.startInventoryTag(); // Bắt đầu thu thập thông tin tag
                    result.success(null); // Trả về kết quả thành công
                    break;

                case "stopInventory":
                    uhfble.stopInventory(); // Dừng thu thập thông tin tag
                    result.success(null); // Trả về kết quả thành công
                    break;

                default:
                    result.notImplemented(); // Nếu phương thức không được hỗ trợ
                    break;
            }
        });
    }

    // Function to trigger inventorySingleTag
    public void triggerInventorySingleTagEvent() {
        ArrayList<HashMap<String, String>> dataList = getTagList();
        Log.d("MINHCHAULOG", "List data feedback: " + dataList.size());
        sendDataToFlutter(dataList);
    }

    // Function to send data to Flutter
    private void sendDataToFlutter(ArrayList<HashMap<String, String>> dataList) {
        if (channel != null) {
            channel.invokeMethod("inventorySingleTag", dataList); // Send data to Flutter
        } else {
            Log.e("MINHCHAULOG", "Error: MethodChannel is not initialized");
        }
    }
//    private void sendTaglistToUi() {
//        ArrayList<HashMap<String, String>> dataList = getTagList();
//        Log.d("MINHCHAULOG", "List data feedback: " + dataList.size());
//        result.success(dataList); // Send the ArrayList of HashMaps to Flutter
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 trở lên
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0 trở lên
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean forceLocationEnable() {
        boolean result = true;
        if (!checkLocationPermission()) {
            android.util.Log.i("MINHCHAULOG", "Location permission denied");
            result = false;
        }
        if (!isLocationEnabled()) {
            android.util.Log.i("MINHCHAULOG", "Location permission granted");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_ACTION_LOCATION_SETTINGS);
        }
        return result;
    }

    private boolean searchBluetoothDevices() {
        try {
            if (!forceLocationEnable()) {
                android.util.Log.i("MINHCHAULOG", "Location is not enable !");
                return false;
            } else {
                android.util.Log.i("MINHCHAULOG", "Location is enable !");
            }
            deviceList.clear();
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBtAdapter == null) {
                android.util.Log.i("MINHCHAULOG", "Bluetooth adapter not found");
                return false;
            }

            if (!mBtAdapter.isEnabled()) {
                android.util.Log.i("MINHCHAULOG", "Please turn on bluetooth !");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT); // Khởi động một hộp thoại yêu cầu bật bluetooth
                return false;
            } else {
                android.util.Log.i("MINHCHAULOG", "Bluetooth adapter is enable !");
            }

            uhfble.startScanBTDevices(callback);

            // Use CountDownTimer to wait for the scanning process to complete
            new CountDownTimer(5000, 1000) { // 5 seconds duration, 1 second interval
                public void onTick(long millisUntilFinished) {
                    // You can add some logging here if needed
                }

                public void onFinish() {
                    ShowDeviceListLog(); // Show the device list after 5 seconds
                    if (deviceList.size() > 0) {
                        android.util.Log.i("MINHCHAULOG", "okoke!");
                        connect("D2:69:9B:4E:39:EC");

                    } else {
                        android.util.Log.i("MINHCHAULOG", "sao ki z !");
                    }
                }
            }.start();

            return false;

        } catch (Exception ex) {
            android.util.Log.i("MINHCHAULOG", "Error !" + ex.getMessage());
            return false;
        }
    }

    public void inventory() { //Read each tag
        UHFTAGInfo info = uhfble.inventorySingleTag();
        if (info != null) {
            Message msg = handler.obtainMessage(FLAG_UHFINFO);
            msg.obj = info;
            handler.sendMessage(msg);
        }
    }

    private boolean checkLocationPermission() {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION_REQUEST);
                result = false;
            }
        }
        return result;
    }

    private synchronized List<UHFTAGInfo> getUHFInfo() {
        List<UHFTAGInfo> list = null;
        if (!isSupportRssi) {
            list = uhfble.readTagFromBufferList_EpcTidUser();

        } else {
            list = uhfble.readTagFromBufferList();
        }
        if (list != null) {
            for (UHFTAGInfo info : list) {
                android.util.Log.d("MINHCHAULOG", "EPC: " + info.getEPC());
            }
        }
        return list;
    }

    private boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return false;
        }
    }


    private void ShowDeviceListLog() {
        try {
            android.util.Log.i("MINHCHAULOG", "Device list size: " + deviceList.size());
        } catch (Exception e) {
            android.util.Log.i("MINHCHAULOG", "Error show device list " + e);
        }
    }

    BTStatus btStatus = new BTStatus();

    public void connect(String deviceAddress) {
        if (uhfble.getConnectStatus() == ConnectionStatus.CONNECTING) {
            //showToast("Connecting, please wait");
            android.util.Log.d("MINHCHAULOG", "Connecting to: " + deviceAddress);
            // disconnect(true);
            // uhfble.connect(deviceAddress, btStatus);
        } else {
            android.util.Log.d("MINHCHAULOG", "Connect is processing to: " + deviceAddress);
            uhfble.connect(deviceAddress, btStatus);
        }
    }

    public void disconnect(boolean isActiveDisconnect) {
        //  cancelDisconnectTimer();
        //  mIsActiveDisconnect = isActiveDisconnect; // 主动断开为true
        uhfble.disconnect();
    }

    class BTStatus implements ConnectionStatusCallback<Object> {
        @Override
        public void getStatus(final ConnectionStatus connectionStatus, final Object device1) {
            runOnUiThread(new Runnable() {
                public void run() {
                    BluetoothDevice device = (BluetoothDevice) device1;

                    if (connectionStatus == ConnectionStatus.CONNECTED) {
//                        remoteBTName = device.getName();
//                        remoteBTAdd = device.getAddress();
                        showToast("Connected to " + device.getName());
                        android.util.Log.d("MINHCHAULOG", "Connected device: " + device.getName());

                    } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                        android.util.Log.d("MINHCHAULOG", "DisConnected device: " + device.getName());
                    }
                }
            });
        }
    }


    private void stopScanRFID() {
        isScanning = false;
    }

    boolean firstStart = false;

    public void startScanRFID() {

        isScanning = true;
        new TagThread().start();

    }

    /// <summary>
    /// Handler for Data Read
    /// </summary>
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FLAG_STOP:
                    if (msg.arg1 == FLAG_SUCCESS) {

                    } else {
                        Utils.playSound(2);
                        // Toast.makeText(this, "Stop Fail !", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), "Stop Fail!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case FLAG_UHFINFO_LIST:
                    List<UHFTAGInfo> list = (List<UHFTAGInfo>) msg.obj;
                    addEPCToList(list);
                    break;
                case FLAG_START:
                    if (msg.arg1 == FLAG_SUCCESS) {
                        //start read success

                    } else {
                        Utils.playSound(2);
                    }
                    break;
                case FLAG_UPDATE_TIME:
                    if (isScanning) {
                        float useTime = (System.currentTimeMillis() - mStrTime) / 1000.0F;
                        String useTimeCustom = NumberTool.getPointDouble(1, useTime) + "s";
                        handler.sendEmptyMessageDelayed(FLAG_UPDATE_TIME, 10);
                    } else {
                        handler.removeMessages(FLAG_UPDATE_TIME);
                    }
                    break;
                case FLAG_TIME_OVER:
                    Log.i("MINHCHAULOG", "FLAG_TIME_OVER =" + (System.currentTimeMillis() - mStrTime));
                    float useTime2 = (System.currentTimeMillis() - mStrTime) / 1000.0F;
                    String useTimeCustom2 = NumberTool.getPointDouble(1, useTime2) + "s";

                    break;
                case FLAG_UHFINFO:
                    UHFTAGInfo info = (UHFTAGInfo) msg.obj; // Get tag info from message
                    List<UHFTAGInfo> listTemp = new ArrayList<UHFTAGInfo>();
                    listTemp.add(info);
                    addEPCToList(listTemp);
                    triggerInventorySingleTagEvent();

                    break;
            }
        }
    };

    private void addEPCToList(List<UHFTAGInfo> list) {
        for (int k = 0; k < list.size(); k++) {
            boolean[] exists = new boolean[1];
            UHFTAGInfo info = list.get(k);
            int idx = CheckUtils.getInsertIndex(tempDatas, info, exists); // find the index to insert the tag
            insertTag(info, idx, exists[0]);
        }
    }

    private ArrayList<HashMap<String, String>> tagList = new ArrayList<>();
    ;
    public static final String TAG_COUNT = "tagCount";
    public static final String TAG_DATA = "tagData";
    public static final String TAG_EPC = "tagEpc";
    public static final String TAG_TID = "tagTid";
    public static final String TAG_USER = "tagUser";
    public static final String TAG_RSSI = "tagRssi";

    private void insertTag(UHFTAGInfo info, int index, boolean exists) {
        try {
            String data = info.getEPC();
            // get TID (user config)
            if (!TextUtils.isEmpty(info.getTid())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EPC:");
                stringBuilder.append(info.getEPC());
                stringBuilder.append("\n");
                stringBuilder.append("TID:");
                stringBuilder.append(info.getTid());
                if (!TextUtils.isEmpty(info.getUser())) {
                    stringBuilder.append("\n");
                    stringBuilder.append("USER:");
                    stringBuilder.append(info.getUser());
                }
                data = stringBuilder.toString(); //Create template  EPC:...TID:...USER:...
                Log.d("MINHCHAULOG", "Data with TID: " + data);
            }
            HashMap<String, String> tagMap = null; // Use HashMap to store tag data and fast access
            if (exists) {
                tagMap = tagList.get(index); // Get tag data from tagList

                //The decimal system (base 10) has a radix of 10 and uses the digits 0 through 9.
                tagMap.put(TAG_COUNT, String.valueOf(Integer.parseInt(tagMap.get(TAG_COUNT), 10) + 1)); // Increase the count of the tag
            } else {
                tagMap = new HashMap<>();
                tagMap.put(TAG_EPC, info.getEPC());
                tagMap.put(TAG_COUNT, String.valueOf(1));
                tempDatas.add(index, info);  // Add tag data to tempDatas
                tagList.add(index, tagMap); // Add tag data to tagList
            }
            // Add another data to tagMap
            tagMap.put(TAG_USER, info.getUser());
            tagMap.put(TAG_DATA, data);
            tagMap.put(TAG_TID, info.getTid());
            tagMap.put(TAG_RSSI, info.getRssi() == null ? "" : info.getRssi());

            for (HashMap<String, String> tag : tagList) {
                Log.d("MINHCHAULOG", "Tag List : " + tag);
            }

            showTempDatasInfo();
        } catch (Exception e) {
            android.util.Log.i("MINHCHAULOG", "Error insert tag " + e);
        }
    }

    private void showTempDatasInfo() {
        try {
            // android.util.Log.d("MINHCHAULOG", "showTempDatasInfo: " + tempDatas.size());
            if (tempDatas != null) {
                for (UHFTAGInfo info : tempDatas) {

                    if (info.getEPC() != null) {
                        Log.d("MINHCHAULOG", "EPC: " + info.getEPC());
                    }
                    if (info.getTid() != null) {
                        Log.d("MINHCHAULOG", "TID: " + info.getTid());
                    }
                    if (info.getUser() != null) {
                        Log.d("MINHCHAULOG", "USER: " + info.getUser());
                    }
                    if (info.getRssi() != null) {
                        Log.d("MINHCHAULOG", "RSSI: " + info.getRssi());
                    }
                }
            } else {
                Log.d("TEMP_DATAS_INFO", "TempDatas is null");
            }
        } catch (Exception e) {
            android.util.Log.i("MINHCHAULOG", "Error show temp data info " + e);
        }

    }

    private ArrayList<HashMap<String, String>> getTagList() {
        return tagList;
    }

    private List<Map<String, String>> getTempDatas() {
        // Convert tempDatas to a List of Maps
        List<Map<String, String>> data = new ArrayList<>();
        for (UHFTAGInfo info : tempDatas) {
            Map<String, String> map = new HashMap<>();

            // Kiểm tra null và gán giá trị mặc định nếu cần
            map.put("EPC", info.getEPC() != null ? info.getEPC() : "");
            map.put("TID", info.getTid() != null ? info.getTid() : "");
            map.put("USER", info.getUser() != null ? info.getUser() : "");
            map.put("RSSI", info.getRssi() != null ? info.getRssi() : "");

            data.add(map);
        }
        return data;
    }


    class MyDevice {
        private String address;
        private String name;
        private int bondState;

        public MyDevice() {

        }

        public MyDevice(String address, String name) {
            this.address = address;
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBondState() {
            return bondState;
        }

        public void setBondState(int bondState) {
            this.bondState = bondState;
        }
    }

    class TagThread extends Thread {
        public void run() {
            try {
                Log.d("MINHCHAULOG", "Init Thread");
                Message msg = handler.obtainMessage(FLAG_START);
                if (uhfble.startInventoryTag()) {
                    msg.arg1 = FLAG_SUCCESS;
                } else {
                    msg.arg1 = FLAG_FAIL;
                    isScanning = false;
                }
                handler.sendMessage(msg);
                while (isScanning) {
                    List<UHFTAGInfo> list = getUHFInfo();
                    if (list == null || list.size() == 0) {
                        SystemClock.sleep(1);
                        Log.d("MINHCHAULOG", "Null: ");
                    } else {
                        Log.d("MINHCHAULOG", "getUHFInfo: " + list.size());
                        if (rfidSearch != null && rfidSearch.length() > 0) {
                            for (int i = 0; i < list.size(); i++) {
                                if (rfidSearch.equals(list.get(i).getEPC())) {
                                    //  Utils.playSound(1);
                                    //    handler.sendMessage(handler.obtainMessage(FLAG_UHFINFO_LIST, list));
                                }
                            }
                        } else {
                            //  Utils.playSound(1);
                            //    handler.sendMessage(handler.obtainMessage(FLAG_UHFINFO_LIST, list));

                        }
                        //   inventory();
                    }
                }
            } catch (Exception e) {
                Log.d("MINHCHAULOG", "Error: " + e);
            }

        }


    }

    private void inventorySingleTag() {
        UHFTAGInfo info = uhfble.inventorySingleTag(); //Identify tag in single mode
        if (info != null) {
            Message msg = handler.obtainMessage(FLAG_UHFINFO);
            msg.obj = info;
            handler.sendMessage(msg); // Send message to handler
        }
    }

    int maxRunTime = 99999999;
    String time = "1"; // time to read tag loop
    private long mStrTime;

    /// Inventory Loop
    public void startThread() {

        //If scanning is already started, return
        if (isScanning) {
            return;
        }

        // if time is not null and not empty and starts with a dot, set time to empty
        if (time != null && !time.isEmpty() && time.startsWith(".")) {
            time = "";
        } else {
            maxRunTime = Integer.parseInt(time) * 1000; // seconds
        }

        // Register callback to handle messages
        uhfble.setInventoryCallback(new IUHFInventoryCallback() {
            @Override
            public void callback(UHFTAGInfo uhftagInfo) {
                handler.sendMessage(handler.obtainMessage(FLAG_UHFINFO, uhftagInfo));
            }
        });
        isScanning = true;
        Message msg = handler.obtainMessage(FLAG_START);
        Log.i("MINHCHAUTAG", "startInventoryTag() 1");

        if (uhfble.startInventoryTag()) {
            mStrTime = System.currentTimeMillis();
            msg.arg1 = FLAG_SUCCESS;
            handler.sendEmptyMessage(FLAG_UPDATE_TIME);
            handler.removeMessages(FLAG_TIME_OVER);
            handler.sendEmptyMessageDelayed(FLAG_TIME_OVER, maxRunTime);
        } else {
            msg.arg1 = FLAG_FAIL;
            isScanning = false;
        }
        handler.sendMessage(msg);
    }

    private void stop() {

        //  Log.i(TAG, "stop mContext.isScanning=false");
        handler.removeMessages(FLAG_TIME_OVER);
        if (isScanning) {
            stopInventory();
        }
        isScanning = false;
        cancelInventoryTask();
    }

    private TimerTask mInventoryPerMinuteTask;

    private void cancelInventoryTask() {
        if (mInventoryPerMinuteTask != null) {
            mInventoryPerMinuteTask.cancel();
            mInventoryPerMinuteTask = null;
        }
    }

    private void stopInventory() {
        //Log.i(TAG, "stopInventory() 2");

        ConnectionStatus connectionStatus = uhfble.getConnectStatus();
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            return;
        }
        boolean result = false;
        result = uhfble.stopInventory();

        Message msg = handler.obtainMessage(FLAG_STOP);

        if (!result && connectionStatus == ConnectionStatus.CONNECTED) {
            msg.arg1 = FLAG_FAIL;
        } else {
            msg.arg1 = FLAG_SUCCESS;
        }
        handler.sendMessage(msg);
    }
}
