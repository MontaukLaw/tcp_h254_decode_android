package com.wulala.myapplicationudprcv;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DisplayThread implements Runnable {

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

    private void initCodec() {
        //skipped the uninteresting parts.
        mediaCodec.setCallback(new MediaCodec.Callback() {

            @Override
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                // Log.d(TAG, "onInputBufferAvailable: " + index);
                freeInputBuffers.add(index);
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                Log.d(TAG, "onOutputBufferAvailable: " + index);
                mediaCodec.releaseOutputBuffer(index, true);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                // codec.configure(format);
            }

            // Dont care about the rest of the Callbacks for this demo...
        });
    }

    @Override
    public void run() {

        while (!Thread.interrupted()) {

            // Log.d(TAG, "run: ");

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            byte[] frameData = null;
            int inputIndex = -1;

            try {

                int queueNumber = h264FrameProducer.getQueueNumber();

                if (queueNumber > 0) {
                    Log.d(TAG, "queueNumber: " + queueNumber);
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
                Log.d(TAG, "inputIndex: " + inputIndex);
                ByteBuffer inputData = mediaCodec.getInputBuffer(inputIndex);
                inputData.clear();
                inputData.put(frameData);
                Log.d(TAG, "frameData.length: " + frameData.length);
                mediaCodec.queueInputBuffer(inputIndex, 0, frameData.length, 0, 0);
            }
        }

        mediaCodec.stop();
        mediaCodec.release();
    }
}
