package com.example.presentation.screens.connected_device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.presentation.databinding.FragmentParamsListBinding
import com.example.presentation.utils.newline_crlf
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ParamsListFragment : Fragment() {
    private lateinit var binding: FragmentParamsListBinding
    private val deviceArgs by navArgs<ParamsListFragmentArgs>()
    private val viewModel by viewModel<ConnectedViewModel>()
    private val adapter by lazy { ParamsAdapter() }
    private var newLine = newline_crlf
    private var pendingNewline = false
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val macAddress = deviceArgs.macAddress
        viewModel.connect(macAddress)
        viewModel.doAfterConnection()
        //viewModel.read()
        lifecycleScope.launch {
            viewModel.errorFlow.collect {
                if (it) {
                    Toast.makeText(requireContext(), "Не вдалося підключитися", Toast.LENGTH_LONG)
                        .show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentParamsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rvRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
            rvRecycler.adapter = adapter
        }
        lifecycleScope.launch {
            viewModel.isConnected.collect { isConnected ->
                binding.rvRecycler.isVisible = isConnected
                binding.connectGroup.isVisible = !isConnected
            }
        }


        adapter.onItemClick = {
            val action =
                ParamsListFragmentDirections.actionParamsListFragmentToChooseParamFragment(it)
            findNavController().navigate(action)
        }

//        lifecycleScope.launch {
//            viewModel.data.collect { data ->
//                val text = String(data, Charsets.UTF_8)
//                val lines = text.split("\n")
//                for (line in lines) {
//                    val parts = line.split(":")
//                    if (parts.size == 2) {
//                        val paramId = parts[0].trim().toIntOrNull()
//                        val paramValue = parts[1].trim().toIntOrNull()
//                        if (paramId != null && paramValue != null) {
//                            adapter.updateValueForParam(paramId, paramValue)
//                            viewModel.isDataLoadedFlow.emit(true)
//                        }
//                    }
//                }
//            }
//        }
    }

    companion object {
        const val MAX_PERCENT_OTHER = 100
        const val MAX_PERCENT_BOOST = 50
        const val MAX_PERCENT_BRAKING = 30
        const val MIN_PERCENT_OTHER = 10
        const val MIN_PERCENT_START_FREQUENCY = 5
        const val MIN_PERCENT_BOOST = 5
        const val MIN_PERCENT_BRAKING = 0

        const val ADDRESS_VOLTAGE = 1
        const val ADDRESS_FREQUENCY = 2
        const val ADDRESS_START_FREQUENCY = 3
        const val ADDRESS_BOOST = 4
        const val ADDRESS_DISPERSAL = 5
        const val ADDRESS_BRAKING_TIME = 6
        const val ADDRESS_BRAKING = 7
    }
}

