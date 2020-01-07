package com.deepbay.webviewdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.csii.powerenter.PEEditText;
import com.csii.powerenter.PEEditTextAttrSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView mText;
    private Button mButtonLoad;
    private Button mButtonEvaluate;
    private WebView mWebview;
    private ImageView mImage;
    private Button mTake;
    private static final int REQUEST_CAPTURE_BACK = 0x01;
    private String mCurrentPhotoPath;
    private PEEditText mPwd1;
    private static final String NAME_1 = "name1";
    private MyReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText = findViewById(R.id.text);
        mButtonLoad = findViewById(R.id.button_load);
        mButtonEvaluate = findViewById(R.id.button_evaluate);
        mWebview = findViewById(R.id.webview);

        initWebView();
        mWebview.addJavascriptInterface(new JavaMethod(this), "android");
        mWebview.loadUrl("file:///android_asset/index.html");
//        mWebview.loadUrl("file:///" + Environment.getExternalStorageDirectory() + File.separator + "index.html");

        mButtonLoad.setOnClickListener(v -> {
            mWebview.loadUrl("javascript:javatojscallback('我来自Java')");
        });

        mButtonEvaluate.setOnClickListener(v -> {
            String param = "我来自Java";
            mWebview.evaluateJavascript("javascript:javatojswith('" + param + "')", value -> {
                mText.setText(mPwd1.getClearText());
            });
        });
        mImage = findViewById(R.id.image);

//        mImage.setImageBitmap(
//                decodeSampledBitmapFromResource(getResources(), R.mipmap.test, 100, 100));
        mTake = findViewById(R.id.take);

        mTake.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAPTURE_BACK);
            } else {
                takePhotoNoCompress(REQUEST_CAPTURE_BACK);
            }
        });
        mPwd1 = findViewById(R.id.pwd1);
        mPwd1.initialize(getAttrs(NAME_1));
        receiver = new MyReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.csii.powerenter.action.Send_msg");
        this.registerReceiver(receiver, filter);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhotoNoCompress(requestCode);
        } else {
            Toast.makeText(this, "You denied camera permission,please granted it", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebSettings webSettings = mWebview.getSettings();
        //5.0以上开启混合模式加载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        //允许js代码
        webSettings.setJavaScriptEnabled(true);
        //禁用放缩
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);
        //禁用文字缩放
        webSettings.setTextZoom(100);
        //允许缓存，设置缓存位置
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(this.getDir("appcache", 0).getPath());

//        webSettings.setAllowFileAccessFromFileURLs(true); //Maybe you don't need this rule
        webSettings.setAllowUniversalAccessFromFileURLs(true);
    }

    public void setShowText(String param) {
        mText.setText(param);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static Bitmap decodeSampledBitmapFromFile(String file,
                                                     int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }

    public void takePhotoNoCompress(int code) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            String filename = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                    .format(new Date()) + ".png";
            //UUID.randomUUID().toString()
//            String filename = System.currentTimeMillis() + ".jpg";
//            File file = new File(Environment.getExternalStorageDirectory(), filename);
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
            mCurrentPhotoPath = file.getAbsolutePath();

            Uri fileUri = FileUtils.getUriForFile(this, file);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, code);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_BACK && resultCode == Activity.RESULT_OK) {
            mImage.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
            saveFile(decodeSampledBitmapFromFile(mCurrentPhotoPath, 480, 800));
        }

//
//        String imageBase64 = response.getString("image");
////                        String base64 = "data:image/png;" + imageBase64;
//        byte[] decodedString = Base64.decode(imageBase64, Base64.NO_PADDING);
//        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//        mImage.setImageBitmap(decodedByte);
    }

    private void saveFile(Bitmap bitmap) {
        String filename = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                .format(new Date()) + ".png";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
                Log.d("MainActivity", "saveFile: ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openPEKbd() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        mPwd1.openPEKbd();
    }

    private PEEditTextAttrSet getAttrs(String name) {
        PEEditTextAttrSet attrs = new PEEditTextAttrSet(); //这里设置attrs内容，比如attrs.textColor = Color.WHITE;
        attrs.name = name;
        attrs.immersiveStyle = true;
        attrs.softkbdType = PEEditTextAttrSet.SOFT_KBD_QWER_NUMBER;
//        attrs.touchTransprtClosed = false;
//        attrs.clearWhenOpenKbd = false;
        return attrs;
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String name = bundle.getString("PEKbdName");
            String type = bundle.getString("PEKbdInfo");
            if (type != null) {
                switch (type) {
                    case "kbdchanged":
                        int length = 0;
                        String jsStr = "";
                        if (NAME_1.equals(name)) {
                            length = mPwd1.getLength();
                            Log.d("kbdchanged", "onReceive: " + length);
                            Log.d("kbdchanged", "onReceive: " + mPwd1.getValue(String.valueOf(System.currentTimeMillis())));
                            jsStr = "getTradePassword";
                            mWebview.evaluateJavascript("javascript:" + jsStr + "(" + length + ")",
                                    new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String s) {

                                        }
                                    });
                        }

                        break;
                    default:
                }
            }
        }
    }
}
