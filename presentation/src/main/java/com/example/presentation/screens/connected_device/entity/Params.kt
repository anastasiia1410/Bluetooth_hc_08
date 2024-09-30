package com.example.presentation.screens.connected_device.entity

import android.os.Parcelable
import com.example.presentation.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Params(
    val id: Int,
    val cardBackground: Int,
    val iconBack : Int,
    val name: Int,
    val icon: Int,
    var value: Int?,
) : Parcelable {
    companion object {
        fun getParams(): MutableList<Params> {
            val paramsList = mutableListOf<Params>()
            paramsList.add(
                Params(
                    1,
                    R.drawable.shape_voltage,
                    R.drawable.shape_for_icon_voltage,
                    R.string.maximum_voltage,
                    R.drawable.ic_bolt,
                    10
                )
            )
            paramsList.add(
                Params(
                    2,
                    R.drawable.shape_frequency,
                    R.drawable.shape_for_icon_frequency,
                    R.string.frequency_maximum,
                    R.drawable.ic_waves,
                    10
                )
            )
            paramsList.add(
                Params(
                    3,
                    R.drawable.shape_start_frequency,
                    R.drawable.shape_for_start_frequency,
                    R.string.start_frequency,
                    R.drawable.ic_speed,
                    5
                )
            )
            paramsList.add(
                Params(
                    4,
                    R.drawable.shape_boost,
                    R.drawable.shape_for_boost_icon,
                    R.string.boost,
                    R.drawable.ic_rocket,
                    5
                )
            )

            paramsList.add(
                Params(
                    5,
                    R.drawable.shape_dispersal,
                    R.drawable.shape_for_dispersal_icon,
                    R.string.time_of_dispersal,
                    R.drawable.ic_dispersal,
                    10
                )
            )
            paramsList.add(
                Params(
                    6,
                    R.drawable.shape_braking_time,
                    R.drawable.shape_for_icon_braking_time,
                    R.string.braking_time,
                    R.drawable.ic_braiking_time,
                    10
                )
            )
            paramsList.add(
                Params(
                    7,
                    R.drawable.shape_braking,
                    R.drawable.shape_for_icon_braking,
                    R.string.braking,
                    R.drawable.ic_braking,
                    0
                )
            )
            return paramsList
        }
    }
}
