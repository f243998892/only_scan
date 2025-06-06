package com.example.onlyscan.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onlyscan.R

/**
 * 扫描结果列表适配器
 * 用于显示连续扫码模式下的扫描结果列表
 */
class ScanResultAdapter : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {
    
    private val scanResults = mutableListOf<String>()
    
    /**
     * 视图持有者
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultTextView: TextView = itemView.findViewById(R.id.resultTextView)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.resultTextView.text = scanResults[position]
    }
    
    override fun getItemCount(): Int = scanResults.size
    
    /**
     * 更新扫描结果列表
     */
    fun updateResults(results: List<String>) {
        scanResults.clear()
        scanResults.addAll(results)
        notifyDataSetChanged()
    }
    
    /**
     * 添加单个扫描结果
     */
    fun addResult(result: String) {
        if (!scanResults.contains(result)) {
            scanResults.add(result)
            notifyItemInserted(scanResults.size - 1)
        }
    }
    
    /**
     * 清空扫描结果
     */
    fun clearResults() {
        scanResults.clear()
        notifyDataSetChanged()
    }
} 