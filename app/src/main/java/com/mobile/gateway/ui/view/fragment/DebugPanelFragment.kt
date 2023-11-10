package com.mobile.gateway.ui.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import com.mobile.gateway.R
import com.mobile.gateway.databinding.FragmentDebugPanelBinding
import com.mobile.gateway.extension.requireManageFilePermission
import com.mobile.gateway.ui.base.BaseActivity
import com.mobile.gateway.ui.base.MVVMFragment
import com.mobile.gateway.ui.viewModel.DebugPanelViewModel
import com.mobile.gateway.util.DebugPanelManager
import com.mobile.gateway.util.PreferencesUtil
import com.mobile.gateway.util.ShareFileUtil


class DebugPanelFragment : MVVMFragment<DebugPanelViewModel, FragmentDebugPanelBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DebugPanelManager.messageToLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message -> // Only proceed if the event has never been handled
                printLogOnScreen(message)
            }
        }

        DebugPanelManager.logPanelHeightRatio.observe(viewLifecycleOwner) {
            it?.let { ratio ->
                (binding.logPanelContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                    matchConstraintPercentHeight = ratio
                    binding.logPanelContainer.layoutParams = this
                }
            }
        }

        viewModel.warpText.observe(viewLifecycleOwner) {
            if (!it) binding.logPanel.maxWidth = Int.MAX_VALUE else binding.logPanel.maxWidth = binding.horizontalScrollView.measuredWidth
        }

        binding.wrapBtn.setOnClickListener {
            Log.d("wrapBtn", "OnClick")
            viewModel.warpText.apply {
                postValue(this.value != true)
            }
        }

        binding.searchBtn.setOnClickListener {
            Log.d("searchBtn", "OnClick")
            showSearchView()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("onQueryTextSubmit", "query: $query")
                if (!query.isNullOrEmpty()) {
                    scrollToTarget(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("onQueryTextChange", "newText: $newText")
                if (!newText.isNullOrEmpty()) {
                    scrollToTarget(newText)
                }
                return false
            }
        })

        binding.searchView.setOnCloseListener {
            Log.d("searchView", "setOnCloseListener")
            viewModel.isSearching.set(false)
            viewModel.searchStartIndex = 0
            resetLogColor()
            false
        }

        binding.nextBtn.setOnClickListener {
            Log.d("nextBtn", "OnClick")
            scrollToTarget(binding.searchView.query.toString(), viewModel.searchStartIndex)
        }

        binding.saveBtn.setOnClickListener {
            Log.d("saveBtn", "OnClick")
            (requireActivity() as BaseActivity).requireManageFilePermission(it) {
                if (binding.logPanel.text.toString().isNotEmpty()) {
                    singleInputDialog(requireContext(), "Please input a file suffix", "Suffix") { suffix ->
                        if (suffix.isNotEmpty()) {
                            ShareFileUtil.saveLogToFile(requireContext(), binding.logPanel.text.toString(), suffix)
                        }
                    }
                }
            }
        }

//        val words = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" // for testing
//        var index = 0 // for testing
        binding.clearBtn.setOnClickListener {
            Log.d("clearBtn", "OnClick")
            binding.logPanel.text = ""
//            printLog("${"-".repeat(100)}${words[index]}") // for testing
//            if (index < words.length - 1) index++ else index = 0 // for testing
        }

        binding.copyBtn.setOnClickListener {
            Log.d("copyBtn", "OnClick")
            copyTextToClipboard(requireContext(), binding.logPanel.text.toString())
        }

        binding.logPanel.textSize = PreferencesUtil.getLogFontSize(requireContext().applicationContext)
    }

    private fun scrollToBottom() {
        binding.verticalScrollView.apply {
            binding.logPanel.clearFocus() // avoid select text to change the focus, ensure focus at the last line
            val bottom = binding.logPanel.bottom + marginBottom
            val currentY = measuredHeight + scrollY
            val alreadyAtBottom = bottom <= currentY
            Log.d("ScrollView", "already at bottom: $alreadyAtBottom")
            if (!alreadyAtBottom) {
                smoothScrollTo(0, bottom)
                Log.d("ScrollView", "scroll to bottom")
            } else {
                // already at bottom, do nothing
                Log.d("ScrollView", "do nothing")
            }
        }
    }

    private fun scrollToTarget(target: String, searchStartIndex: Int = 0) {
        Log.d("scrollToTarget", "target: $target, searchStartIndex: $searchStartIndex")
        binding.logPanel.clearFocus() // avoid select text to change the focus, ensure focus at the last line
        val fullText = binding.logPanel.text.toString()
        if (fullText.substring(searchStartIndex).contains(target, ignoreCase = true)) {
            val startIndex = fullText.indexOf(target, searchStartIndex, ignoreCase = true)
            val offset = binding.logPanel.layout.getLineForOffset(startIndex)
            Log.d("scrollToTarget", "offset: $offset")
            val yTarget = binding.logPanel.layout.getLineTop(offset)
            val xTarget = binding.logPanel.layout.getPrimaryHorizontal(startIndex)
            Log.d("scrollToTarget", "xTarget: $xTarget")

            binding.verticalScrollView.smoothScrollTo(0, yTarget) // scroll vertically
            binding.verticalScrollView.post {
                binding.horizontalScrollView.smoothScrollTo(xTarget.toInt(), yTarget) // scroll horizontally
            }

            //set search target color
            val endIndex = startIndex + target.length
            viewModel.searchStartIndex = endIndex
            Log.d("scrollToTarget", "searchStartIndex: ${viewModel.searchStartIndex}")
            if (startIndex != -1) { // coloredText is found in fullText
                val spannableString = SpannableString(fullText)
                spannableString.setSpan(
                    ForegroundColorSpan(Color.RED),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                binding.logPanel.text = spannableString
            }
        } else if (fullText.contains(target, ignoreCase = true)) {
            Log.d("scrollToTarget", "search result looped")
            scrollToTarget(target)
        } else {
            Log.d("scrollToTarget", "No record")
            viewModel.searchStartIndex = 0
            resetLogColor()
        }
    }

    private fun printLogOnScreen(logStr: String) {
        binding.logPanel.append("$logStr\n")
        Log.d("LogPanel", logStr)
        binding.logPanel.post {
            scrollToBottom()
        }
    }

    private fun resetLogColor() {
        binding.logPanel.text = binding.logPanel.text.toString() //reset text color
    }

    private fun showSearchView() {
        viewModel.isSearching.set(true)
        binding.searchView.isIconified = false
    }

    private fun updateLayout() {
        binding.logPanel.textSize = PreferencesUtil.getLogFontSize(requireContext().applicationContext)
    }

    override fun onResume() {
        super.onResume()
        Log.d("DebugPanelFragment", "onResume")
        updateLayout()
        DebugPanelManager.isForeground = true
        DebugPanelManager.logPendingMessage()
    }

    override fun onPause() {
        super.onPause()
        Log.d("DebugPanelFragment", "onPause")
        DebugPanelManager.isForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DebugPanelFragment", "onDestroy")
    }

    override fun getViewModelInstance(): DebugPanelViewModel = DebugPanelViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_debug_panel

    override fun screenName(): String = "DebugPanel"
}