package com.thoughtcrime.v2raylite

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewModelScope
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.thoughtcrime.v2raylite.bean.AppConfig
import com.thoughtcrime.v2raylite.model.MainViewModel
import com.thoughtcrime.v2raylite.service.V2RayServiceManager
import com.thoughtcrime.v2raylite.ui.MainScreen
import com.thoughtcrime.v2raylite.ui.theme.V2rayLiteTheme
import com.thoughtcrime.v2raylite.util.MessageUtil
import com.thoughtcrime.v2raylite.util.toast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_VPN_PREPARE = 0
    }

    private val viewModel: MainViewModel by viewModels()

    @ExperimentalFoundationApi
    @ExperimentalAnimatedInsets
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel.startListenBroadcast()
        viewModel.viewModelScope.launch {
            viewModel.vpnEventFlow.collect { toStart: Boolean ->
                if (toStart) {
                    prepareStartV2ray()
                } else {
                    viewModel.stopV2Ray(this@MainActivity)
                }
            }
        }

        setContent {
            V2rayLiteTheme(true) {
                var scaffoldState = rememberScaffoldState()
                var systemUiController = rememberSystemUiController()
                systemUiController.setStatusBarColor(MaterialTheme.colors.primary, true)
                ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        snackbarHost = {
                            SnackbarHost(it) { data ->
                                Snackbar(
                                    snackbarData = data
                                )
                            }
                        },
                        topBar = {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(MaterialTheme.colors.primary)) {
                                Text(
                                    text = "完全自主研发，打破国外垄断",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.W500,
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        },
                        modifier = Modifier.padding(
                            paddingValues = rememberInsetsPaddingValues(
                                insets = LocalWindowInsets.current.statusBars
                            )
                        )
                    ) {
                        MainScreen(viewModel, scaffoldState)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadServerList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_VPN_PREPARE ->
                if (resultCode == RESULT_OK) {
                    viewModel.startV2Ray(this)
                }
        }
    }

    private fun prepareStartV2ray() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            viewModel.startV2Ray(this)
        } else {
            startActivityForResult(intent, MainActivity.REQUEST_CODE_VPN_PREPARE)
        }
    }
}