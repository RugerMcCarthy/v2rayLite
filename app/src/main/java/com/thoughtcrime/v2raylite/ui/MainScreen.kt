package com.thoughtcrime.v2raylite.ui
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.thoughtcrime.v2raylite.R
import com.thoughtcrime.v2raylite.bean.NodeState
import com.thoughtcrime.v2raylite.model.MainViewModel


@ExperimentalFoundationApi
@Composable
fun MainScreen(viewModel: MainViewModel, scaffoldState: ScaffoldState) {
    LaunchedEffect(viewModel.nextSelectedNodeIndex) {
        viewModel.changeNode()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
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
                items(viewModel.nodeStateList.size) {
                    NodeInfoBlock(viewModel, viewModel.nodeStateList.get(it))
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
        val backgroundResId = context.resources.getIdentifier("node_placeholder_${nodeState.nodeIndex}", "drawable", context.packageName)
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = "node_background",
            contentScale = ContentScale.FillBounds
        )
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
                viewModel.generateV2rayNodeConfig()
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
                text = if (!viewModel.isRunning) "启动代理" else "停止",
                fontSize = 18.sp,
                fontWeight = FontWeight.W900,
                color = Color.White,
            )
        }
    }
}

