package com.wulala.myapplicationudprcv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.wulala.myapplicationudprcv.databinding.ActivityMainBinding;
import com.wulala.myapplicationudprcv.nouse.SyncDecoder;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    TextureView videoView;
    private MediaCodec mediaCodec;

    private SurfaceTexture surfaceTexture;
    private MediaFormat mediaFormat;

    private boolean ifMediaDecoderConfigured = false;
    // MyDecoder myDecoder;
    public H264FrameProducer h264FrameProducer;

    private boolean ifStarted = false;
    SyncDecoder syncDecoder;
    DisplayThread displayThread;
    private SocketRev socketRev;
    private Thread thread;

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

        videoView = binding.textureVideoView;
        videoView.setSurfaceTextureListener(this);

        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // myDecoder = new MyDecoder(mediaCodec);
        //  myDecoder.setMediaDecodeCallback();
        // syncDecoder = new SyncDecoder(mediaCodec);
        h264FrameProducer = new H264FrameProducer();

        displayThread = new DisplayThread(mediaCodec, h264FrameProducer);

        thread = new Thread(displayThread);

        socketRev = new SocketRev(mediaCodec, h264FrameProducer, thread);

        // mediaFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
        mediaFormat = MediaFormat.createVideoFormat("video/avc", 640, 360);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        // mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);

    }

    public void getUdpPacket(byte[] data) {

        h264FrameProducer.addFrameToQueue(data);
        Log.d(TAG, "getUdpPacket: " + data.length);

        if (!ifStarted) {
            Log.d(TAG, "start decode");
            mediaCodec.start();
            ifStarted = true;

            thread.start();
        }

//        if (ifStarted) {
//            myDecoder.h264FrameProducer.addFrameToQueue(data);
//            // } else if (data.length == 20 && data[4] == 0x67) {
//        } else if (data.length == 20) {
//            // Log.d(TAG, "getUdpPacket: " + data.length + data[4]);
//            if (ifStarted == false && ifMediaDecoderConfigured) {
//                Log.d(TAG, "start decode");
//                mediaCodec.start();
//                ifStarted = true;
//                // myDecoder.renderTH.start();
//                // new Thread(syncDecoder).start();
//                // displayThread.run();
//                thread = new Thread(displayThread);
//                thread.start();
//            }
//        }
    }

    /**
     * A native method that is implemented by the 'myapplicationudprcv' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    public native void threadTest();

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.start_rev_btn:
                // threadTest();
                socketRev.init();
                break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        surfaceTexture = surface;
        mediaCodec.configure(mediaFormat, new Surface(surfaceTexture), null, 0);
        // myDecoder.configFormat(surfaceTexture);
        // mediaCodec.configure(mediaFormat, new Surface(surfaceTexture), null, 0);
        Log.d(TAG, "Surface ready ");

        ifMediaDecoderConfigured = true;

        thread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

}