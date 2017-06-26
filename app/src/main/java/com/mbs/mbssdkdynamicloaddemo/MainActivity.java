package com.mbs.mbssdkdynamicloaddemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;

/**
 * make jar with `dx --dex --output=dest.jar src.jar`
 */

public class MainActivity extends Activity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String TOKEN = "fd30cd7d-2f70-6abc-2ace-915cf72b8a30";
  private static final String PLACEMENT_ID = "1662684189370000_1769833153869302";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Button btn = new Button(this);
    btn.setText("LoadAd");
    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String srcPath = "msadsdk-1.0.2.20.dex.jar";
        String destPath = getFilesDir() + File.separator + "dex.jar";
        try {
          copyAsset(srcPath, destPath);
          loadJar(destPath);
        } catch (IOException e) {
          e.printStackTrace();
        }

      }
    });
    setContentView(btn);
  }

  private void copyAsset(String srcName, String destPath) throws IOException {
    File dest = new File(destPath);
    OutputStream outputStream = new FileOutputStream(dest);
    InputStream inputSteam = getAssets().open(srcName);
    byte[] buffer = new byte[1024];
    int length = inputSteam.read(buffer);
    while (length > 0) {
      outputStream.write(buffer, 0, length);
      length = inputSteam.read(buffer);
    }

    outputStream.flush();
    inputSteam.close();
    outputStream.close();
  }

  private Object mInterstitialAd;

  public void loadJar(String dexPath) {
    File dexOptimized = this.getDir("dexOptimized", MODE_PRIVATE);
    final DexClassLoader classloader =
        new DexClassLoader(dexPath, dexOptimized.getAbsolutePath(), null, getClassLoader());
    try {
      Class<?> clsAdSdk = classloader.loadClass("com.mbs.sdk.rich.ads.MSAdsSdk");
      Class<?> clsInterstitialAd = classloader.loadClass("com.mbs.sdk.rich.ads.InterstitialAd");
      Class<?> clsAdRequest = classloader.loadClass("com.mbs.sdk.rich.ads.AdRequest");
      Class<?> clsAdListener = classloader.loadClass("com.mbs.sdk.rich.ads.AdListener");

      ReflectHelper.invokeStatic(clsAdSdk,
                                 "start",
                                 new Class<?>[] {Context.class, String.class},
                                 this,
                                 TOKEN);

      mInterstitialAd =
          ReflectHelper.construct(clsInterstitialAd, new Class<?>[] {Context.class}, this);
      Object adRequestBuilder = ReflectHelper.invokeStatic(clsAdRequest, "newBuilder");
      adRequestBuilder = ReflectHelper.invoke(adRequestBuilder,
                                              "placementId",
                                              PLACEMENT_ID);
      adRequestBuilder = ReflectHelper.invoke(adRequestBuilder, "displayInWindow");
      Object adRequest = ReflectHelper.invoke(adRequestBuilder, "build");
      Object adListenerProxy = Proxy.newProxyInstance(clsAdListener.getClassLoader(),
                                                      new Class<?>[] {clsAdListener},
                                                      mAdListenerInvocationHandler);
      ReflectHelper.invoke(mInterstitialAd,
                           clsInterstitialAd,
                           "setAdListener",
                           new Class<?>[] {clsAdListener},
                           adListenerProxy);
      ReflectHelper.invoke(mInterstitialAd, "loadAd", adRequest);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private InvocationHandler mAdListenerInvocationHandler = new InvocationHandler() {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      switch (name) {
        case "onAdLoaded":
          Log.d(TAG, "onAdLoaded");
          ReflectHelper.invoke(mInterstitialAd, "show");
          break;
        case "onAdClosed":
          Log.d(TAG, "onAdClosed");
          break;
        case "onAdShowed":
          Log.d(TAG, "onAdShowed");
          break;
        case "onAdClicked":
          Log.d(TAG, "onAdClicked");
          break;
        case "onAdError":
          Log.d(TAG, "onAdError");
          break;
      }
      return null;
    }
  };
}


