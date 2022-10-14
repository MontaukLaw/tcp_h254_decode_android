package com.wulala.myapplicationudprcv;

import static com.wulala.myapplicationudprcv.Utils.byteToHexString;
import static com.wulala.myapplicationudprcv.Utils.charArrayToByteArray;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketRev {

    public static final int PORT = 54322;

    public static final String SEVER_IP = "120.77.179.171";

    private static final String TAG = SocketRev.class.getSimpleName();
    private static final int SPE_FRAME_LEN = 20 + 8 + 9;

    private static final int BUF_SIZE = 65535;

    byte[] dataFrameBuffer = new byte[BUF_SIZE * 100];
    byte[] speFrameBuffer = new byte[SPE_FRAME_LEN];
    int dataFrameBufferIdx = 0;
    int speFrameBufferIdx = 0;

    public static final byte I_FRAME_CHAR = 0x65;
    public static final byte D_FRAME_CHAR = 0x61;
    public static final byte SPE_FRAME_CHAR = 0x67;

    long totalRecv = 0;

    H264FrameProducer mh264FrameProducer;
    MediaCodec mDecoder;
    Thread myDisplayThread;

    private boolean ifStartDisplay = false;

    public SocketRev(MediaCodec myDecoder, H264FrameProducer _h264FrameProducer, Thread thread) {
        mh264FrameProducer = _h264FrameProducer;
        mDecoder = myDecoder;
        myDisplayThread = thread;
    }

    private byte[] trans_char_array_to_byte_array(byte[] data, int startIdx, int len) {
        byte[] frame = new byte[len];
        for (int i = 0; i < len; i++) {
            frame[i] = (byte) data[startIdx + i];
        }
        return frame;
    }

    void add_data_into_data_frame(byte[] data, int startIdx, int len) {
        // Log.d(TAG, "copy data " + len + " bytes into data frame buffer");
        System.arraycopy(data, startIdx, dataFrameBuffer, dataFrameBufferIdx, len);
        dataFrameBufferIdx += len;
    }

    void add_data_into_spe_frame(byte[] data, int startIdx, int len) {
        if (len >= 0) System.arraycopy(data, startIdx, speFrameBuffer, speFrameBufferIdx, len);
        speFrameBufferIdx += len;
    }

    private int getFrameSize(char keyByte) {
        switch (keyByte) {
            case 0x67:
                return 20;
            case 0x68:
                return 8;
            case 0x06:
                return 9;
            default:
                return 0;
        }
    }

    boolean leftFrameTail = false;
    boolean leftSPETail = false;

    byte[] cutFrame(byte[] data, int startIdx, int len) {
        byte[] frame = new byte[len];
        System.arraycopy(data, startIdx, frame, 0, len);
        return frame;
    }

    private void handleSPEFrameWrite() {
        totalHandle = totalHandle + SPE_FRAME_LEN;
        // writeFile(speFrameBuffer, SPE_FRAME_LEN);
    }

    private void handleSPEFrame() {
        byte[] spsFrame = cutFrame(speFrameBuffer, 0, 20);
        byte[] ppsFrame = cutFrame(speFrameBuffer, 20, 8);
        byte[] seiFrame = cutFrame(speFrameBuffer, 28, 9);

        mh264FrameProducer.addFrameToQueue(spsFrame);
        mh264FrameProducer.addFrameToQueue(ppsFrame);
        mh264FrameProducer.addFrameToQueue(seiFrame);

        speFrameBufferIdx = 0;

        Log.d(TAG, "-------handleSPEFrame: ");
    }

    long totalHandle = 0;

    private void handleDataFrameWrite() {
        totalHandle = totalHandle + dataFrameBufferIdx;
        Log.d(TAG, "totalRecv: " + totalRecv);
        Log.d(TAG, "totalHandle: " + totalHandle);

        // writeFile(dataFrameBuffer, dataFrameBufferIdx);
        dataFrameBufferIdx = 0;
    }

    private void handleDataFrame() {

        if (ifStartDisplay) {
            // Log.d(TAG, "-------handleDataFrame: " + dataFrameBufferIdx);
            byte[] frame = cutFrame(dataFrameBuffer, 0, dataFrameBufferIdx);

            mh264FrameProducer.addFrameToQueue(frame);

//            if (frame[4] != 0x65) {
//                Log.d(TAG, "p/b data frame: " + byteToHexString(frame[4]));
//            } else {
//                Log.d(TAG, "i frame");
//            }

            // Log.d(TAG, "-------handleDataFrame: " + dataFrameBufferIdx);
            dataFrameBufferIdx = 0;
        }

    }

    private boolean ifFoundSPEFrame(byte[] data, int packetLen) {
        if (packetLen == 20 && (data[4] == 0x67)) {
            return true;
        } else if (packetLen == 8 && (data[4] == 0x68)) {
            return true;
        } else if (packetLen == 9 && (data[4] == 0x06)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean ifFoundKeyFrame(byte[] data, int startIdx, byte keyByte) {
        return data[startIdx] == 0x00 && data[startIdx + 1] == 0x00
                && data[startIdx + 2] == 0x00 && data[startIdx + 3] == 0x01 && data[startIdx + 4] == keyByte;
    }

    private void handle_spe_data(byte[] data, int dataIdx) {
        if (dataIdx + SPE_FRAME_LEN < BUF_SIZE) {
            Log.d(TAG, "complete spe");

            speFrameBufferIdx = 0;
            // 加入全部数据
            add_data_into_spe_frame(data, dataIdx, SPE_FRAME_LEN);
            // 发送到解码器
            handleSPEFrame();

            leftSPETail = false;

        } else {
            Log.d(TAG, "leftSPETail");
            // 加入一半的数据
            add_data_into_spe_frame(data, dataIdx, BUF_SIZE - dataIdx);
            // 标注SPE帧没有处理完
            leftSPETail = true;
        }
    }

    public void checkUDPData(byte[] data, int dataLen) {
        if (ifStartDisplay && ifFoundKeyFrame(data, 0, I_FRAME_CHAR)) {
            Log.d(TAG, "I frame found: ");
            add_data_into_data_frame(data, 0, dataLen);
        } else if (ifStartDisplay && ifFoundKeyFrame(data, 0, D_FRAME_CHAR)) {
            // Log.d(TAG, "Data frame found: ");
            // 发送到解码器
            handleDataFrame();
            // 要将目前的数据添加进数据缓存
            add_data_into_data_frame(data, 0, dataLen);

        } else if (ifFoundSPEFrame(data, dataLen)) {
            // 如果是SPS, 还需要发送前面的数据帧
            if (dataLen == 20 && (data[4] == 0x67)) {
                handleDataFrame();
            }
            // Log.d(TAG, "SPE frame found: ");
            mh264FrameProducer.addFrameToQueue(data);
            if (!ifStartDisplay) {
                Log.d(TAG, "ifStartDisplay true ");
                ifStartDisplay = true;
            }
        } else if (ifStartDisplay) {
            add_data_into_data_frame(data, 0, dataLen);
        }
    }

    // 核心
    public void checkData(byte[] data, int dataLen) {
        // Log.d(TAG, "checkData: " + data.length);
        boolean ifFoundKeyFrame = false;
        for (int dataIdx = 0; dataIdx < dataLen - 5; dataIdx++) {
            // 发现I帧的时候
            if (ifStartDisplay && ifFoundKeyFrame(data, dataIdx, I_FRAME_CHAR)) {
                Log.d(TAG, "I frame found: " + dataIdx);

                ifFoundKeyFrame = true;
                // 表示SPE帧没有处理完
                if (leftSPETail) {

                    Log.d(TAG, "found leftSPETail");

                    // 1. 把数据装入缓存
                    add_data_into_spe_frame(data, 0, dataIdx);

                    // 2. 恢复索引
                    speFrameBufferIdx = 0;

                    // 3. 处理SPE帧
                    handleSPEFrame();

                    // 4. 恢复状态机
                    leftSPETail = false;

                } else {
                    // 把后面的数据装入缓存
                    add_data_into_data_frame(data, dataIdx, dataLen - dataIdx);
                }

                // 发现S.P.E.
            } else if (ifFoundKeyFrame(data, dataIdx, SPE_FRAME_CHAR)) {
                // 发现S.P.E.的时候
                Log.d(TAG, "SPE frame found: " + dataIdx);
                ifFoundKeyFrame = true;

                // 如果第1次发现SPE帧
                if (!ifStartDisplay) {

                    Log.d(TAG, "Start display thread: ");

                    // mDecoder.start();

                    // 修改状态机
                    ifStartDisplay = true;

                    // 发送SPE数据包
                    handle_spe_data(data, dataIdx);

                } else {

                    // 不是第一次, 需要把前面的数据装进缓存, 然后发送到解码器
                    if (dataIdx > 0) {
                        // 把前面的数据放入数据缓存, 从0, 共dataIdx个字节
                        add_data_into_data_frame(data, 0, dataIdx);
                        // 发送到解码器
                        handleDataFrame();
                    }

                    // 并发送SPE数据包
                    handle_spe_data(data, dataIdx);
                }

            } else if (ifStartDisplay && ifFoundKeyFrame(data, dataIdx, D_FRAME_CHAR)) {
                Log.d(TAG, "D frame found: " + dataIdx);
                ifFoundKeyFrame = true;

                if (dataIdx > 0) {
                    // 把前面的数据放入数据缓存
                    add_data_into_data_frame(data, 0, dataIdx);
                }
                // 把缓存的数据发送到解码器
                handleDataFrame();
                // 把后面的数据放入数据缓存
                add_data_into_data_frame(data, dataIdx, dataLen - dataIdx);
            }
        }

        if (ifStartDisplay && !ifFoundKeyFrame) {
            add_data_into_data_frame(data, 0, dataLen);
        }
    }
    // 基本上不怎么存在一个帧的数据小于1024的情况

    // Log.d(TAG, "key byte is : " + byteToHexString(data[i + 4]));
    // 因为数据被切重新组合存在3种情况, 而且这三种情况的发生概率是一样的
    // 1. S P E帧完整的包含在在1024的buf中
    // 2. 只有头, 没有尾巴
    // 3. 只有尾巴, 没有头
/*
                if (data[i + 4] == 0x67) {
                    Log.d(TAG, "S: " + i);


                    // 第1步, 把剩下的数据放入队列

                    // 第一步生成sps帧数据
                    byte[] spsFrame = trans_char_array_to_byte_array(data, i, 20);

                    if (!ifStartDisplay) {

                        myDisplayThread.start();

                        mDecoder.start();
                        ifStartDisplay = true;
                    }

                    mh264FrameProducer.addFrameToQueue(spsFrame);

                } else if (data[i + 4] == 0x68) {
                    Log.d(TAG, "P: " + i);
                    if (ifStartDisplay) {
                        byte[] ppsFrame = trans_char_array_to_byte_array(data, i, 8);
                        mh264FrameProducer.addFrameToQueue(ppsFrame);
                    }
                } else if (data[i + 4] == 0x65) {
                    Log.d(TAG, "I: " + i);
                    if (ifStartDisplay) {
                        byte[] iFrame = trans_char_array_to_byte_array(data, i, BUF_SIZE - i);
                        mh264FrameProducer.addFrameToQueue(iFrame);
                    }
                } else if (data[i + 4] == 0x06) {
                    Log.d(TAG, "E: " + i);
                    if (ifStartDisplay) {
                        byte[] seiFrame = trans_char_array_to_byte_array(data, i, 9);
                        mh264FrameProducer.addFrameToQueue(seiFrame);
                    }
                } else {
                    if (ifStartDisplay) {
                        // add_data_into_data_frame(data, i, BUF_SIZE - i);
                        byte[] dataFrame = trans_char_array_to_byte_array(data, i, BUF_SIZE - i);
                        mh264FrameProducer.addFrameToQueue(dataFrame);
                    }
                }
 */


    public void init() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    DatagramSocket ds = new DatagramSocket(PORT);
                    //创建字节数组
                    byte[] data = new byte[BUF_SIZE];
                    byte[] byteData = new byte[BUF_SIZE];
                    //创建数据包对象，传递字节数组
                    DatagramPacket dp = new DatagramPacket(data, data.length);
                    int readBytes = 0;
                    while (true) {
                        //接收数据
                        ds.receive(dp);
                        //解析数据
                        // String ip=dp.getAddress().getHostAddress();
                        // String text=new String(dp.getData(),0,dp.getLength());
                        //输出数据
                        // System.out.println(ip+":"+text);
                        readBytes = dp.getLength();
                        byteData = dp.getData();
                        Log.d(TAG, "readBytes: " + readBytes);
                        checkData(byteData, readBytes);

                        byteData = new byte[BUF_SIZE];

                        // writeFile(byteData, readBytes);
                        // data = new byte[BUF_SIZE];
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        /*
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {

                    // DatagramSocket ds = new DatagramSocket(PORT);
                    Socket socket = new Socket(SEVER_IP, PORT);

                    InputStream is = socket.getInputStream();

                    // InputStreamReader reader = new InputStreamReader(is);
                    // BufferedReader bufferedReader = new BufferedReader(reader);
                    // char[] buffer = new char[BUF_SIZE];
                    byte[] byteData = new byte[BUF_SIZE];
                    DatagramPacket dp = new DatagramPacket(byteData, byteData.length);
                    int readBytes = 0;
                    while (readBytes != -1) {
                        readBytes = is.read(byteData);
                        // readBytes = bufferedReader.read(buffer);
                        if (readBytes > 0) {
                            totalRecv += readBytes;
                            checkData(byteData, readBytes);
                            // writeFile(byteData, readBytes);
                            byteData = new byte[BUF_SIZE];
                            // Log.d(TAG, "receive message from server, size:" + buffer.length);
                            // System.out.println("receive message from server, size:" + buffer.length + " msg: " + new String(buffer));

                            // 转一下
                            // byte[] writeBuf = charArrayToByteArray(buffer, readBytes);
                            // Log.d(TAG, "head: " + byteToHexString(writeBuf[0]) + " " + byteToHexString(writeBuf[1]) + " " + byteToHexString(writeBuf[2]) + " " + byteToHexString(writeBuf[3]));

                            // Log.d(TAG, "head: " + byteToHexString(byteData[0]) + " " + byteToHexString(byteData[1]) + " " + byteToHexString(byteData[2]) + " " + byteToHexString(byteData[3]));
                            // writeFile(writeBuf, readBytes);
                            // buffer = new char[BUF_SIZE];
                        }

                        // checkData(charArrayToByteArray(buffer, readBytes));
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // bufferedReader.close();
                    // reader.close();
                    is.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.

                start();
         */
    }
}
