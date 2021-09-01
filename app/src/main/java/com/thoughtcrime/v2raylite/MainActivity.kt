package com.thoughtcrime.v2raylite

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
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
import com.thoughtcrime.v2raylite.model.MainViewModel
import com.thoughtcrime.v2raylite.ui.MainScreen
import com.thoughtcrime.v2raylite.ui.theme.V2rayLiteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_VPN_PREPARE = 0
    }

    private val viewModel: MainViewModel by viewModels()

    @ExperimentalAnimationApi
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
                var scaffoldState = rememberScaffoldState().also {
                    viewModel.scaffoldState = it
                }
                var systemUiController = rememberSystemUiController()
                systemUiController.setStatusBarColor(Color.White, true)
                systemUiController.setNavigationBarColor(MaterialTheme.colors.primary, true)
                ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        snackbarHost = {
                            SnackbarHost(
                                it,
                                modifier = Modifier.padding(
                                    paddingValues = rememberInsetsPaddingValues(
                                        insets = LocalWindowInsets.current.navigationBars
                                    )
                                )
                            ) { data ->
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
                                    .background(Color.White)) {
                                Text(
                                    text = "v2ray青春版",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.W500,
                                    color = Color.Black,
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
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadServerList()
    }

    override fun onPause() {
        super.onPause()
        viewModel.clearDeletedNode()
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