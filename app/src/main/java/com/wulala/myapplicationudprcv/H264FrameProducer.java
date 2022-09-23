package com.wulala.myapplicationudprcv;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class H264FrameProducer {
    private final static String TAG = H264FrameProducer.class.getSimpleName();

    // 缓冲队列长度
    public static final int QUEUE_NUMBER = 200;

    private BlockingQueue<byte[]> h264FrameQueue = new LinkedBlockingQueue<>();

    public void addFrameToQueue(byte[] frameData) {

        // 加入缓存列表
        h264FrameQueue.add(frameData);

    }

    public byte[] takeFrameFromQueue() {
        byte[] frame = null;

        try {
            // Log.d(TAG, "h264FrameQueue size: " + h264FrameQueue.size());
            // frame = h264FrameQueue.take(); // take();
            frame = h264FrameQueue.poll(1, TimeUnit.MILLISECONDS);

            // frame = h264FrameQueue.poll(100, TimeUnit.MICROSECONDS); // take();
            // Log.d(TAG, "h264FrameQueue size" + h264FrameQueue.size());

        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        return frame;
    }

    public int getQueueNumber() {
        return h264FrameQueue.size();
    }

}
