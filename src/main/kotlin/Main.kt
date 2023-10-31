import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.SimpleTheme
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import components.HorizontalBar
import components.InteractableGraph
import java.util.*
import kotlin.math.max
import kotlin.system.exitProcess

fun TerminalSize.ensureBounds(bounds: TerminalSize): TerminalSize {
    val width = minOf(this.columns, bounds.columns)
    val height = minOf(this.rows, bounds.rows)

    return TerminalSize(width, height)
}

fun TerminalSize.center(size: TerminalSize): TerminalPosition {
    val x = (this.columns - size.columns) / 2
    val y = (this.rows - size.rows) / 2

    return TerminalPosition(x, y)
    // return TerminalPosition(columns / 2, rows / 2)
}

class GuiGPUSelect(
    gpus: Array<LshwData>,
    terminalSize: TerminalSize
): BasicWindow() {

    var selected: LshwData? = null

    private val selector: ActionListBox

    init {
        setTitle("Select GPU")

        setCloseWindowWithEscape(true)

        var maxWidth = 4 // "Exit".length
        selector = ActionListBox()
        gpus.sortBy {
            it.physid.toInt()
        }
        gpus.sortBy {
            it.isCurrentGPU()
        }
        gpus.forEach { gpu ->
            maxWidth = max(maxWidth, gpu.product.length)
            selector.addItem(gpu.product) {
                selected = gpu
                close()
            }
        }
        selector.size = TerminalSize(maxWidth, gpus.size + 1).ensureBounds(terminalSize)
        selector.addItem("Exit") { close() }
        component = selector
        position = terminalSize.center(selector.size)
    }

}

fun main(argsIn: Array<String>) {
    val args = parseArgs(argsIn)
    val screen = TerminalScreen(
        if ("components" in args)
            DefaultTerminalFactory().createSwingTerminal()
        else
            DefaultTerminalFactory().createTerminal()
    )

    fun exit(code: Int = 0): Nothing {
        screen.stopScreen()
        exitProcess(code)
    }

    val gui = MultiWindowTextGUI(screen)

    screen.startScreen()

    val win = GuiGPUSelect(lshwFetch(), screen.terminalSize)
    gui.addWindowAndWait(win)
    val gpu = win.selected
        ?: exit(130)

    val theme = SimpleTheme.makeTheme(
        true,
        TextColor.ANSI.BLACK,
        TextColor.ANSI.WHITE,
        TextColor.ANSI.GREEN,
        TextColor.ANSI.WHITE,
        TextColor.ANSI.BLUE,
        TextColor.ANSI.WHITE,
        TextColor.RGB(173, 173, 173)
    )

    val bus = gpu.getBusID()
    val dataInit = createGPUData()
    dataInit += pciInfo(bus)
    val driver = (dataInit[StatTypes.DRIVER]!!.first() as StringData).value
    dataInit += sensorsInfo(driver, bus)
    runCatching {
        dataInit += radeontopInfo(bus)
    }

    val win2 = WindowMain(
        theme,
        dataInit,
        screen,
        bus,
        driver,
        gui,
        ::exit
    )
    gui.addWindowAndWait(win2)

    exit()
}