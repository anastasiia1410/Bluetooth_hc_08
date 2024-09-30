package com.example.presentation.screens.connected_device

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.presentation.databinding.ItemParamBinding
import com.example.presentation.screens.connected_device.entity.Params

class ParamsAdapter : RecyclerView.Adapter<ParamsAdapter.VH>() {
    private val paramsList = Params.getParams()
    var onItemClick: ((param: Params) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemParamBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = paramsList.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val param = paramsList[position]
        with(holder.binding) {
            root.background = ContextCompat.getDrawable(
                root.context,
                param.cardBackground
            )
            tvName.text = tvName.context.getString(param.name)
            ivImage.setImageResource(param.icon)
//            if (param.value == null) {
//                tvValue.isVisible = false
//                progress.isVisible = true
//            } else {
//                tvValue.text = "${param.value} %"
//                progress.isVisible = false
//            }
            tvValue.text = "${param.value} %"

            root.setOnClickListener {
                onItemClick?.invoke(param)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateValueForParam(paramId: Int, newValue: Int) {
        val updatedParam = paramsList.find { it.id == paramId }
        updatedParam?.value = newValue
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemParamBinding) : RecyclerView.ViewHolder(binding.root)
}