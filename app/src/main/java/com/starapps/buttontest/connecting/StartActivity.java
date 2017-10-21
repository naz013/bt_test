package com.starapps.buttontest.connecting;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.starapps.buttontest.MainActivity;
import com.starapps.buttontest.R;
import com.starapps.buttontest.core.ConnectionManager;
import com.starapps.buttontest.core.Constants;
import com.starapps.buttontest.core.DeviceData;

import java.util.Set;

public class StartActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 15;
    private static final int REQUEST_ENABLE_BT_AUTO = 16;
    private static final int REQUEST_CLICK = 105;
    private static final int REQUEST_CLICK_AUTO = 106;
    private static final int REQUEST_AUTO = 102;

    private String mDeviceAddress;
    private String mDeviceName;

    private ProgressDialog mDialog;

    private DevicesRecyclerAdapter mRecyclerAdapter;
    private ConnectionManager mBtService = null;
    private BluetoothAdapter mBtAdapter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDeviceToList(device);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initActionBar();
        initDeviceList();
        initReceiver();
        doDiscovery(REQUEST_CLICK);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle(R.string.modules);
        }
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void initDeviceList() {
        RecyclerView mDeviceList = findViewById(R.id.deviceList);
        mDeviceList.setHasFixedSize(true);
        mDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerAdapter = new DevicesRecyclerAdapter(StartActivity.this, mListener);
        mDeviceList.setAdapter(mRecyclerAdapter);
        addBoundedDevicesToList();
    }

    private void addBoundedDevicesToList() {
        if (mBtAdapter != null) {
            Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
            for (BluetoothDevice device : devices) {
                addDeviceToList(device);
            }
        }
    }

    private void doDiscovery(int code) {
        if (!checkLocationPermission(code)) {
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            requestBtEnabling(REQUEST_ENABLE_BT);
        } else {
            cancelDiscovering();
            if (code == REQUEST_CLICK_AUTO || code == REQUEST_CLICK) {
                mBtAdapter.startDiscovery();
            }
        }
    }

    private void addDeviceToList(BluetoothDevice device) {
        String name = device.getName();
        String address = device.getAddress();
        mRecyclerAdapter.addDevice(name, address);
    }

    private void saveBtDevice(String address) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.DEVICE_ADDRESS, address);
        editor.apply();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            splitByMessageType(msg);
        }
    };

    private void splitByMessageType(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                obtainConnectionMessage(msg);
                break;
        }
    }

    private void obtainConnectionMessage(Message msg) {
        switch (msg.arg1) {
            case ConnectionManager.STATE_CONNECTED:
                hideDialog();
                saveBtDevice(mDeviceAddress);
                openApplication();
                break;
            case ConnectionManager.STATE_CONNECTING:
                mDialog = ProgressDialog.show(this, getString(R.string.title_connecting),
                        mDeviceName, true, true);
                break;
            case ConnectionManager.STATE_NONE:
                break;
        }
    }

    private void hideDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private boolean checkLocationPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
                return false;
            }
            return true;
        }
        return true;
    }

    private void requestBtEnabling(int requestCode) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, requestCode);
    }

    private void cancelDiscovering() {
        if (mBtAdapter != null && mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
    }

    private void stopBtService() {
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    private final DeviceClickListener mListener = new DeviceClickListener() {
        @Override
        public void onClick(View view, int position) {
            mBtAdapter.cancelDiscovery();
            BluetoothDevice device = mBtAdapter.getRemoteDevice(mRecyclerAdapter.getDevice(position));
            mDeviceAddress = device.getAddress();
            mDeviceName = device.getName();
            setupConnector(device);
        }
    };

    private void stopConnection() {
        if (mBtService != null) {
            mBtService.stop();
            mBtService = null;
        }
    }

    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = "None";
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            mBtService = new ConnectionManager(data, mHandler);
            mBtService.connect();
        } catch (IllegalArgumentException e) {
            Log.d("TAG", "setupConnector failed: " + e.getMessage());
        }
    }

    private void openApplication() {
        startActivity(new Intent(StartActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                doDiscovery(REQUEST_CLICK);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBtAdapter.isEnabled()) {
            requestBtEnabling(REQUEST_ENABLE_BT_AUTO);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBtService();
        cancelDiscovering();
        this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUTO:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    checkLocationPermission(REQUEST_AUTO);
                }
                break;
            case REQUEST_CLICK:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery(REQUEST_CLICK);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                doDiscovery(REQUEST_CLICK_AUTO);
            } else requestBtEnabling(REQUEST_ENABLE_BT);
        }
        if (requestCode == REQUEST_ENABLE_BT_AUTO && resultCode == RESULT_OK) {
            doDiscovery(REQUEST_AUTO);
        }
    }
}
