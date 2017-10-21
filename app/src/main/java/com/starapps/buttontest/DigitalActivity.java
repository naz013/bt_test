package com.starapps.buttontest;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.starapps.buttontest.core.ConnectionStatus;
import com.starapps.buttontest.core.QueueItem;
import com.starapps.buttontest.core.QueueManager;
import com.starapps.buttontest.core.ReadEvent;
import com.starapps.buttontest.core.StatusRequest;
import com.starapps.buttontest.databinding.ActivityDigitalInBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;

public class DigitalActivity extends AppCompatActivity {

    private static final String TAG = "LedActivity";

    private ActivityDigitalInBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_digital_in);
        initActionBar();

        binding.clearButton.setOnClickListener(view -> binding.contentTable.removeAllViewsInLayout());

        binding.sendButton.setOnClickListener(view -> sendTestData());
    }

    private void sendTestData() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        short day = (short) calendar.get(Calendar.DAY_OF_MONTH);
        short month = (short) (calendar.get(Calendar.MONTH) + 1);
        short year = (short) calendar.get(Calendar.YEAR);
        short h = (short) calendar.get(Calendar.HOUR_OF_DAY);
        short m = (short) calendar.get(Calendar.MINUTE);
        short s = (short) calendar.get(Calendar.SECOND);
        byte[] data = new byte[14];
        data[0] = (byte) 1;
        data[1] = (byte) 12;
        pack(2, data, day);
        pack(4, data, month);
        pack(6, data, year);
        pack(8, data, h);
        pack(10, data, m);
        pack(12, data, s);
        QueueManager.getInstance().insert(new QueueItem(data));
    }

    private void pack(int stIndex, byte[] dst, short value) {
        dst[stIndex] = (byte) (value & 0xff);
        dst[stIndex + 1] = (byte) ((value >> 8) & 0xff);
    }

    private void showStatus(String value) {
        if (!value.equalsIgnoreCase("0")) {
            TextView tv = new TextView(DigitalActivity.this);
            tv.setText(value);
            binding.contentTable.addView(tv);
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle("Digital In");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().post(new StatusRequest());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        QueueManager.getInstance().clearQueue();
    }

    @Subscribe
    public void onEvent(ConnectionStatus event) {
        if (event.isConnected()) {
            binding.statusView.setText("Connected");
        } else {
            binding.statusView.setText("Connecting...");
        }
    }

    @Subscribe
    public void onEvent(ReadEvent event) {
        showStatus(event.getData());
    }
}
