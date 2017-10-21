package com.starapps.buttontest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.starapps.buttontest.connecting.StartActivity;
import com.starapps.buttontest.core.BluetoothService;
import com.starapps.buttontest.core.Constants;
import com.starapps.buttontest.core.QueueManager;
import com.starapps.buttontest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT_AUTO = 16;

    private ActivityMainBinding binding;
    private BluetoothAdapter mBtAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initActionBar();
        startService(new Intent(this, BluetoothService.class));

        binding.digitalIn.setOnClickListener(view -> startActivity(new Intent(this, DigitalActivity.class)));
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
        stopService(new Intent(this, BluetoothService.class));
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
}
