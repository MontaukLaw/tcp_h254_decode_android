package com.wulala.myapplicationudprcv.nouse;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.wulala.myapplicationudprcv.H264FrameProducer;
import com.wulala.myapplicationudprcv.OutputInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MyDecoder {

    private static final String TAG = MyDecoder.class.getSimpleName();
    private MediaFormat mediaFormat;

    MediaCodec mediaCodec;
    public H264FrameProducer h264FrameProducer;

    protected long prevOutputPTSUs = 0;
    private CopyOnWriteArrayList<Long> timeStampList = new CopyOnWriteArrayList();

    public Thread renderTH;
    private RenderThread renderThread;

    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    public MyDecoder(MediaCodec _mediaCodec) {

        this.mediaCodec = _mediaCodec;
        h264FrameProducer = new H264FrameProducer();

        initDecoderFormat();
        renderThread = new RenderThread(mediaCodec);
        renderTH = new Thread(renderThread);
        // new Thread(new RenderThread()).start;
    }

    public void setMediaDecodeCallback() {
        boolean pResult = false;
        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int inputBufferId) {

                byte[] input = h264FrameProducer.takeFrameFromQueue();
                long nowMS = getPTSUs();

                if (input != null) {

                    // Log.d(TAG, "input len: " + input.length);
                    // Log.d(TAG, "nowMS: " + nowMS);
                    ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    codec.queueInputBuffer(inputBufferId, 0, input.length, System.currentTimeMillis(), 0);
                }
            }

            private long lastMS = 0;

            private long playTime = 0;

            private long startPos = 0;

            private List<Long> averageGapList = new ArrayList();

            private long getAverageGap(long gap) {

                long total = 0;
                averageGapList.add(gap);

                if (averageGapList.size() > 100) {
                    averageGapList.remove(0);
                }
                // Log.d(TAG, "getAverageGap: before " + getPTSUs());
                if (averageGapList.size() == 100) {
                    for (int i = 0; i < 100; i++) {
                        total = total + averageGapList.get(i);
                    }
                }
                // Log.d(TAG, "getAverageGap: after  " + getPTSUs());

                return total / 100;
            }

            private int init12Frame = 0;

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec,
                                                int index, @NonNull MediaCodec.BufferInfo info) {

                Log.d(TAG, "idx: " + index);

                if (init12Frame < 100) {
                    init12Frame++;
                }
                if (init12Frame >= 100) {
                    // renderThread.addBuffer(info.presentationTimeUs, index);

                    renderThread.outputList.add(new OutputInfo(info.presentationTimeUs, System.currentTimeMillis(), index));
                    // renderThread.outputList.add(new OutputInfo(System.currentTimeMillis(), index));

                } else {
                    codec.releaseOutputBuffer(index, false);
                }
                // codec.releaseOutputBuffer(index, true);
                // mediaCodec.releaseOutputBuffer(index, true);
                // codec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d(TAG, "onError: ");
                codec.reset();
            }

            @Override
            public void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanged: ");

                mediaFormat = format;
            }
        });
    }

    private void initDecoderFormat() {

        mediaFormat = MediaFormat.createVideoFormat("video/avc", 640, 360);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        // mediaFormat = new MediaFormat("video/avc");
        // mediaFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
        // mediaFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        // mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        // mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        // PIXEL_FORMAT_YUV_SEMIPLANAR_420
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface); // COLOR_FormatSurface);
        // mediaFormat.setInteger(MediaFormat.KEY_WIDTH, 640);   // 360 * 640
        // mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, 360);  //
        // mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        // mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);
        // mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 500 * 1000);
        // mediaFormat.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        // stVpssExtChnAttr.u32Height = 360;
        // stVpssExtChnAttr.u32Width = 640;

    }

    public void configFormat(SurfaceTexture surfaceTexture) {

        mediaCodec.configure(mediaFormat, new Surface(surfaceTexture), null, 0);
    }

}
