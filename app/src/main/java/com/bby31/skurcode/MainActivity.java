package com.bby31.skurcode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements PermissionUtil.PermissionsCallBack{

    private ImageView imageView;
    private TextView QRText;
    private Button SaveBtn, ShareBtn;
    private LinearLayout linearLayout;
    private String QRdata;
    private Uri imageUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.main_image);
        QRText = findViewById(R.id.main_text);
        SaveBtn = findViewById(R.id.main_save);
        ShareBtn = findViewById(R.id.main_share);
        linearLayout = findViewById(R.id.main_layout);

        requestPermissions();


        SaveBtn.setOnClickListener(v -> {
            try {
                SaveToStorage(getImageOfView(imageView),"save");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        ShareBtn.setOnClickListener(v -> {
            if (imageUri!=null)
            {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                Uri imageUri2 = imageUri;
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri2);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Image");
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            }
            else
            {
                try {
                    SaveToStorage(getImageOfView(imageView),"share");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Bitmap generateQRCode(String data, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, width, height);
        int matrixWidth = bitMatrix.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixWidth, Bitmap.Config.RGB_565);

        for (int x = 0; x < matrixWidth; x++) {
            for (int y = 0; y < matrixWidth; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        boolean isAndroid10Plus = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);

        if(!isAndroid10Plus)return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip())
        {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0); // get position 0 of the clipboard (last copied item)
            String data = item.getText().toString();
            int qrData = 0;
            try {
                qrData = Integer.parseInt(data.split("\\.")[0]);
                QRdata = String.valueOf(qrData);
                try {
                    Bitmap qrCode = generateQRCode(""+qrData, 512, 512);
                    imageView.setImageBitmap(qrCode);
                    QRText.setText("QR Code Data : "+QRdata);
                    linearLayout.setVisibility(View.VISIBLE);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            } catch (NumberFormatException e) {
                // myString is not an integer
            }
        }
        super.onWindowFocusChanged(hasFocus);
    }

    public boolean requestPermissions() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (PermissionUtil.checkAndRequestPermissions(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES)) {
                return true;
            }
        }
        else
        {
            if (PermissionUtil.checkAndRequestPermissions(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults, this);
    }

    @Override
    public void permissionsGranted() {
    }

    @Override
    public void permissionsDenied() {
    }

    private void SaveToStorage(Bitmap bitmap, String page) throws IOException {
        String imageName = String.format("%d.jpg", System.currentTimeMillis());
        OutputStream fos = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = this.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        } else {
            File imagesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File image = new File(imagesDirectory, imageName);
            fos = new FileOutputStream(image);
        }

        if (fos != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (page=="save")
                Toast.makeText(this, "Image Saved!!", Toast.LENGTH_SHORT).show();
            else
            {
                if (imageUri!=null)
                {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpeg");
                    Uri imageUri2 = imageUri;
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri2);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Image");
                    startActivity(Intent.createChooser(shareIntent, "Share Image"));
                }
            }
            fos.close();
        }
    }

    private Bitmap getImageOfView(ImageView view) {
        Bitmap image = null;
        try {
            image = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            view.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }
}