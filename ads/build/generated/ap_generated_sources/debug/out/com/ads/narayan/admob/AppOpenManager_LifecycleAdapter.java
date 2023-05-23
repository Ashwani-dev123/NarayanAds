package com.ads.narayan.admob;

import androidx.lifecycle.GeneratedAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MethodCallsLogger;
import java.lang.Override;

public class AppOpenManager_LifecycleAdapter implements GeneratedAdapter {
  final AppOpenManager mReceiver;

  AppOpenManager_LifecycleAdapter(AppOpenManager receiver) {
    this.mReceiver = receiver;
  }

  @Override
  public void callMethods(LifecycleOwner owner, Lifecycle.Event event, boolean onAny,
      MethodCallsLogger logger) {
    boolean hasLogger = logger != null;
    if (onAny) {
      return;
    }
    if (event == Lifecycle.Event.ON_START) {
      if (!hasLogger || logger.approveCall("onResume", 1)) {
        mReceiver.onResume();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_STOP) {
      if (!hasLogger || logger.approveCall("onStop", 1)) {
        mReceiver.onStop();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_PAUSE) {
      if (!hasLogger || logger.approveCall("onPause", 1)) {
        mReceiver.onPause();
      }
      return;
    }
  }
}
