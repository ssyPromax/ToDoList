package com.dswork.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dswork.list.R
import com.dswork.list.data.Todo
import com.dswork.list.data.TodoStore
import com.dswork.list.data.dateCn
import com.dswork.list.data.weekdayCn
import com.dswork.list.ui.theme.LocalListPalette
import java.time.LocalDate

/** 历史日程：所有早于今天的安排，按日期从新到旧排列（只读）。 */
@Composable
fun HistoryScreen(store: TodoStore, onBack: () -> Unit) {
    val palette = LocalListPalette.current
    val today = LocalDate.now()
    val groups = store.items
        .filter { it.date.isBefore(today) }
        .groupBy { it.date }
        .toSortedMap(compareByDescending<LocalDate> { it })
    val total = groups.values.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 6.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "返回",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "历史日程",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (total == 0) "" else "共 $total 项",
                fontSize = 12.sp,
                color = palette.textSecondary,
            )
        }

        if (groups.isEmpty()) {
            EmptyHistory()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                groups.forEach { (date, todos) ->
                    item(key = "date-${date.toEpochDay()}") {
                        HistoryDateHeader(date = date, todos = todos)
                    }
                    items(
                        items = todos.sortedWith(compareBy({ it.minutes }, { it.id })),
                        key = { "item-${it.id}" },
                    ) { todo ->
                        HistoryRow(todo)
                    }
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun HistoryDateHeader(date: LocalDate, todos: List<Todo>) {
    val palette = LocalListPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${date.dateCn()} ${date.weekdayCn()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSecondary,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .height(1.dp)
                .weight(1f)
                .background(palette.border)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${todos.size} 项",
            fontSize = 11.sp,
            color = palette.textSecondary,
        )
    }
}

@Composable
private fun HistoryRow(todo: Todo) {
    val palette = LocalListPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (todo.done) palette.accent.copy(alpha = 0.18f) else palette.border
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (todo.done) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = todo.title,
                fontSize = 15.sp,
                color = if (todo.done) palette.textSecondary else palette.textPrimary,
                textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = todo.timeText,
                fontSize = 12.sp,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    val palette = LocalListPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "还没有历史日程",
            fontSize = 15.sp,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "过去日期的安排会自动归档到这里",
            fontSize = 12.sp,
            color = palette.textSecondary.copy(alpha = 0.75f),
        )
    }
}
