package com.thoughtcrime.v2raylite.ui
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.thoughtcrime.v2raylite.BuildConfig
import com.thoughtcrime.v2raylite.R
import com.thoughtcrime.v2raylite.bean.NodeState
import com.thoughtcrime.v2raylite.model.MainViewModel
import com.thoughtcrime.v2raylite.util.toast


@ExperimentalFoundationApi
@Composable
fun MainScreen(viewModel: MainViewModel, scaffoldState: ScaffoldState) {
    var context = LocalContext.current
    LaunchedEffect(viewModel.nextSelectedNodeIndex) {
        viewModel.changeProxyNode(context)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        DeleteProxyNodeDialog(viewModel)
        UrlCheckDialog(viewModel)
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier
                    .height(540.dp)
            ) {
                var newNodeStatList = viewModel.nodeStateList.filter { !it.isDeleted }
                items(newNodeStatList.size) {
                    NodeInfoBlock(viewModel, newNodeStatList.get(it))
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        paddingValues = rememberInsetsPaddingValues(
                            insets = LocalWindowInsets.current.navigationBars
                        )
                    )
                    .padding(bottom = 25.dp)
            ) {
                NodeSelectBar(viewModel)
            }
        }
    }
}

@Composable
fun NodeInfoBlock(viewModel: MainViewModel, nodeState: NodeState){
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                viewModel.nextSelectedNodeIndex = nodeState.nodeIndex
            }
    ) {
        val context = LocalContext.current
        val backgroundResId = context.resources.getIdentifier("node_placeholder_${nodeState.nodeIndex % 11}", "drawable", context.packageName)
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = "node_background",
            contentScale = ContentScale.FillBounds
        )
        IconButton(
            onClick = {
                viewModel.showDeleteProxyNodeDialog(context, nodeState.nodeIndex)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 10.dp, top = 10.dp)
                .size(15.dp)
                .drawWithContent {
                    drawCircle(color = Color.White, radius = 30f, alpha = 0.8f)
                    drawContent()
                }
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "delete_proxy_node", tint = Color.Black)
        }
        Canvas(modifier = Modifier.padding(start = 20.dp, top = 20.dp), onDraw = {
            drawCircle(
                if (nodeState.isSelected())
                    Color(0xFF008577) else Color.White,
                style = Stroke(width = 8f),
                radius = 25f
            )
            drawCircle(
                if (nodeState.isSelected())
                    Color(0xFF008577) else Color.White,
                radius = 15f
            )
        })
        Text(
            text = nodeState.nodeInfo.remark,
            fontSize = 30.sp,
            fontWeight = FontWeight.W900,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
        )
        NodeSelectStatus(nodeState)
    }
}

@Composable
fun BoxScope.NodeSelectStatus(nodeState: NodeState) {
    Box(modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(40.dp)
        .padding(top = nodeState.selectStatusBarPadding.value)
        .background(Color(0xFF5FB878))
    ) {
        if (nodeState.isSelected()) {
            Row(modifier = Modifier
                .align(Alignment.Center)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_star), contentDescription = "star", tint = Color.White)
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "已启用",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W900,
                    color = Color.White
                )
            }
        }
    }
}


@Composable
fun NodeSelectBar(viewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = {
                viewModel.showUrlCheckDialog()
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.onBackground
            ),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier
                .height(40.dp)
                .weight(0.5f)
        ) {
            Text(
                text = "更新订阅",
                fontSize = 18.sp,
                fontWeight = FontWeight.W900,
                color = Color.White,
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        Button(
            onClick = {
                viewModel.toggleVpn()
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.onBackground
            ),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier
                .height(40.dp)
                .weight(0.5f)
        ) {
            Text(
                text = if (!viewModel.isRunning) "启动" else "停止",
                fontSize = 18.sp,
                fontWeight = FontWeight.W900,
                color = Color.White,
            )
        }
    }
}


@Composable
fun DeleteProxyNodeDialog(viewModel: MainViewModel) {
    if (viewModel.deleteProxyNodeDiaglogState.isShow) {
        AlertDialog(
            onDismissRequest = {
                viewModel.hideUrlCheckDialog()
            },
            title = {
                Text(
                    text = "确定要删除这个代理节点嘛？",
                    fontWeight = FontWeight.W700,
                    style = MaterialTheme.typography.h6
                )
            },
            text = {
                Text(
                    text = "删除之后，如需再次使用需要重新导入该节点配置信息",
                    fontWeight = FontWeight.W700,
                    style = MaterialTheme.typography.h6
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProxyNode(viewModel.deleteProxyNodeDiaglogState.deleteNodeIndex)
                        viewModel.hideDeleteProxyNodeDialog()
                    },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    Text(
                        "确认",
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.button,
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.hideDeleteProxyNodeDialog()
                    },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    Text(
                        "取消",
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.button,
                        color = Color.Black
                    )
                }
            }
        )
    }
}

@Composable
fun UrlCheckDialog(viewModel: MainViewModel) {
    var context = LocalContext.current
    var inputUrlText by remember { mutableStateOf("") }
    if (viewModel.urlCheckDialogState) {
        AlertDialog(
            onDismissRequest = {
                viewModel.hideUrlCheckDialog()
            },
            title = {
                Text(
                    text = "输入订阅URL",
                    fontWeight = FontWeight.W700,
                    style = MaterialTheme.typography.h6
                )
            },
            text = {
                // Spacer(modifier = Modifier.height(30.dp))
                BasicTextField(
                    value = inputUrlText,
                    onValueChange = {
                        inputUrlText = it
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500
                    ),
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .height(40.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_url), "url", tint = Color.Black)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 5.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                innerTextField()
                            }
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputUrlText.isEmpty()) {
                            context.toast("输入URL不能为空")
                            return@TextButton
                        }
                        if (!inputUrlText.startsWith("vmess://",true)) {
                            context.toast("当前仅支持VMESS协议")
                            return@TextButton
                        }
                        var base64ConfigInfo = inputUrlText.substringAfter("vmess://", "")
                        if (base64ConfigInfo.isEmpty()) {
                            context.toast("URL格式不合法")
                            return@TextButton
                        }
                        if (!viewModel.parseURl(base64ConfigInfo)) {
                            context.toast("URL格式不合法")
                            return@TextButton
                        }
                        viewModel.hideUrlCheckDialog()
                    },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    Text(
                        "确认",
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.button,
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.hideUrlCheckDialog()
                    },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    Text(
                        "取消",
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.button,
                        color = Color.Black
                    )
                }
            }
        )
    }
}

