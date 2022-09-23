package com.wulala.myapplicationudprcv.nouse;

import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.wulala.myapplicationudprcv.H264FrameProducer;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SyncDecoder implements Runnable {

    MediaCodec mediaCodec;
    private final static long OVER_TIME_US = 100;
    public H264FrameProducer h264FrameProducer;

    private BlockingQueue<Integer> freeInputBuffers;
    //skipped the uninteresting parts.

    private void initCodec() {
        //skipped the uninteresting parts.
        mediaCodec.setCallback(new MediaCodec.Callback() {

            @Override
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                freeInputBuffers.add(index);
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {


            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }

        });
    }


    public SyncDecoder(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;
        h264FrameProducer = new H264FrameProducer();
        // setOutputCallback();
        freeInputBuffers = new LinkedBlockingDeque();
        initCodec();
    }

    private void setOutputCallback() {
        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                int outputId = mediaCodec.dequeueOutputBuffer(info, OVER_TIME_US);

                if (outputId >= 0) {
                    //释放buffer，并渲染到 Surface 中
                    mediaCodec.releaseOutputBuffer(outputId, true);
                }
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });
    }

    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    //解码输出缓冲区
    protected ByteBuffer[] mOutputBuffers = null;

    private int pullBufferFromDecoder() {
        // 查询是否有解码完成的数据，index >=0 时，表示数据有效，并且index为缓冲区索引
        int index = mediaCodec.dequeueOutputBuffer(mBufferInfo, 1000);
        switch (index) {
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mOutputBuffers = mediaCodec.getOutputBuffers();
                break;
            default:
                return index;
        }
        return -1;
    }


    @Override
    public void run() {

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        // 解码
        while (!Thread.interrupted()) {
            int inputIndex;
            // 等待最多OVER_TIME_US获取缓冲队列id
            int inputBufferId = mediaCodec.dequeueInputBuffer(OVER_TIME_US);

            byte[] input = null;
            if (inputBufferId > 0) {
                // 通过缓冲队列id, 获取buffer
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);

                if (inputBuffer != null) {

                    // 读取udp数据队列
                    try {
                        input = h264FrameProducer.takeFrameFromQueue();
                        inputIndex = freeInputBuffers.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (input != null && input.length > 0) {
                        inputBuffer.clear();
                        inputBuffer.put(input);
                        mediaCodec.queueInputBuffer(
                                inputBufferId,
                                0,
                                input.length,
                                System.currentTimeMillis(),
                                0
                        );
                    }

                    int index = pullBufferFromDecoder();

                    if (index > 0) {
                        //【解码步骤:5.释放输出缓冲】
                        mediaCodec.releaseOutputBuffer(index, true);
                    }

                }

            }
        }

        mediaCodec.stop();
        mediaCodec.release();

    }
}
