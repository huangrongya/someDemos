package com.etekcity.vbmp.common.utils;

import java.io.UnsupportedEncodingException;

public class MyHexUtils {
    // byte转为hex串
    static String bytes2HexStr(byte[] byteArr) {
        if (null == byteArr || byteArr.length < 1) return "";
        StringBuilder sb = new StringBuilder();
        for (byte t : byteArr) {
            if ((t & 0xF0) == 0) sb.append("0");
            sb.append(Integer.toHexString(t & 0xFF));  //t & 0xFF 操作是为去除Integer高位多余的符号位（java数据是用补码表示）
        }
        return sb.toString();
    }

    // hex串转为byte
    static String hexStr2Bytes(String hexStr) {
        if (null == hexStr || hexStr.length() < 1) return null;

        int byteLen = hexStr.length() / 2;
        byte[] result = new byte[byteLen];
        char[] hexChar = hexStr.toCharArray();
        for (int i = 0; i < byteLen; i++) {
            result[i] = (byte) (Character.digit(hexChar[i * 2], 16) << 4 | Character.digit(hexChar[i * 2 + 1], 16));
        }
        String resultString = null;
        try {
            resultString = new String(result, "utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return resultString;
    }
}
