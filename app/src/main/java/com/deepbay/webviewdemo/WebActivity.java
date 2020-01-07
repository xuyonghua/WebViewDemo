package com.deepbay.webviewdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class WebActivity extends AppCompatActivity {

    private static final String TAG = "WebActivity";
    private WebView mWebview;
    private static final int REQUEST_CAPTURE_BACK = 0x01;
    private static final int REQUEST_PICK = 0x02;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        mWebview = findViewById(R.id.webview);
        initWebView();
        mWebview.loadUrl("file:///android_asset/test.html");
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
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(this.getDir("appcache", 0).getPath());

//        webSettings.setAllowFileAccessFromFileURLs(true); //Maybe you don't need this rule
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        mWebview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                Log.d(TAG, "onShowFileChooser: " + fileChooserParams.getTitle());
                Log.d(TAG, "onShowFileChooser: " + fileChooserParams.getFilenameHint());
                Log.d(TAG, "onShowFileChooser: " + fileChooserParams.getMode());
                Log.d(TAG, "onShowFileChooser: " + fileChooserParams.isCaptureEnabled());
                Log.d(TAG, "onShowFileChooser: " + Arrays.toString(fileChooserParams.getAcceptTypes()));
                mUploadCallbackAboveL = filePathCallback;
                if (fileChooserParams.isCaptureEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAPTURE_BACK);
                    } else {
                        takePhotoNoCompress(REQUEST_CAPTURE_BACK);
                    }
                } else {
                    goPhotoAlbum();
                }


                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhotoNoCompress(requestCode);
        } else {
            Toast.makeText(this, "You denied camera permission,please granted it", Toast.LENGTH_SHORT).show();
        }
    }

    private String mCurrentPhotoPath;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;

    public void takePhotoNoCompress(int code) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            String filename = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                    .format(new Date()) + ".png";
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
            mCurrentPhotoPath = file.getAbsolutePath();

            imageUri = FileUtils.getUriForFile(this, file);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, code);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mUploadCallbackAboveL != null) {
            onActivityResultAboveL(requestCode, resultCode, data);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
//        if (requestCode != REQUEST_CAPTURE_BACK || mUploadCallbackAboveL == null) {
//            return;
//        }

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                results = new Uri[]{imageUri};
            } else {
                String dataString = data.getDataString();
                String pathByUri = FileUtils.getFilePathByUri(this, data.getData());
                Log.d(TAG, "onActivityResultAboveL: " + pathByUri);
                if (!TextUtils.isEmpty(pathByUri)) {
                    Uri uri = FileUtils.getUriForFile24(this, new File(pathByUri));
                    Log.d(TAG, "onActivityResultAboveL: " + uri.toString());
                    results = new Uri[]{uri};
                }
//                ClipData clipData = data.getClipData();
//                if (clipData != null) {
//                    results = new Uri[clipData.getItemCount()];
//                    for (int i = 0; i < clipData.getItemCount(); i++) {
//                        ClipData.Item item = clipData.getItemAt(i);
//                        results[i] = item.getUri();
//                    }
//                }
//
//                if (dataString != null)
//                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mUploadCallbackAboveL.onReceiveValue(results);
        mUploadCallbackAboveL = null;
    }

    //激活相册操作
    private void goPhotoAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK);
    }
}
