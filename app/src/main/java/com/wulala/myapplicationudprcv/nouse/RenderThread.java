package com.wulala.myapplicationudprcv.nouse;

import android.media.MediaCodec;
import android.util.Log;

import com.wulala.myapplicationudprcv.OutputInfo;

import java.util.concurrent.CopyOnWriteArrayList;

public class RenderThread implements Runnable {

    private final static String TAG = RenderThread.class.getSimpleName();

    private final static int OUTPUT_BUF_SIZE = 200;
    private MediaCodec mediaCodec;

    public RenderThread(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;
    }

    public CopyOnWriteArrayList<OutputInfo> outputList = new CopyOnWriteArrayList();

    private int findOldestBuf() {
        int index = 0;
        long oldestTs;
        if (outputList.size() > 2) {
            oldestTs = outputList.get(0).getTs();
            for (int i = 1; i < outputList.size(); i++) {
                long temp = outputList.get(i).getTs();
                if (temp < oldestTs) {
                    Log.d(TAG, "find strange one: ");
                    oldestTs = temp;
                    index = i;
                }
            }

            return index;

        } else {
            return 0;
        }
    }

    private void removeOldest() {
        if (outputList.size() > 0) {
            outputList.remove(findOldestBuf());
        }
    }

    private long lastPlayMS = 0;

    @Override
    public void run() {
        int sleepTimes;

        // test();
        while (true) {
            sleepTimes = 4;
            // Log.d(TAG, "linkedHashMap: " + linkedHashMap.toString());
            // Log.d(TAG, "linkedHashMap: " + getHead(linkedHashBufferMap).toString());
            try {
                // test();
                if (outputList.size() > 2) {

//                    Log.d(TAG, "get buffer size " + outputList.size());
//                    int oldestIdxOfList = findOldestBuf();
//
//                    if (oldestIdxOfList > 0) {
//                        Log.d(TAG, "oldestIdxOfList: " + oldestIdxOfList);
//                    }

                    // Log.d(TAG, "playtime ts: " + outputList.get(0).getTs());
                    // input gap是从input到现在的总时间
                    Log.d(TAG, "input output gap is " + (System.currentTimeMillis() - outputList.get(0).getTs()));

                    // 这个outputgap是在这渲染的队列中排队的时间
                    Log.d(TAG, "output gap is " + (System.currentTimeMillis() - outputList.get(0).getOutputTs()));

                    Log.d(TAG, "last play gap is " + (System.currentTimeMillis() - lastPlayMS));

                    if ((System.currentTimeMillis() - outputList.get(0).getOutputTs()) > 100) {
                        Log.d(TAG, "refresh twice");
                        sleepTimes = 3;
                    }
                    if ((System.currentTimeMillis() - outputList.get(0).getTs()) > 400) {
                        /// mediaCodec.releaseOutputBuffer(outputList.get(0).getIndex(), true);
                        // outputList.remove(0);
                        Log.d(TAG, "refresh twice");
                        sleepTimes = 3;
                    }

                    mediaCodec.releaseOutputBuffer(outputList.get(0).getIndex(), true);
                    outputList.remove(0);

                    lastPlayMS = System.currentTimeMillis();
                }
                Thread.sleep(10 * sleepTimes);
                // Log.d(TAG, "playtime: " + System.currentTimeMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //  mediaCodec.releaseOutputBuffer(idx, true);
        }
    }

    private void test() {

//        outputList.add(new OutputInfo(111L, 1));
//        outputList.add(new OutputInfo(122L, 2));
//        outputList.add(new OutputInfo(13L, 3));
//        outputList.add(new OutputInfo(41L, 4));
//        outputList.add(new OutputInfo(11L, 5));
//        outputList.add(new OutputInfo(13L, 6));
//        Log.d(TAG, "-----------------------");
//        Log.d(TAG, "get buffer size " + outputList);
//        Log.d(TAG, "findOldestBuf " + findOldestBuf());
//        outputList.remove(findOldestBuf());
//        Log.d(TAG, "get buffer size " + outputList);
//        Log.d(TAG, "-----------------------");
//        Log.d(TAG, "get buffer size " + outputList);
//        Log.d(TAG, "findOldestBuf " + findOldestBuf());
//        outputList.remove(findOldestBuf());
//        Log.d(TAG, "get buffer size " + outputList);
//        Log.d(TAG, "-----------------------");
//
//        Log.d(TAG, "get buffer size " + outputList);
//        Log.d(TAG, "findOldestBuf " + findOldestBuf());
//        outputList.remove(findOldestBuf());
//        Log.d(TAG, "get buffer size " + outputList);
//
//        // removeOldest();
//        // printOutBufferArrAllItem();
//
//        Log.d(TAG, outputList.toString());
    }

}
