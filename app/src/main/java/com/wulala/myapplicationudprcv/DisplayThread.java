package com.wulala.myapplicationudprcv;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DisplayThread implements Runnable {
    long lastOutTime = 0;
    long lastInTime = 0;
    long totalIn = 0;
    long totalOut = 0;

    MediaCodec mediaCodec;

    private BlockingQueue<Integer> freeInputBuffers;
    //skipped the uninteresting parts.
    public H264FrameProducer h264FrameProducer;

    private final static String TAG = DisplayThread.class.getSimpleName();

    public DisplayThread(MediaCodec codec, H264FrameProducer h264FrameProducer) {
        this.mediaCodec = codec;
        freeInputBuffers = new LinkedBlockingDeque<>();
        this.h264FrameProducer = h264FrameProducer;
        initCodec();
    }

    long timeToRender = 0;

    long outputFrameTickMS = 0;

    private void initCodec() {
        //skipped the uninteresting parts.
        mediaCodec.setCallback(new MediaCodec.Callback() {

            @Override
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                // Log.d(TAG, "onInputBufferAvailable: " + index);
                freeInputBuffers.add(index);
            }

            boolean ifStopLitterBit = false;
            long lastFrameRendTsUS = 0;

            long DECODE_AVG_TIME_MS = 145;

            long DROP_FRAME_TIME_MS = 200;

            // 最迟在50ms后播放
            long MAX_PLAY_TIME_MS = 100;

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                if (outputFrameTickMS == 0) {
//                    outputFrameTickMS = System.currentTimeMillis() + 33;
//                } else {
//                    outputFrameTickMS = outputFrameTickMS + 33;
//                }
//                while (System.currentTimeMillis() < outputFrameTickMS) {
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    mediaCodec.releaseOutputBuffer(index, true);
//                }

                // Log.d(TAG, "onOutputBufferAvailable: " + index);
                // Log.d(TAG, "output: " + (System.currentTimeMillis() - lastOutTime));

//                // 这里是解码时间, 即获取的是编码的时间戳, 减去现在的时间, 就是解码时间
                long averageDecodingTimeMS = (System.nanoTime() / 1000 - info.presentationTimeUs) / 1000;
                // Log.d(TAG, "presentationTimeUs OUT: " + info.presentationTimeUs);
                Log.d(TAG, "decode used: " + averageDecodingTimeMS + "ms");
//
//                // mediaCodec.releaseOutputBuffer(index, outputFrameTickNS + 900 * 1000 * 1000);
//                if (averageDecodingTimeMS > DROP_FRAME_TIME_MS) {
//                    mediaCodec.releaseOutputBuffer(index, true);
//                    Log.d(TAG, "drop frame: ");
//                } else {
//                    long sleepMS = DROP_FRAME_TIME_MS - averageDecodingTimeMS;
//                    Log.d(TAG, "sleepMS " + sleepMS);
//                    try {
//                        Thread.sleep(sleepMS);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    mediaCodec.releaseOutputBuffer(index, true);
//                }
                if (averageDecodingTimeMS < DECODE_AVG_TIME_MS) {
                    try {
                        Thread.sleep(DECODE_AVG_TIME_MS - averageDecodingTimeMS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // mediaCodec.releaseOutputBuffer(index, true);
                }

                mediaCodec.releaseOutputBuffer(index, true);

//                long timeGap = System.currentTimeMillis() - lastOutTime;
//
//                if (timeGap > 50) {
//                    Log.d(TAG, "out >>>>>>" + timeGap);
//                } else {
//                    Log.d(TAG, "out: " + timeGap);
//                }
                // Log.d(TAG, " total out: " + totalOut);

                // 尝试加入delay
                // Log.d(TAG, "output: " + (System.currentTimeMillis() - lastOutTime) + " total out: " + totalOut);

                // 100ms buf, = 100000000
                // mediaCodec.releaseOutputBuffer(index, System.nanoTime() + 100000000 - timeGap * 1000000);

//                lastOutTime = System.currentTimeMillis();
//                totalOut++;
//
//                lastFrameRendTsUS = System.nanoTime() / 1000;

            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
//                Log.d(TAG, "format: " + format);
//                Log.d(TAG, "codec: " + codec);
                // codec.configure(format, null, null, 0);
                // mediaCodec.configure(format);
            }

            // Dont care about the rest of the Callbacks for this demo...
        });
    }

    long frameCountTimsStamp = 0;
    long frameCounter = 0;
    long frameInCounter = 0;

    @Override
    public void run() {
        int queueNumber = 0;
        while (!Thread.interrupted()) {

            // Log.d(TAG, "run: ");
            if (System.currentTimeMillis() - frameCountTimsStamp > 1000) {
                frameCountTimsStamp = System.currentTimeMillis();
                // Log.d(TAG, " out per seconds OUT: " + (totalOut - frameCounter) + " IN:" + (totalIn - frameInCounter));
                frameCounter = totalOut;
                frameInCounter = totalIn;

                // Log.d(TAG, "nanotime:" + System.nanoTime());
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            byte[] frameData = null;
            int inputIndex = -1;

            try {

                queueNumber = h264FrameProducer.getQueueNumber();

                if (queueNumber > 0) {
//                    Log.d(TAG, "take frame between: " + (System.currentTimeMillis() - lastTime));
//                    lastTime = System.currentTimeMillis();
                    // Log.d(TAG, "queueNumber: " + queueNumber);
                    // Log.d(TAG, "inputIndex: before: " + inputIndex);
                    inputIndex = freeInputBuffers.take();
                    // Log.d(TAG, "inputIndex: after: " + inputIndex);
                    frameData = h264FrameProducer.takeFrameFromQueue();
                    if (frameData == null) {
                        continue;
                    }
                }

            } catch (InterruptedException e) {
                break;
            }

            if (inputIndex != -1) {
                // Log.d(TAG, "inputIndex: " + inputIndex);
                // Log.d(TAG, "input: " + (System.currentTimeMillis() - lastInTime) + " data len: " + frameData.length + " total in:" + totalIn);
                long timeGap = System.currentTimeMillis() - lastInTime;

                // Log.d(TAG, "input: " + timeGap);

                lastInTime = System.currentTimeMillis();
                totalIn++;
                if (totalIn < 10000) {
                    ByteBuffer inputData = mediaCodec.getInputBuffer(inputIndex);
                    inputData.clear();
                    inputData.put(frameData);
                    // Log.d(TAG, "frameData.length: " + frameData.length);
                    long ts = System.nanoTime() / 1000;
                    if (frameData.length < 30) {
                        // Log.d(TAG, "presentationTimeUs IN KEY: " + ts);
                    } else {
                        // Log.d(TAG, "presentationTimeUs IN: " + ts);
                    }

                    mediaCodec.queueInputBuffer(inputIndex, 0, frameData.length, ts, 0);
                    // mediaCodec.queueInputBuffer(inputIndex, 0, frameData.length, 0, 0);
                }
            }
        }

        mediaCodec.stop();
        mediaCodec.release();
    }
}
