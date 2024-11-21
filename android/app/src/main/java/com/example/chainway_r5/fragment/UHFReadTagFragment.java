//package com.example.chainway_r5.fragment;
//import android.os.Handler;
//import android.text.TextUtils;
//
//import com.example.chainway_r5.tool.CheckUtils;
//import com.example.tool;
//import com.rscja.deviceapi.entity;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//public class UHFReadTagFragment {
//
//    final int FLAG_TIME_OVER= 12;//
//    final int FLAG_STOP = 1;
//    final int FLAG_UPDATE_TIME = 2; // 更新时间
//    final int FLAG_UHFINFO = 3;
//    final int FLAG_UHFINFO_LIST = 5;
//    final int FLAG_START = 0;
//    Handler handler = new Handler(Looper.getMainLooper()){
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case FLAG_TIME_OVER:
//
//                    break;
//                case FLAG_STOP:
//
//                    break;
//                case FLAG_UHFINFO_LIST:
//
//                    break;
//                case FLAG_START:
//
//                    break;
//                case FLAG_UPDATE_TIME:
//
//                    break;
//                case FLAG_UHFINFO:
//                    UHFTAGInfo info = (UHFTAGInfo) msg.obj;
//                    addEPCToList(info);
//                    //Utils.playSound(1);
//                    break;
//            }
//        }
//    };
//
//    private List<UHFTAGInfo> tempDatas = new ArrayList<UHFTAGInfo>();
//    private void addEPCToList(UHFTAGInfo uhftagInfo) {
//        boolean[] exists=new boolean[1];
//        int idx= CheckUtils.getInsertIndex(tempDatas,uhftagInfo,exists);
//        insertTag(uhftagInfo,idx,exists[0]);
//    }
//
//    void insertTag(UHFTAGInfo info, int index,boolean exists){
//        if(!exists){
//            tempDatas.add(index,info);
//        }
//    }
//
//    private void inventory() { //Read each tag
//        UHFTAGInfo info = mContext.uhf.inventorySingleTag();
//        if (info != null) {
//            Message msg = handler.obtainMessage(FLAG_UHFINFO);
//            msg.obj = info;
//            handler.sendMessage(msg);
//        }
//    }
//
//}
