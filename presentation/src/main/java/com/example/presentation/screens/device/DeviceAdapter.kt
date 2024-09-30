package com.example.presentation.screens.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.presentation.databinding.ItemDeviceBinding

class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.VH>() {
    private var deviceList = mutableListOf<BluetoothDevice>()
    var onDeviceClick: ((BluetoothDevice) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val device = deviceList[position]
        with(holder.binding) {
            tvDeviceName.text = if (device.name != null) device.name else "NO NAME"
            root.setOnClickListener { onDeviceClick?.invoke(device) }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newDeviceList: List<BluetoothDevice>) {
        deviceList.clear()
        deviceList.addAll(newDeviceList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = deviceList.size

    class VH(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)
}