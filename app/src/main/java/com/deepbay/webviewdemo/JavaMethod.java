package com.deepbay.webviewdemo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class JavaMethod {
    private MainActivity mainActivity;
    private Handler uiHandler;

    public JavaMethod(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @JavascriptInterface
    public void JsToJavaInterface(String param) {
        uiHandler.post(() -> {
            Log.d("JavaMethod", "JsToJavaInterface: " + Thread.currentThread().getName());
            mainActivity.setShowText(param);
        });
    }

    @JavascriptInterface
    public boolean showKeyboard() {
        uiHandler.post(() -> {
            Log.d("JavaMethod", "JsToJavaInterface: " + Thread.currentThread().getName());
            mainActivity.openPEKbd();
        });
        return true;
    }
}
