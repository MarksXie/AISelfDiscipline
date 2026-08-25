package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.data.model.ApprovalRecord
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen() {
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()
    val historyRecords by repository.historyRecords.collectAsState(initial = emptyList())

    val totalCount = historyRecords.size
    val approvedCount = historyRecords.count { it.approved }
    val rejectedCount = totalCount - approvedCount
    val passRate = if (totalCount > 0) (approvedCount * 100 / totalCount) else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E17))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 统计面板
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "拦截与审批周报统计",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatColumn(title = "拦截总次数", value = "$totalCount", color = Color(0xFF00E5FF))
                        StatColumn(title = "通过放行", value = "$approvedCount", color = Color(0xFF00E676))
                        StatColumn(title = "驳回拦截", value = "$rejectedCount", color = Color(0xFFFF5252))
                        StatColumn(title = "自律过审率", value = "$passRate%", color = Color(0xFFB388FF))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "审批记录明细",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90CAF9),
                    modifier = Modifier.padding(start = 4.dp)
                )

                if (historyRecords.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                repository.clearHistory()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "清空记录",
                            tint = Color(0xFF90A4AE),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "清空历史",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }
            }
        }

        if (historyRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无审批记录，开启保护后记录将在此展示",
                        fontSize = 13.sp,
                        color = Color(0xFF546E7A)
                    )
                }
            }
        } else {
            items(historyRecords, key = { it.id }) { record ->
                HistoryItemCard(record = record)
            }
        }
    }
}

@Composable
private fun StatColumn(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF90A4AE)
        )
    }
}

@Composable
private fun HistoryItemCard(record: ApprovalRecord) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (record.approved) Color(0x2200E676) else Color(0x22FF5252),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (record.approved) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                            contentDescription = null,
                            tint = if (record.approved) Color(0xFF00E676) else Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = Color(0xFF78909C)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (record.reason.isNotBlank()) {
                val isMultiTurn = record.reason.contains("➔")
                if (isMultiTurn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F121E), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "多轮自律问答纪要",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = record.reason,
                                fontSize = 11.sp,
                                color = Color(0xFFCFD8DC),
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "申请理由：${record.reason}",
                        fontSize = 12.sp,
                        color = Color(0xFFECEFF1),
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = "AI 评语：${record.comment}",
                fontSize = 12.sp,
                color = if (record.approved) Color(0xFF81C784) else Color(0xFFEF9A9A),
                lineHeight = 16.sp
            )

            if (record.approved && record.allowedMinutes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "放行时长：${record.allowedMinutes} 分钟",
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}
