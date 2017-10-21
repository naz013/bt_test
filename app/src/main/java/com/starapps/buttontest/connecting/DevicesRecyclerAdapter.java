package com.starapps.buttontest.connecting;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.starapps.buttontest.R;

import java.util.ArrayList;
import java.util.List;

class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.DeviceViewHolder> {

    private final Context mContext;
    private final List<String> mDataList;
    private final List<String> mDataAddresses;
    private final DeviceClickListener mListener;

    DevicesRecyclerAdapter(Context context, DeviceClickListener listener) {
        this.mContext = context;
        this.mDataList = new ArrayList<>();
        this.mDataAddresses = new ArrayList<>();
        this.mListener = listener;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.device_list_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.deviceName.setText(mDataList.get(position));
        holder.deviceAddress.setText(mDataAddresses.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    String getDevice(int position) {
        return mDataAddresses.get(position);
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder{

        final TextView deviceName;
        final TextView deviceAddress;

        DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.deviceName);
            deviceAddress = (TextView) itemView.findViewById(R.id.deviceAddress);
            itemView.findViewById(R.id.containerItem).setOnClickListener(v -> handleClick(getAdapterPosition()));
        }
    }

    void addDevice(String name, String address) {
        if (mDataList.size() > 0) {
            String noDevices = mContext.getString(R.string.none_found);
            if (mDataList.contains(noDevices)) {
                mDataList.remove(noDevices);
            }
            if (mDataAddresses.contains(address)) {
                return;
            }
        }
        mDataList.add(name);
        mDataAddresses.add(address);
        int pos = mDataAddresses.indexOf(address);
        notifyItemInserted(pos);
    }

    private void handleClick(int adapterPosition) {
        if (mListener != null) {
            mListener.onClick(null, adapterPosition);
        }
    }
}
