import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.Component
import com.googlecode.lanterna.gui2.Panel

fun parseArgs(args: Array<String>): Map<String, Collection<String>> {
    val map = mutableMapOf<String, Collection<String>>()
    var key: String? = null
    for (arg in args) {
        if (arg.startsWith("-")) {
            key = arg
            map[key] = emptyList()
        } else {
            if (key == null) {
                throw IllegalArgumentException("Argument $arg has no key")
            }
            map[key] = map[key]!! + arg
        }
    }
    return map
}

fun Collection<Number>.maxOrNull(): Number? {
    var max: Number? = null
    for (element in this) {
        if (max == null || element.toDouble() > max.toDouble()) {
            max = element
        }
    }
    return max
}

fun Collection<Number>.minOrNull(): Number? {
    var min: Number? = null
    for (element in this) {
        if (min == null || element.toDouble() < min.toDouble()) {
            min = element
        }
    }
    return min
}

operator fun Panel.plusAssign(component: Component) {
    addComponent(component)
}

fun Number.roundToString(digits: Int): String =
    "%.${digits}f".format(this)

operator fun TerminalSize.times(other: Number): TerminalSize =
    TerminalSize(
        (columns * other.toDouble()).toInt(),
        (rows * other.toDouble()).toInt()
    )

infix fun TerminalSize.mulRows(other: Number): TerminalSize =
    TerminalSize(
        columns,
        (rows * other.toDouble()).toInt()
    )