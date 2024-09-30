package com.example.presentation.screens.device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presentation.R
import com.example.presentation.databinding.FragmentDeviceBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeviceFragment : Fragment() {
    private val viewModel by viewModel<DeviceViewModel>()
    private lateinit var binding: FragmentDeviceBinding
    private val deviceAdapter by lazy {
        DeviceAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.rvDevicesRecycle) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }

        lifecycleScope.launch {
            viewModel.scanResult.collect {
                with(binding) {
                    delay(2000)
                    if (it.isNotEmpty()) {
                        animation.pauseAnimation()
                        animation.isVisible = false
                        tvDevices.isVisible = true
                        rvDevicesRecycle.isVisible = true
                    }
                }
                deviceAdapter.updateItems(it)
            }
        }

        lifecycleScope.launch {
            viewModel.isScanning.collect {
                with(binding) {
                    if (it) {
                        animation.playAnimation()
                        btScan.text = getText(R.string.stop_scan)
                    } else {
                        animation.pauseAnimation()
                        btScan.text = getText(R.string.scan)
                    }
                }
            }
        }

        binding.btScan.setOnClickListener {
            if (!viewModel.isScanning.value) {
                viewModel.scan()
            } else {
                viewModel.stopScan()
            }
        }

        deviceAdapter.onDeviceClick = {
            viewModel.stopScan()
            val action =
                DeviceFragmentDirections.actionDeviceFragmentToParamsListFragment(it.address!!)
            findNavController().navigate(action)
        }
    }
}