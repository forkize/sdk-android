package com.forkize.sdk.messaging;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.forkize.sdk.ForkizeConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ForkizeTemplateManager {

    private final String FOLDER = "forkize";
    private static ForkizeTemplateManager instance = new ForkizeTemplateManager();

    private ForkizeTemplateManager() {
    }

    public static ForkizeTemplateManager getInstance() {
        return instance;
    }

    public void saveHash(String key, String hash) {
        SharedPreferences preferences = ForkizeConfig.getInstance().getApplicationContext().getSharedPreferences("forkize_template", 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, hash);
        editor.apply();
    }

    public String getHash(String key) {
        SharedPreferences preferences = ForkizeConfig.getInstance().getApplicationContext().getSharedPreferences("forkize_template", 0);
        return preferences.getString(key, "0");
    }

    public void writeFile(String fileName, String content) {
        File file = new File(ForkizeConfig.getInstance().getApplicationContext().getCacheDir().getAbsolutePath(), FOLDER);
        if (file.isDirectory() || file.mkdir()) {
            writeFile(file.getAbsolutePath(), fileName, content);
        }
    }

    public void writeFile(@NonNull String dir, String fileName, String content) {
        File file = new File(dir, fileName);
        try {
            // TODO::REMOVE
            Log.e("Forkize SDK", "TemplateManager:Writing\n" + fileName + "\n" + content);

            byte[] byteArray = content.getBytes("UTF-8");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(byteArray, 0, byteArray.length);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readFiles(String fileName) {
        File file = new File(ForkizeConfig.getInstance().getApplicationContext().getCacheDir().getAbsolutePath(), FOLDER);
        if (file.isDirectory()) {
            return readFiles(file.getAbsolutePath(), fileName);
        }
        return null;
    }

    public String readFiles(@NonNull String dir, String fileName) {
        String content = null;
        File file = new File(dir, fileName);
        byte[] bytes = new byte[(int) file.length()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();

            content = new String(bytes, "UTF-8");
            // TODO::REMOVE
            Log.e("Forkize SDK", "TemplateManager:Reading\n" + fileName + "\n" + content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}