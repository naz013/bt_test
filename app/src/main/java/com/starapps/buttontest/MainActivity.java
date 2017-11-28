package com.starapps.buttontest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.starapps.buttontest.connecting.DefaultConnectEvent;
import com.starapps.buttontest.connecting.DisconnectEvent;
import com.starapps.buttontest.connecting.StartActivity;
import com.starapps.buttontest.core.BluetoothService;
import com.starapps.buttontest.core.ConnectionStatus;
import com.starapps.buttontest.core.Constants;
import com.starapps.buttontest.core.QueueItem;
import com.starapps.buttontest.core.QueueManager;
import com.starapps.buttontest.core.StatusRequest;
import com.starapps.buttontest.databinding.ActivityMainBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT_AUTO = 16;
    private static final DateFormat HOUR_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private int mHour;
    private int mMinute;

    private ActivityMainBinding binding;
    private BluetoothAdapter mBtAdapter;
    private boolean isMoveNext = false;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new DefaultConnectEvent());
        initTime();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initActionBar();
        updateTimeView();

        binding.timePickerView.setIs24HourView(true);
        binding.timePickerView.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            mHour = hourOfDay;
            mMinute = minute;
            updateTimeView();
        });
        binding.sendButton.setOnClickListener(v -> sendTime());
    }

    private Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private void sendTime() {
        new Thread(() -> {
            Calendar calendar = getCalendar();
            calendar.set(Calendar.HOUR_OF_DAY, mHour);
            calendar.set(Calendar.MINUTE, mMinute);
            try {
                byte[] data = HOUR_FORMAT.format(calendar.getTime()).getBytes("UTF-8");
                byte[] withExtra = new byte[data.length + 2];
                System.arraycopy(data, 0, withExtra, 0, data.length);
                withExtra[withExtra.length - 2] = (byte) 13;
                withExtra[withExtra.length - 1] = (byte) 10;
                QueueManager.getInstance().insert(new QueueItem(withExtra));
                showToast(withExtra);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3 - 1];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            if (j < bytes.length - 1) {
                hexChars[j * 3 + 2] = '-';
            }
        }
        return new String(hexChars);
    }

    private void showToast(byte[] data) {
        Log.d(TAG, "sendTime: " + bytesToHex(data));
        mUiHandler.post(() -> Toast.makeText(MainActivity.this, "Send: " + bytesToHex(data), Toast.LENGTH_LONG).show());
    }

    private void updateTimeView() {
        Calendar calendar = getCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        binding.selectedTimeView.setText(HOUR_FORMAT.format(calendar.getTime()));
    }

    private void initTime() {
        Calendar calendar = getCalendar();
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle(R.string.app_name);
        }
    }

    private void requestBtEnabling(int requestCode) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, requestCode);
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
        EventBus.getDefault().post(new DisconnectEvent());
        EventBus.getDefault().unregister(this);
        QueueManager.getInstance().clearQueue();
        if (!isMoveNext) {
            stopService(new Intent(this, BluetoothService.class));
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT_AUTO && resultCode != RESULT_OK) {
            requestBtEnabling(REQUEST_ENABLE_BT_AUTO);
        }
    }

    private void removeBtDevice() {
        isMoveNext = true;
        EventBus.getDefault().post(new DisconnectEvent());
        removePrefs();
        startActivity(new Intent(getApplicationContext(), StartActivity.class));
        finish();
    }

    private void removePrefs() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        if (preferences.contains(Constants.DEVICE_ADDRESS)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(Constants.DEVICE_ADDRESS);
            editor.apply();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                showConfirmationDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String getDeviceName() {
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
            return device.getName();
        }
        return "";
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(getString(R.string.are_you_sure_disconnect) + " " + getDeviceName());
        builder.setPositiveButton(getString(R.string.forgot), (dialog, which) -> {
            dialog.dismiss();
            removeBtDevice();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().post(new StatusRequest());
    }

    @Subscribe
    public void onEvent(ConnectionStatus event) {
        if (event.isConnected()) {
            binding.statusView.setText("Connected");
        } else {
            binding.statusView.setText("Connecting...");
        }
    }
}
