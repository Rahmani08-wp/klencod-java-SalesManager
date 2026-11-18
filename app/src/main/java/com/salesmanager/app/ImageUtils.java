package com.salesmanager.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class ImageUtils {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(30);

    private ImageUtils() {
    }

    /** Copies the picked image into app storage (resized) and returns its path, or null on failure. */
    public static String saveFromUri(Context ctx, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();

            int sample = 1;
            while ((bounds.outWidth / sample) > 1024 || (bounds.outHeight / sample) > 1024) {
                sample *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            if (bmp == null) return null;

            File dir = new File(ctx.getFilesDir(), "images");
            if (!dir.exists() && !dir.mkdirs()) return null;
            File file = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            bmp.recycle();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** Loads a down-sampled bitmap for display, or null if the file is missing. */
    public static Bitmap load(String path, int reqSize) {
        if (path == null) return null;
        String key = path + "@" + reqSize;
        Bitmap cached = CACHE.get(key);
        if (cached != null) return cached;

        File file = new File(path);
        if (!file.exists()) return null;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= reqSize && o.outHeight / (sample * 2) >= reqSize) {
            sample *= 2;
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeFile(path, o);
        if (bmp != null) CACHE.put(key, bmp);
        return bmp;
    }

    public static void deleteFile(String path) {
        if (path == null) return;
        try {
            File f = new File(path);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {
        }
    }
}
