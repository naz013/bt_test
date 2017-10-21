package com.starapps.buttontest.core;

import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class QueueItem {
    private byte[] data;
    private String uuId;

    public QueueItem(byte[] data) {
        this.data = data;
        Log.d("QueueItem ", "QueueItem: " + Arrays.toString(data));
        this.uuId = UUID.randomUUID().toString();
    }

    public byte[] getData() {
        return data;
    }

    public String getUuId() {
        return uuId;
    }
}