package com.wulala.myapplicationudprcv;

public class Utils {
    public static String byteToHexString(byte input) {
        String hexString = Integer.toHexString(input & 0xFF);
        if (hexString.length() == 1) {
            hexString = '0' + hexString;
        }
        return hexString;
    }

    public static byte[] charArrayToByteArray(char[] input, int len){
        byte[] output = new byte[len];
        for(int i = 0; i < len; i++){
            output[i] = (byte) input[i];
        }
        return output;
    }
}
