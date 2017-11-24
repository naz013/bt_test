package com.starapps.buttontest.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ConnectionManager {

    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice connectedDevice;
    private final String deviceName;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int retryCount = 0;

    public ConnectionManager(DeviceData deviceData, Handler handler) {
        mHandler = handler;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = btAdapter.getRemoteDevice(deviceData.getAddress());
        deviceName = (deviceData.getName() == null) ? deviceData.getAddress() : deviceData.getName();
        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void connect() {
        if (D) Log.d(TAG, "connect to: " + connectedDevice);
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                if (D) Log.d(TAG, "cancel mConnectThread");
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread(connectedDevice);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket) {
        if (D) Log.d(TAG, "connected");
        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_CONNECTED);
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME, deviceName);
        mHandler.sendMessage(msg);
        retryCount = 0;
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void writeMessage(byte[] data) {
        if (D) Log.d(TAG, "writeMessage: " + mConnectedThread);
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        if (data.length == 1) r.write(data[0]);
        else r.writeData(data);
    }

    private void connectionFailed() {
        if (D) Log.d(TAG, "connectionFailed");
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
        if (retryCount < 5) {
            connect();
            retryCount++;
        }
    }

    private void connectionLost() {
        if (D) Log.d(TAG, "connectionLost: ");
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            if (D) Log.d(TAG, "create ConnectThread");
            mmDevice = device;
            mmSocket = BluetoothUtils.createRfcommSocket(mmDevice);
        }

        public void run() {
            if (D) Log.d(TAG, "ConnectThread run");
            btAdapter.cancelDiscovery();
            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to connect to device, socket isn't created");
                connectionFailed();
                return;
            }
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    if (D) Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            synchronized (ConnectionManager.this) {
                mConnectThread = null;
            }
            connected(mmSocket);
        }

        public void cancel() {
            if (D) Log.d(TAG, "ConnectThread cancel");

            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to close null socket");
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            if (D) Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (D) Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (D) Log.i(TAG, "ConnectedThread run");
            byte[] buffer = new byte[512];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String s = new String(buffer, 0, bytes);
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, s).sendToTarget();
                } catch (IOException e) {
                    if (D) Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void writeData(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                Log.d(TAG, "writeData: " + Arrays.toString(buffer));
                Thread.sleep(500);
                QueueManager.getInstance().deQueue();
            } catch (IOException e) {
                if (D) Log.e(TAG, "Exception during write", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void write(byte command) {
            byte[] buffer = new byte[1];
            buffer[0] = command;
            writeData(buffer);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
