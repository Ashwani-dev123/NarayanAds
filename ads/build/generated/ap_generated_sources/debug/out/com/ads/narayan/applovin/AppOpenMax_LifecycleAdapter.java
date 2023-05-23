package com.ads.narayan.applovin;

import androidx.lifecycle.GeneratedAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MethodCallsLogger;
import java.lang.Override;

public class AppOpenMax_LifecycleAdapter implements GeneratedAdapter {
  final AppOpenMax mReceiver;

  AppOpenMax_LifecycleAdapter(AppOpenMax receiver) {
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
  }
}
