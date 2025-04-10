package com.example.jxw.util;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {
    private static final int COMPRESSION_QUALITY = 60;

    public static byte[] compressImage(ByteBuffer buffer, int width, int height) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, stream);

        bitmap.recycle();
        return stream.toByteArray();
    }
}
