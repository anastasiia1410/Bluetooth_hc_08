package com.example.presentation.screens.connected_device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.presentation.R
import com.example.presentation.databinding.FragmentChooseParamBinding
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_BOOST
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_BRAKING
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_BRAKING_TIME
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_DISPERSAL
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_FREQUENCY
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_START_FREQUENCY
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.ADDRESS_VOLTAGE
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MAX_PERCENT_BOOST
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MAX_PERCENT_BRAKING
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MAX_PERCENT_OTHER
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MIN_PERCENT_BOOST
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MIN_PERCENT_BRAKING
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MIN_PERCENT_OTHER
import com.example.presentation.screens.connected_device.ParamsListFragment.Companion.MIN_PERCENT_START_FREQUENCY
import com.example.presentation.screens.connected_device.entity.Params
import com.example.presentation.utils.inputText
import com.example.presentation.utils.setDividerColor
import com.example.presentation.utils.toEditText
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChooseParamFragment : Fragment() {
    private lateinit var binding: FragmentChooseParamBinding
    private val paramArgs by navArgs<ChooseParamFragmentArgs>()
    private val viewModel by viewModel<ConnectedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentChooseParamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val param = paramArgs.param
        lifecycleScope.launch {
            viewModel.valueFlow.emit(param.value!!)
        }
        setViews(param)
        checkEnabledButton(param)
        clickListeners(param)
        checkBoxingClick()

    }

    private fun setViews(param: Params) {
        with(binding) {
            ivIcon.setImageResource(param.icon)
            divider.dividerColor = ContextCompat.getColor(requireContext(), setDividerColor(param))
            shape.setImageResource(param.iconBack)
            tvName.text = getText(param.name)
            ivArrowBack.setOnClickListener {
                cbEndlessSending.isChecked = false
                findNavController().popBackStack()
            }
            tvCurrentValue.text = "${param.value}%"
            etSetValue.text = param.value.toString().toEditText
        }
    }

    private fun checkEnabledButton(param: Params) {
        with(binding) {
            lifecycleScope.launch {
                viewModel.valueFlow.collect { value ->
                    when (param.name) {
                        R.string.maximum_voltage,
                        R.string.frequency_maximum,
                        R.string.time_of_dispersal,
                        R.string.braking_time,
                        -> {
                            ivMinus.isEnabled = value > MIN_PERCENT_OTHER
                            ivPlus.isEnabled = value < MAX_PERCENT_OTHER
                        }

                        R.string.start_frequency -> {
                            ivMinus.isEnabled = value > MIN_PERCENT_START_FREQUENCY
                            ivPlus.isEnabled = value < MAX_PERCENT_OTHER
                        }

                        R.string.boost -> {
                            ivMinus.isEnabled = value > MIN_PERCENT_BOOST
                            ivPlus.isEnabled = value < MAX_PERCENT_BOOST
                        }

                        R.string.braking -> {
                            ivMinus.isEnabled = value > MIN_PERCENT_BRAKING
                            ivPlus.isEnabled = value < MAX_PERCENT_BRAKING
                        }
                    }
                }
            }
        }
    }

    private fun clickListeners(param: Params) {
        with(binding) {
            ivPlus.setOnClickListener {
                if (param.id == ADDRESS_VOLTAGE) {
                    add(param.value!!, ADDRESS_VOLTAGE)
                }
                if (param.id == ADDRESS_FREQUENCY) {
                    add(param.value!!, ADDRESS_FREQUENCY)
                }
                if (param.id == ADDRESS_START_FREQUENCY) {
                    add(param.value!!, ADDRESS_START_FREQUENCY)
                }
                if (param.id == ADDRESS_BOOST) {
                    add(param.value!!, ADDRESS_BOOST)
                }
                if (param.id == ADDRESS_DISPERSAL) {
                    add(param.value!!, ADDRESS_DISPERSAL)
                }
                if (param.id == ADDRESS_BRAKING_TIME) {
                    add(param.value!!, ADDRESS_BRAKING_TIME)
                }
                if (param.id == ADDRESS_BRAKING) {
                    add(param.value!!, ADDRESS_BRAKING)
                }
            }

            ivMinus.setOnClickListener {
                if (param.id == ADDRESS_VOLTAGE) {
                    remove(param.value!!, ADDRESS_VOLTAGE)
                }
                if (param.id == ADDRESS_FREQUENCY) {
                    remove(param.value!!, ADDRESS_FREQUENCY)
                }
                if (param.id == ADDRESS_START_FREQUENCY) {
                    remove(param.value!!, ADDRESS_START_FREQUENCY)
                }
                if (param.id == ADDRESS_BOOST) {
                    remove(param.value!!, ADDRESS_BOOST)
                }
                if (param.id == ADDRESS_DISPERSAL) {
                    remove(param.value!!, ADDRESS_DISPERSAL)
                }
                if (param.id == ADDRESS_BRAKING_TIME) {
                    remove(param.value!!, ADDRESS_BRAKING_TIME)
                }
                if (param.id == ADDRESS_BRAKING) {
                    remove(param.value!!, ADDRESS_BRAKING)
                }
            }
        }
    }


    private fun add(value: Int, address: Int) {
        var currentValue = value
        lifecycleScope.launch {
            viewModel.valueFlow.collect {
                currentValue = it
                binding.etSetValue.text = it.toString().toEditText
            }
        }
        viewModel.add(currentValue, address)

    }


    private fun remove(value: Int, address: Int) {
        var currentValue = value
        lifecycleScope.launch {
            viewModel.valueFlow.collect {
                currentValue = it
                binding.etSetValue.text = it.toString().toEditText
            }
        }
        viewModel.remove(currentValue, address)
    }

    private fun checkBoxingClick() {
        with(binding) {
            cbEndlessSending.setOnClickListener {
                if (cbEndlessSending.isChecked) {
                    lifecycleScope.launch {
                        viewModel.sendingEnabledFlow.emit(true)
                        val address = 1
                        val time = sTimeTick.value.toLong()
                        viewModel.sendNonStop(
                            address.toByte(),
                            binding.etSetValue.inputText().toByte(),
                            time
                        )
                    }
                } else {
                    lifecycleScope.launch {
                        viewModel.sendingEnabledFlow.emit(false)
                    }
                }
            }
        }
    }
}