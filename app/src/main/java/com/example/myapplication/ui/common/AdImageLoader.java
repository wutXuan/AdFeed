package com.example.myapplication.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.widget.ImageView;

import com.example.myapplication.model.AdItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class AdImageLoader {
    private static final int IMAGE_WIDTH = 1200;
    private static final int IMAGE_HEIGHT = 720;

    private AdImageLoader() {
    }

    public static void load(ImageView imageView, AdItem item) {
        if (item == null) {
            imageView.setImageBitmap(null);
            return;
        }
        File imageFile = ensureLocalImage(imageView.getContext(), item);
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imageView.setAlpha(1f);
        imageView.setImageBitmap(bitmap);
        imageView.setBackgroundColor(startColor(item.getId()));
    }

    private static File ensureLocalImage(Context context, AdItem item) {
        File directory = new File(context.getFilesDir(), "ad_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File imageFile = new File(directory, item.getId() + ".png");
        if (!imageFile.exists() || imageFile.length() == 0) {
            writeImage(imageFile, item);
        }
        return imageFile;
    }

    private static void writeImage(File imageFile, AdItem item) {
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int startColor = startColor(item.getId());
        int endColor = endColor(item.getId());
        paint.setShader(new LinearGradient(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT,
                startColor, endColor, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, paint);

        paint.setShader(null);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(96, 112, 760, 430, 36, 36, paint);
        paint.setColor(Color.argb(72, 255, 255, 255));
        canvas.drawCircle(980, 210, 170, paint);
        canvas.drawCircle(900, 520, 260, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(64f);
        paint.setFakeBoldText(true);
        canvas.drawText(item.getBrand(), 150, 235, paint);

        paint.setTextSize(44f);
        paint.setFakeBoldText(false);
        canvas.drawText(item.getTitle(), 150, 315, paint);

        try (FileOutputStream output = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (IOException ignored) {
            // The ImageView background still shows the selected ad color if file creation fails.
        } finally {
            bitmap.recycle();
        }
    }

    private static int startColor(String id) {
        switch (group(id)) {
            case 0:
                return Color.parseColor("#1F7A8C");
            case 1:
                return Color.parseColor("#2D6A4F");
            case 2:
                return Color.parseColor("#C76F00");
            case 3:
                return Color.parseColor("#D1495B");
            case 4:
                return Color.parseColor("#006D77");
            case 5:
                return Color.parseColor("#7B2CBF");
            default:
                return Color.parseColor("#64748B");
        }
    }

    private static int endColor(String id) {
        switch (group(id)) {
            case 0:
                return Color.parseColor("#A7C7E7");
            case 1:
                return Color.parseColor("#B7E4C7");
            case 2:
                return Color.parseColor("#F8DDA4");
            case 3:
                return Color.parseColor("#FFD6BA");
            case 4:
                return Color.parseColor("#B8F2E6");
            case 5:
                return Color.parseColor("#FFC8DD");
            default:
                return Color.parseColor("#E2E8F0");
        }
    }

    private static int group(String id) {
        switch (id) {
            case "featured-01":
            case "shop-04":
            case "local-03":
                return 0;
            case "featured-03":
            case "shop-03":
            case "local-06":
                return 1;
            case "featured-02":
            case "shop-01":
            case "local-02":
                return 2;
            case "featured-06":
            case "shop-06":
            case "local-04":
                return 3;
            case "featured-04":
            case "shop-02":
            case "local-05":
                return 4;
            case "featured-05":
            case "shop-05":
            case "local-01":
                return 5;
            default:
                return -1;
        }
    }
}
