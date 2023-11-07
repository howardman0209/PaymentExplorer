package com.mobile.gateway.ui.view.fragment

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.mobile.gateway.R
import com.mobile.gateway.databinding.FragmentHomeBinding
import com.mobile.gateway.server.BasicServer
import com.mobile.gateway.server.iso8583.ISO8583Server
import com.mobile.gateway.server.restful.HttpServer
import com.mobile.gateway.ui.base.MVVMFragment
import com.mobile.gateway.ui.view.activity.SettingActivity
import com.mobile.gateway.ui.viewModel.HomeViewModel
import com.mobile.gateway.util.DebugPanelManager
import com.mobile.gateway.util.LongLogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address

class HomeFragment : MVVMFragment<HomeViewModel, FragmentHomeBinding>() {
    private var server: BasicServer? = null
    private lateinit var ip: String
    private val port = 8080
    private var isDebug = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate")
        ip = getWifiIpAddress(requireContext().applicationContext) ?: "127.0.0.1"
//        server = HttpServer(port).also {
//            lifecycleScope.launch(Dispatchers.IO) {
//                try {
//                    it.startServer(true)
//                } catch (ex: Exception) {
//                    DebugPanelManager.log("Exception: $ex")
//                }
//            }
//        }

        server = ISO8583Server(ip, "$port").also {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    it.startServer(true)
                } catch (ex: Exception) {
                    DebugPanelManager.log("Exception: $ex")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.tools, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("onOptionsItemSelected", "item: $item")
        when (item.itemId) {
            R.id.action_test -> {
                val test = ""
                LongLogUtil.debug("@@", "test: $test")
                DebugPanelManager.log("test: $test")
            }

            R.id.tool_logcat -> {
                isDebug = !isDebug
                DebugPanelManager.show(isDebug)
            }

            R.id.action_settings -> {
                startActivity(Intent(requireContext().applicationContext, SettingActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DebugPanelManager.log("HomeFragment - onViewCreated")
        DebugPanelManager.log("Server IP: $ip Port: $port")
        DebugPanelManager.log("-".repeat(50))
        binding.homeLabel.setOnClickListener {
            viewModel.sendRequest("http://$ip:$port", "message")
            viewModel.sendRequestWithRetrofit("http://$ip:$port/")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stopServer()
    }

    private fun getWifiIpAddress(context: Context): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val activeNetworkCap =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        if (activeNetworkCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val linkProp = connectivityManager.getLinkProperties(activeNetwork)

            linkProp?.linkAddresses?.forEach {
                if (it.address is Inet4Address) {
                    return it.address.toString().trimStart('/')
                }
            }
        }

        return null
    }

    override fun getViewModelInstance(): HomeViewModel = HomeViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_home

    override fun screenName(): String = "Home"
}