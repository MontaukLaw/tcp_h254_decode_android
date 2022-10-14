package com.wulala.myapplicationudprcv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.wulala.myapplicationudprcv.databinding.ActivityMainBinding;
import com.wulala.myapplicationudprcv.nouse.SyncDecoder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    TextureView videoView;
    private MediaCodec mediaCodec;
    private SurfaceView mSurfaceView;
    // private SurfaceTexture surfaceTexture;
    // MyDecoder myDecoder;
    public H264FrameProducer h264FrameProducer;

    private boolean ifStarted = false;
    SyncDecoder syncDecoder;
    DisplayThread displayThread;
    private SocketRev socketRev;
    private Thread thread;
    H264FileReader h264FileReader;

    static {
        System.loadLibrary("myapplicationudprcv");
    }

    private ActivityMainBinding binding;
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        btn = binding.startRevBtn;
        btn.setOnClickListener(this);

        // mSurfaceView = binding.surfaceView;
        mSurfaceView = findViewById(R.id.surfaceView);
        Log.d(TAG, "mediacodec init: ");
        // initDecoder();

        Log.d(TAG, "mediacodec init finished: ");
        // videoView = binding.textureVideoView;
        // videoView.setSurfaceTextureListener(this);

        // myDecoder = new MyDecoder(mediaCodec);
        //  myDecoder.setMediaDecodeCallback();
        // syncDecoder = new SyncDecoder(mediaCodec);

        /*
        h264FrameProducer = new H264FrameProducer();

        displayThread = new DisplayThread(mediaCodec, h264FrameProducer);

        thread = new Thread(displayThread);

        socketRev = new SocketRev(mediaCodec, h264FrameProducer, thread);

        h264FileReader = new H264FileReader(socketRev);

         */

    }

    private final static String MIME_TYPE = "video/avc";
    private final static int VIDEO_WIDTH = 1920;
    private final static int VIDEO_HEIGHT = 1080;

    private final static int TIME_INTERNAL = 40;
    private final static int HEAD_OFFSET = 4;
    private final static int readBufferLen = 200000;
    private final static int workBufferLen = readBufferLen * 2;

    public void initDecoder() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);

        h264FrameProducer = new H264FrameProducer();

        displayThread = new DisplayThread(mediaCodec, h264FrameProducer);

        thread = new Thread(displayThread);

        socketRev = new SocketRev(mediaCodec, h264FrameProducer, thread);

        h264FileReader = new H264FileReader(socketRev);

        thread.start();

        mediaCodec.start();
    }

    public void decodeStart() {

        Log.d(TAG, "decodeStart: ");
        // mediaCodec.start();
    }

    long lastTime = 0;

    FileWriter fileWriter = new FileWriter();

    public void getUdpPacket(byte[] data) {
        // Log.d(TAG, "getUdpPacket: " + data.length);
        socketRev.checkUDPData(data, data.length);
        // fileWriter.writeFile(data, data.length);
        // socketRev.mh264FrameProducer.addFrameToQueue(data);

        if (data.length > 20) {
            // Log.d(TAG, "frame between: " + (System.currentTimeMillis() - lastTime));
            lastTime = System.currentTimeMillis();
        }

        /*
        // 启动工作由c负责
        if (ifStarted) {
            socketRev.mh264FrameProducer.addFrameToQueue(data);
            // myDecoder.h264FrameProducer.addFrameToQueue(data);
            // } else if (data.length == 20 && data[4] == 0x67) {
        } else if (data.length == 20 && ifMediaDecoderConfigured) {
            socketRev.mh264FrameProducer.addFrameToQueue(data);

            // Log.d(TAG, "getUdpPacket: " + data.length + data[4]);
            Log.d(TAG, "start decode");
            mediaCodec.start();
            ifStarted = true;
            // myDecoder.renderTH.start();
            // new Thread(syncDecoder).start();
            // displayThread.run();
            thread.start();
        }
        */
    }

    /**
     * A native method that is implemented by the 'myapplicationudprcv' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    public native void threadTest();

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.start_rev_btn:
                initDecoder();
                // udp
                threadTest();
                // tcp
                // socketRev.init();
                // readH264File();
                // readFileThread = new Thread(readRunnable);
                // readFileThread.start();
                // h264FileReader.readH264File();
                break;
        }
    }

}