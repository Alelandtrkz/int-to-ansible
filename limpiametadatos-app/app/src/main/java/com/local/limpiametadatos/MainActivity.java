package com.local.limpiametadatos;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMAGES = 1001;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView statusText;
    private ProgressBar progressBar;
    private Button selectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        int bg = Color.rgb(246, 247, 251);
        int text = Color.rgb(30, 35, 46);
        int muted = Color.rgb(99, 107, 123);
        int blue = Color.rgb(35, 102, 209);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView badge = new TextView(this);
        badge.setText("PRIVACIDAD LOCAL");
        badge.setTextColor(blue);
        badge.setTextSize(12);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = rounded(Color.rgb(226, 236, 253), 100);
        badge.setBackground(badgeBg);
        badge.setPadding(dp(12), dp(7), dp(12), dp(7));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.bottomMargin = dp(18);
        root.addView(badge, badgeLp);

        TextView title = new TextView(this);
        title.setText("LimpiaMetadatos");
        title.setTextColor(text);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Elimina metadatos de varias fotos sin Internet.");
        subtitle.setTextColor(muted);
        subtitle.setTextSize(17);
        subtitle.setPadding(0, dp(8), 0, dp(24));
        root.addView(subtitle);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(Color.WHITE, 18));
        root.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView cardTitle = new TextView(this);
        cardTitle.setText("¿Qué elimina?");
        cardTitle.setTextColor(text);
        cardTitle.setTextSize(18);
        cardTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(cardTitle);

        TextView details = new TextView(this);
        details.setText("• Ubicación GPS\n• Fecha y hora EXIF\n• Marca y modelo de cámara\n• Software y otros campos incrustados\n\nTus fotos originales no se modifican.");
        details.setTextColor(muted);
        details.setTextSize(15);
        details.setLineSpacing(0, 1.18f);
        details.setPadding(0, dp(10), 0, 0);
        card.addView(details);

        selectButton = new Button(this);
        selectButton.setText("Seleccionar fotos");
        selectButton.setTextSize(16);
        selectButton.setTextColor(Color.WHITE);
        selectButton.setAllCaps(false);
        selectButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selectButton.setBackground(rounded(blue, 14));
        selectButton.setPadding(dp(16), dp(13), dp(16), dp(13));
        selectButton.setOnClickListener(v -> pickImages());
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58));
        buttonLp.topMargin = dp(24);
        root.addView(selectButton, buttonLp);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(8));
        progressLp.topMargin = dp(18);
        root.addView(progressBar, progressLp);

        statusText = new TextView(this);
        statusText.setText("Ninguna foto seleccionada.");
        statusText.setTextColor(muted);
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(16), 0, dp(16));
        root.addView(statusText);

        TextView footer = new TextView(this);
        footer.setText("100% local  •  Sin permiso de Internet\nLas copias limpias se guardan en Pictures/LimpiaMetadatos");
        footer.setTextColor(Color.rgb(120, 127, 140));
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(0, 1.2f);
        footer.setPadding(0, dp(20), 0, 0);
        root.addView(footer);

        return scroll;
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGES || resultCode != RESULT_OK || data == null) return;

        List<Uri> uris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                uris.add(clipData.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }

        if (!uris.isEmpty()) processImages(uris);
    }

    private void processImages(List<Uri> uris) {
        selectButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        statusText.setText("Preparando " + uris.size() + (uris.size() == 1 ? " foto…" : " fotos…"));

        executor.execute(() -> {
            int ok = 0;
            int failed = 0;
            for (int i = 0; i < uris.size(); i++) {
                final int position = i + 1;
                runOnUiThread(() -> statusText.setText("Limpiando foto " + position + " de " + uris.size() + "…"));
                try {
                    cleanAndSave(uris.get(i), position);
                    ok++;
                } catch (Exception e) {
                    failed++;
                }
                int progress = Math.round(position * 100f / uris.size());
                runOnUiThread(() -> progressBar.setProgress(progress));
            }

            final int successCount = ok;
            final int failCount = failed;
            runOnUiThread(() -> {
                selectButton.setEnabled(true);
                if (failCount == 0) {
                    statusText.setText("✓ Listo: " + successCount + (successCount == 1 ? " foto limpia guardada." : " fotos limpias guardadas."));
                } else {
                    statusText.setText("Proceso terminado: " + successCount + " guardadas, " + failCount + " no pudieron procesarse.");
                }
            });
        });
    }

    private void cleanAndSave(Uri sourceUri, int index) throws Exception {
        ContentResolver resolver = getContentResolver();
        int orientation = ExifInterface.ORIENTATION_NORMAL;

        try (InputStream exifStream = resolver.openInputStream(sourceUri)) {
            if (exifStream != null) {
                ExifInterface exif = new ExifInterface(exifStream);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Exception ignored) {
            orientation = ExifInterface.ORIENTATION_NORMAL;
        }

        Bitmap bitmap;
        try (InputStream imageStream = resolver.openInputStream(sourceUri)) {
            if (imageStream == null) throw new IllegalStateException("No se pudo abrir la imagen");
            bitmap = BitmapFactory.decodeStream(imageStream);
        }
        if (bitmap == null) throw new IllegalStateException("Formato de imagen no compatible");

        Bitmap corrected = rotateForExif(bitmap, orientation);
        if (corrected != bitmap) bitmap.recycle();

        String sourceType = resolver.getType(sourceUri);
        boolean saveAsPng = "image/png".equals(sourceType) || corrected.hasAlpha();
        String extension = saveAsPng ? ".png" : ".jpg";
        String mime = saveAsPng ? "image/png" : "image/jpeg";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = "Limpia_" + timestamp + "_" + String.format(Locale.US, "%02d", index) + extension;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, mime);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LimpiaMetadatos");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (outUri == null) {
            corrected.recycle();
            throw new IllegalStateException("No se pudo crear la imagen de salida");
        }

        boolean written = false;
        try (OutputStream output = resolver.openOutputStream(outUri, "w")) {
            if (output == null) throw new IllegalStateException("No se pudo escribir la imagen");
            Bitmap.CompressFormat format = saveAsPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
            int quality = saveAsPng ? 100 : 95;
            written = corrected.compress(format, quality, output);
        } finally {
            corrected.recycle();
        }

        if (!written) {
            resolver.delete(outUri, null, null);
            throw new IllegalStateException("Error al guardar la imagen");
        }

        ContentValues ready = new ContentValues();
        ready.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(outUri, ready, null, null);
    }

    private Bitmap rotateForExif(Bitmap src, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.preScale(-1, 1);
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.preScale(-1, 1);
                matrix.postRotate(90);
                break;
            default:
                return src;
        }
        try {
            return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        } catch (OutOfMemoryError error) {
            return src;
        }
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
