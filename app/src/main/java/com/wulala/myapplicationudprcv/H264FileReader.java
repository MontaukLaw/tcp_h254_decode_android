package com.wulala.myapplicationudprcv;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class H264FileReader {

    private final static String TAG = H264FileReader.class.getSimpleName();
    @SuppressLint("SdCardPath")
    private static String fileName = "/data/data/com.wulala.myapplicationudprcv/1665284407294.h264";
    // InputStream is = getResources().openRawResource(R.raw.bbb_s4_1920x1080_wide_mp4_h264_hp4_20mbps_30fps_aac_lc_6ch_384kbps_44100hz);

    File h264File = new File(fileName);

    SocketRev socketRev;

    public H264FileReader(SocketRev mSocketRev) {
        this.socketRev = mSocketRev;
    }

    public void readH264File() {
        byte[] data = new byte[1024];
        int len = 0;
        try {
            FileInputStream fileInputStream = new FileInputStream(h264File);
            while (len != -1) {
                len = fileInputStream.read(data);
                Log.d(TAG, "readH264File: " + len);

                socketRev.checkData(data, len);
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
