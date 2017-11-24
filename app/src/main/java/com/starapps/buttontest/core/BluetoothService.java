package com.starapps.buttontest.core;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.starapps.buttontest.R;
import com.starapps.buttontest.connecting.ConnectEvent;
import com.starapps.buttontest.connecting.ConnectingEvent;
import com.starapps.buttontest.connecting.DefaultConnectEvent;
import com.starapps.buttontest.connecting.DisconnectEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class BluetoothService extends Service {

    final static String TAG = "BTService";
    private static final int NOTIFICATION_ID = 12563;

    private ConnectionManager mBtService = null;
    private BluetoothAdapter mBtAdapter = null;
    private static boolean isConnected;

    private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    obtainConnectionMessage(msg);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    postStatus(true);
                    break;
                case Constants.MESSAGE_READ:
                    String message = (String) msg.obj;
                    Log.d(TAG, "handleMessage: " + message);
                    EventBus.getDefault().post(new ReadEvent(message));
                    break;
            }
        }
    };

    private static void obtainConnectionMessage(Message msg) {
        switch (msg.arg1) {
            case ConnectionManager.STATE_CONNECTING:
                EventBus.getDefault().post(new ConnectingEvent(true));
            case ConnectionManager.STATE_NONE:
                postStatus(false);
                break;
            case ConnectionManager.STATE_CONNECTED: {
                postStatus(false);
                QueueManager.getInstance().deQueue();
                break;
            }
        }
    }

    public BluetoothService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Subscribe
    public void onEvent(StatusRequest request) {
        postStatus(isConnected);
    }

    @Subscribe
    public void onEvent(ConnectEvent event) {
        try {
            mBtService = new ConnectionManager(event.getDeviceData(), mHandler);
            mBtService.connect();
            QueueManager.getInstance().setManager(mBtService);
        } catch (IllegalArgumentException e) {
            Log.d("TAG", "setupConnector failed: " + e.getMessage());
        }
    }

    @Subscribe
    public void onEvent(DisconnectEvent event) {
        if (isConnected) mBtService.stop(false);
    }

    @Subscribe
    public void onEvent(DefaultConnectEvent event) {
        Log.d(TAG, "onEvent: " + mBtService);
        if (mBtService == null) {
            setupConnection();
        } else {
            if (!isConnected && mBtService.getState() == ConnectionManager.STATE_NONE) {
                mBtService.stop(false);
                mBtService.connect();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        showNotification();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        setupConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void showNotification() {
        Notification.Builder mNotificationBuilder = new Notification.Builder(getApplicationContext());
        mNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mNotificationBuilder.setContentTitle(getString(R.string.app_name));
        mNotificationBuilder.setContentText(getString(R.string.bt_service_started));
        mNotificationBuilder.setOngoing(true);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        stopConnection();
        EventBus.getDefault().unregister(this);
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    private void setupConnection() {
        try {
            String emptyName = "None";
            SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
            String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
            if (mAddress != null) {
                BluetoothDevice mConnectedDevice = mBtAdapter.getRemoteDevice(mAddress);
                DeviceData data = new DeviceData(mConnectedDevice, emptyName);
                mBtService = new ConnectionManager(data, mHandler);
                mBtService.connect();
                QueueManager.getInstance().setManager(mBtService);
            }
        } catch (IllegalArgumentException e) {
            Log.d("TAG", "setupConnector failed: " + e.getMessage());
        }
    }

    private static void postStatus(boolean status) {
        isConnected = status;
        EventBus.getDefault().post(new ConnectionStatus(status));
    }

    private void stopConnection() {
        if (mBtService != null) {
            mBtService.stop(true);
            mBtService = null;
        }
        postStatus(false);
    }
}
