package com.starapps.buttontest.core;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.text.TextUtils;

import java.util.ArrayList;

public class DeviceData {
    private String name = "";
    private String address = "";
    private int bondState = BluetoothDevice.BOND_NONE;
    private ArrayList<ParcelUuid> uuids = null;
    private final int deviceClass;
    private final int majorDeviceClass;

    public DeviceData(BluetoothDevice device, String emptyName) {
        name = device.getName();
        address = device.getAddress();
        bondState = device.getBondState();

        if (TextUtils.isEmpty(name)) name = emptyName;
        deviceClass = device.getBluetoothClass().getDeviceClass();
        majorDeviceClass = device.getBluetoothClass().getMajorDeviceClass();
        uuids = BluetoothUtils.getDeviceUuids(device);
    }

    public String getName() {
        return name;
    }

    public void setName(String deviceName) {
        name = deviceName;
    }

    public String getAddress() {
        return address;
    }

    public int getDeviceClass() {
        return deviceClass;
    }

    public int getMajorDeviceClass() {
        return majorDeviceClass;
    }

    public void setBondState(int state) {
        bondState = state;
    }

    public ArrayList<ParcelUuid> getUuids() {
        return uuids;
    }

    public int getBondState() {
        return bondState;
    }
}
