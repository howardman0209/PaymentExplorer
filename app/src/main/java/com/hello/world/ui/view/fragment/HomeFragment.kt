package com.hello.world.ui.view.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.hello.world.R
import com.hello.world.databinding.FragmentHomeBinding
import com.hello.world.server.BasicServer
import com.hello.world.server.HttpServer
import com.hello.world.server.ISO8583Server
import com.hello.world.ui.base.MVVMFragment
import com.hello.world.ui.viewModel.HomeViewModel
import com.hello.world.util.DebugPanelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address

class HomeFragment : MVVMFragment<HomeViewModel, FragmentHomeBinding>() {
    private var server: BasicServer? = null
    private lateinit var ip: String
    private val port = 8080

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DebugPanelManager.log("HomeFragment - onViewCreated")
        DebugPanelManager.log("Server IP: $ip Port: $port")

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