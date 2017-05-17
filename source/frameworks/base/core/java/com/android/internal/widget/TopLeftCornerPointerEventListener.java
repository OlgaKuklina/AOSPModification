/*
* Copyright (C) 2010 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.internal.widget;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy.PointerEventListener;
import android.os.Vibrator;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.KeyguardManager;
import android.provider.Settings.Secure;
import android.util.Log;
public class TopLeftCornerPointerEventListener implements PointerEventListener {
    private static final String TAG = TopLeftCornerPointerEventListener.class.getSimpleName();
    private final Object mScreenshotLock = new Object();
    private ServiceConnection mScreenshotServiceConnection = null;
    private final Context mContext;
    private final GestureDetector mDetector;
    private final KeyguardManager mKeyguardManager;

    public TopLeftCornerPointerEventListener(Context c) {
        mContext = c;
        mDetector = new GestureDetector(c, new CustomTapGestureListener());
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Override
    public void onPointerEvent(MotionEvent event) {
        if (mDetector.onTouchEvent(event)) {
            return;
        }
    }

    public void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotServiceConnection != null) {
                return;
            }
            ComponentName compName = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(compName);
            ServiceConnection localServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotServiceConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection serviceConnection  = this;
                        Handler handler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotServiceConnection == serviceConnection) {
                                        mContext.unbindService(mScreenshotServiceConnection);
                                        mScreenshotServiceConnection = null;
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(handler);
                        msg.arg1 = msg.arg2 = 0;

                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.w(TAG, e);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindServiceAsUser(intent, localServiceConnection, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
                mScreenshotServiceConnection = localServiceConnection;
            }
        }
    }

    class CustomTapGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean mSingleTap = false;
        
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            int screenshotEnabled = Secure.getInt(mContext.getContentResolver(), Secure.SCREENSHOT_ON_TOP_LEFT_CORNER_DOUBLE_TAP, 1);
            if(screenshotEnabled == 0) {
                mSingleTap = false;
                return true;
            }
            for (int i = 0; i < event.getPointerCount(); i++) {
                int toolType = event.getToolType(i);
                if (toolType == MotionEvent.TOOL_TYPE_FINGER_TOP_LEFT_CORNER) {
                    if(mSingleTap) {
                        mSingleTap = false;
                        android.widget.Toast.makeText(mContext, "Please double tap faster to take the screenshot", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        mSingleTap = true;
                    }
                }
                else {
                    mSingleTap = false;
                }

            }
            return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
            
            int screenshotEnabled = Secure.getInt(mContext.getContentResolver(), Secure.SCREENSHOT_ON_TOP_LEFT_CORNER_DOUBLE_TAP, 1);
            if(screenshotEnabled == 0) {
                return true;
            }
            
            for (int i = 0; i < event.getPointerCount(); i++) {
                int toolType = event.getToolType(i);
                if (toolType == MotionEvent.TOOL_TYPE_FINGER_TOP_LEFT_CORNER && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
                    
                    takeScreenshot();
                }
            }
            return true;
        }
    }
}
