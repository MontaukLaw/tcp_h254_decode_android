package com.wulala.myapplicationudprcv;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.FileOutputStream;

public class FileWriter {
    @SuppressLint("SdCardPath")
    File h264File = new File("/data/data/com.wulala.myapplicationudprcv/", System.currentTimeMillis() + ".h264");
    FileOutputStream fileOutputStream = null;

    public void writeFile(byte[] data, int len) {
        try {
            if (fileOutputStream == null) {
                fileOutputStream = new FileOutputStream(h264File, true);
            }
            fileOutputStream.write(data, 0, len);
            fileOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
