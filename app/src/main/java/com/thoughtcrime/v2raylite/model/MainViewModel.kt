package com.thoughtcrime.v2raylite.model

import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.thoughtcrime.v2raylite.bean.NodeInfo
import com.thoughtcrime.v2raylite.bean.NodeSelectStatus
import com.thoughtcrime.v2raylite.bean.NodeState

class MainViewModel: ViewModel() {
    var currentSelectedNodeIndex = 0
    private set

    var nextSelectedNodeIndex by mutableStateOf(0)

    var nodeStateList = listOf<NodeState>(
        NodeState(NodeInfo("俄罗斯"), 0).apply { select() },
        NodeState(NodeInfo("日本"), 1),
        NodeState(NodeInfo("美国"), 2)
    )

    suspend fun execChangeNodeTransitionAnim() {
        nodeStateList[currentSelectedNodeIndex].unSelect()
        nodeStateList[currentSelectedNodeIndex].selectStatusBarPadding.animateTo(
            40.dp,
            tween(250)
        )
        currentSelectedNodeIndex = nextSelectedNodeIndex
        nodeStateList[currentSelectedNodeIndex].select()
        nodeStateList[currentSelectedNodeIndex].selectStatusBarPadding.animateTo(
            0.dp,
            tween(250)
        )
    }
}