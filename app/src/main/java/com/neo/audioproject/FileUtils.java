package com.neo.audioproject;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static void copyFileFromAssets(Context context, String assetsFilePath, String targetFileFullPath) throws Exception {
        File file = new File(targetFileFullPath);
        if (file.exists()) {
            return;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
        InputStream in;
        // 从assets目录下复制
        in = context.getAssets().open(assetsFilePath);
        FileOutputStream out = new FileOutputStream(file);
        int length = -1;
        byte[] buf = new byte[1024];
        while ((length = in.read(buf)) != -1) {
            out.write(buf, 0, length);
        }
        out.flush();
        in.close();
        out.close();
    }
}
