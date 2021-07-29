package com.thoughtcrime.v2raylite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.thoughtcrime.v2raylite.model.MainViewModel
import com.thoughtcrime.v2raylite.ui.MainScreen
import com.thoughtcrime.v2raylite.ui.theme.Purple700
import com.thoughtcrime.v2raylite.ui.theme.V2rayLiteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @ExperimentalFoundationApi
    @ExperimentalAnimatedInsets
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
                                    text = "v2ray青春版",
                                    fontSize = 18.sp,
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
}