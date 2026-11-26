package com.dswork.list.data

import java.time.LocalDate

/** 一条待办事项。minutes 表示当天的第几分钟（0..1439）。 */
data class Todo(
    val id: Long,
    val date: LocalDate,
    val minutes: Int,
    val title: String,
    val done: Boolean = false,
) {
    val timeText: String
        get() = "%02d:%02d".format(minutes / 60, minutes % 60)
}

fun LocalDate.weekdayCn(): String = "周" + "一二三四五六日"[dayOfWeek.value - 1]

fun LocalDate.dateCn(): String = "${monthValue}月${dayOfMonth}日"

/** 相对今天的称呼：今天 / 明天 / 后天 / 周X */
fun LocalDate.relativeLabel(today: LocalDate): String = when (this) {
    today -> "今天"
    today.plusDays(1) -> "明天"
    today.plusDays(2) -> "后天"
    else -> weekdayCn()
}
