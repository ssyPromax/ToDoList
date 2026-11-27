package com.dswork.list.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dswork.list.R
import com.dswork.list.data.Todo
import com.dswork.list.data.TodoStore
import com.dswork.list.data.dateCn
import com.dswork.list.data.relativeLabel
import com.dswork.list.data.weekdayCn
import com.dswork.list.ui.theme.LocalListPalette
import java.time.LocalDate

@Composable
fun HomeScreen(
    store: TodoStore,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val palette = LocalListPalette.current
    val today = LocalDate.now()
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selectedDay = LocalDate.ofEpochDay(selectedEpochDay)
    val days = remember(today) { (0..6).map { today.plusDays(it.toLong()) } }

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Todo?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                dark = dark,
                onToggleTheme = onToggleTheme,
                onOpenHistory = onOpenHistory,
                onOpenAbout = { aboutOpen = true },
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(days, key = { it.toEpochDay() }) { day ->
                    DayCard(
                        day = day,
                        today = today,
                        selected = day == selectedDay,
                        todos = store.itemsFor(day),
                        onClick = { selectedEpochDay = day.toEpochDay() },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionHeader(
                day = selectedDay,
                todos = store.itemsFor(selectedDay),
                today = today,
                onAdd = {
                    editing = null
                    editorOpen = true
                },
            )

            val dayTodos = store.itemsFor(selectedDay)
            val weekUnfinished = store.items
                .filter { !it.done && it.date in days }
                .sortedWith(compareBy({ it.date }, { it.minutes }, { it.id }))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (dayTodos.isEmpty()) {
                    item(key = "selected-empty") {
                        EmptyState(isToday = selectedDay == today)
                    }
                } else {
                    items(dayTodos, key = { "selected-${it.id}" }) { todo ->
                        TodoRow(
                            todo = todo,
                            onToggle = { store.toggle(todo.id) },
                            onClick = {
                                editing = todo
                                editorOpen = true
                            },
                        )
                    }
                }

                item(key = "unfinished-header") {
                    UnfinishedHeader(count = weekUnfinished.size)
                }

                if (weekUnfinished.isEmpty()) {
                    item(key = "unfinished-empty") {
                        UnfinishedEmpty()
                    }
                } else {
                    items(weekUnfinished, key = { "unfinished-${it.id}" }) { todo ->
                        TodoRow(
                            todo = todo,
                            onToggle = { store.toggle(todo.id) },
                            onClick = {
                                editing = todo
                                editorOpen = true
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (editorOpen) {
        TodoEditorDialog(
            editing = editing,
            date = selectedDay,
            onDismiss = { editorOpen = false },
            onSave = { title, minutes ->
                val current = editing
                if (current == null) {
                    store.add(selectedDay, minutes, title)
                } else {
                    store.update(current.copy(title = title, minutes = minutes))
                }
                editorOpen = false
            },
            onDelete = editing?.let { todo ->
                {
                    store.delete(todo.id)
                    editorOpen = false
                }
            },
        )
    }

    if (aboutOpen) {
        val context = LocalContext.current
        val versionName = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "1.0"
        }
        AboutDialog(versionName = versionName, onDismiss = { aboutOpen = false })
    }
}

@Composable
private fun Header(
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val palette = LocalListPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = "List 图标",
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "List",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = palette.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleTheme) {
            Icon(
                painter = painterResource(if (dark) R.drawable.ic_sun else R.drawable.ic_moon),
                contentDescription = if (dark) "切换到浅色模式" else "切换到深色模式",
                tint = palette.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        MoreMenu(onOpenHistory = onOpenHistory, onOpenAbout = onOpenAbout)
    }
}

@Composable
private fun MoreMenu(onOpenHistory: () -> Unit, onOpenAbout: () -> Unit) {
    val palette = LocalListPalette.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "更多",
                tint = palette.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = palette.card,
        ) {
            DropdownMenuItem(
                text = {
                    Text("历史日程", fontSize = 15.sp, color = palette.textPrimary)
                },
                onClick = {
                    expanded = false
                    onOpenHistory()
                },
            )
            DropdownMenuItem(
                text = {
                    Text("关于", fontSize = 15.sp, color = palette.textPrimary)
                },
                onClick = {
                    expanded = false
                    onOpenAbout()
                },
            )
        }
    }
}

@Composable
private fun DayCard(
    day: LocalDate,
    today: LocalDate,
    selected: Boolean,
    todos: List<Todo>,
    onClick: () -> Unit,
) {
    val palette = LocalListPalette.current
    val labelColor = if (selected) palette.onSelected else palette.textPrimary
    val subColor = if (selected) palette.onSelected.copy(alpha = 0.62f) else palette.textSecondary
    val doneCount = todos.count { it.done }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) palette.selectedCard else palette.card,
        border = if (selected) null else BorderStroke(1.dp, palette.border),
        modifier = Modifier.width(96.dp).height(130.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = day.weekdayCn(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                val relative = day.relativeLabel(today)
                if (relative != day.weekdayCn()) {
                    Surface(
                        shape = CircleShape,
                        color = if (selected) {
                            palette.onSelected.copy(alpha = 0.14f)
                        } else {
                            palette.accent.copy(alpha = 0.13f)
                        },
                    ) {
                        Text(
                            text = relative,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) palette.onSelected else palette.accent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                text = day.dateCn(),
                fontSize = 10.5.sp,
                color = subColor,
                maxLines = 1,
            )
            Spacer(
                Modifier
                    .padding(vertical = 7.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (selected) palette.onSelected.copy(alpha = 0.18f) else palette.border)
            )
            if (todos.isEmpty()) {
                Text(
                    text = "暂无安排",
                    fontSize = 10.5.sp,
                    color = subColor,
                    maxLines = 1,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    todos.take(2).forEach { todo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(if (todo.done) subColor else palette.accent)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = todo.title,
                                fontSize = 10.5.sp,
                                color = subColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (todos.size > 2) {
                        Text(
                            text = "+${todos.size - 2} 项",
                            fontSize = 10.sp,
                            color = subColor,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (todos.isEmpty()) "" else "${todos.size} 项 · 完成 $doneCount",
                fontSize = 9.5.sp,
                color = subColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    day: LocalDate,
    todos: List<Todo>,
    today: LocalDate,
    onAdd: () -> Unit,
) {
    val palette = LocalListPalette.current
    val dark = palette.background.luminance() < 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = if (day == today) "今天的待办事项" else "${day.dateCn()}的待办事项",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Text(
                text = "${day.dateCn()} ${day.weekdayCn()} · 共 ${todos.size} 项，已完成 ${todos.count { it.done }} 项",
                fontSize = 11.5.sp,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = palette.accent,
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = if (day == today) "添加今日事项" else "添加 ${day.dateCn()} 的事项",
                    tint = if (dark) Color(0xFF10240F) else Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, onToggle: () -> Unit, onClick: () -> Unit) {
    val palette = LocalListPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (todo.done) {
                            Modifier.background(palette.accent)
                        } else {
                            Modifier.border(1.6.dp, palette.textSecondary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        }
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (todo.done) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "取消完成",
                        tint = if (palette.background.luminance() < 0.5f) Color(0xFF10240F) else Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    fontSize = 16.sp,
                    color = if (todo.done) palette.textSecondary else palette.textPrimary,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = todo.dateTimeText,
                    fontSize = 12.sp,
                    color = if (todo.done) palette.textSecondary else palette.accent,
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = null,
                tint = palette.textSecondary.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(isToday: Boolean) {
    val palette = LocalListPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isToday) "今天还没有安排" else "这天还没有安排",
            fontSize = 15.sp,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "点右上角绿色 + 添加一条",
            fontSize = 12.sp,
            color = palette.textSecondary.copy(alpha = 0.75f),
        )
    }
}

/** 「未完成待办事项」分区标题：汇总今天起 7 天内所有未完成事项。 */
@Composable
private fun UnfinishedHeader(count: Int) {
    val palette = LocalListPalette.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "未完成待办事项",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (count == 0) "已全部完成" else "$count 项",
                fontSize = 12.sp,
                color = palette.textSecondary,
            )
        }
        Text(
            text = "今天起 7 天内所有没做完的事项（含选中的那天）",
            fontSize = 11.5.sp,
            color = palette.textSecondary,
        )
    }
}

@Composable
private fun UnfinishedEmpty() {
    val palette = LocalListPalette.current
    Text(
        text = "这几天没有未完成的事项",
        fontSize = 13.sp,
        color = palette.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}

@Composable
private fun TodoEditorDialog(
    editing: Todo?,
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, minutes: Int) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val palette = LocalListPalette.current
    var title by remember(editing) { mutableStateOf(editing?.title ?: "") }
    var showTimePicker by remember { mutableStateOf(false) }
    var minutes by remember(editing) {
        mutableStateOf(editing?.minutes ?: run {
            val now = java.time.LocalTime.now()
            ((now.hour + (if (now.minute >= 30) 1 else 0)) % 24) * 60
        })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = if (editing == null) "添加事项" else "编辑事项",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "${date.dateCn()} ${date.weekdayCn()}",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("事项内容") },
                    placeholder = { Text("例如：买牛奶") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("时间", fontSize = 14.sp, color = palette.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.accentSoft,
                    ) {
                        Text(
                            text = "%02d:%02d".format(minutes / 60, minutes % 60),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.danger),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("删除这条事项", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), minutes) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)) {
                Text("取消")
            }
        },
    )

    if (showTimePicker) {
        ListTimePickerDialog(
            initialMinutes = minutes,
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                minutes = picked
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
) {
    val palette = LocalListPalette.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.card,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "选择时间",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                TimePicker(
                    state = state,
                    colors = androidx.compose.material3.TimePickerDefaults.colors(
                        clockDialColor = palette.background,
                        clockDialSelectedContentColor = Color.White,
                        clockDialContentColor = palette.textPrimary,
                        selectorColor = palette.accent,
                        containerColor = palette.card,
                        timeSelectorSelectedContainerColor = palette.accentSoft,
                        timeSelectorSelectedContentColor = palette.accent,
                        timeSelectorContainerColor = palette.background,
                        timeSelectorContentColor = palette.textPrimary,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary),
                    ) {
                        Text("取消")
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { onConfirm(state.hour * 60 + state.minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
