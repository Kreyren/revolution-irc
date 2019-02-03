package io.mrarm.irc.chat.preview.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;

public class ImageCacheManager {

    private final File mCacheDir;
    private final LinkPreviewDatabase mDatabase;

    public ImageCacheManager(Context context, LinkPreviewDatabase db) {
        mDatabase = db;
        mCacheDir = new File(context.getCacheDir(), "image_preview");
        mCacheDir.mkdirs();
    }

    private synchronized ImageCacheEntry findEntryFor(String url) {
        ImageCacheEntry ret = mDatabase.imageCacheDao().findEntryFor(url);
        if (ret != null) {
            ret.updateLastUsed();
            mDatabase.imageCacheDao().updateEntry(ret);
        }
        return ret;
    }

    public Bitmap getImageFromCache(String url) {
        ImageCacheEntry entry = findEntryFor(url);
        if (entry == null)
            return null;
        File file = new File(mCacheDir, getURLHash(url));
        if (!file.exists())
            return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public void storeImageInCache(String url, Bitmap bitmap) {
        ImageCacheEntry entry = new ImageCacheEntry(url);
        File file = new File(mCacheDir, getURLHash(url));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            Log.w("ImageCacheManager", "Failed to store image in cache");
            e.printStackTrace();
            return;
        }
        mDatabase.imageCacheDao().insertEntry(entry);
    }

    private static String getURLHash(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.encodeToString(
                    digest.digest(url.getBytes("UTF-8")),
                    Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
