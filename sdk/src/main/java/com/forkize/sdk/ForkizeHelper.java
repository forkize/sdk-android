package com.forkize.sdk;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ForkizeHelper {

    public static boolean isNullOrEmpty(String str) {
        return (str == null || str.length() == 0);
    }

    public static String md5(String text) {
        if (text == null) {
            return null;
        }
        try {
            byte[] bytesOfMessage = text.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(bytesOfMessage);

            StringBuilder hexDigest = new StringBuilder();
            for (byte aHash : hash) {
                if ((0xFF & aHash) < 16) {
                    hexDigest.append("0");
                }
                hexDigest.append(Integer.toHexString(0xFF & aHash));
            }
            return hexDigest.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.wtf("Forkize SDK", "Couldn't find MD5", e);
        }
        return "";
    }


    // FZ::TODO the view of key
    public static boolean isKeyValid(String key) {
        if (key.length() > 255 || key.startsWith("$")) {
            Log.e("Forkize SDK", "The " + key + " key is not valid, it shouldn't start with $ and length must be less than 255");
            return false;
        }
        return true;
    }
}