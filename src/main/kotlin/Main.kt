import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.SimpleTheme
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
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
}

fun main(argsIn: Array<String>) {
    val args = parseArgs(argsIn)
    if ("-h" in args || "--help" in args) {
        println("Usage: gpuinfo [-m mode]")
        println("Modes:")
        println("  default - default mode")
        println("  headless - headless mode")
        println("  swing - swing mode")
        println("  awt - awt mode")
        println("  telnet - telnet mode (specify port with -port)")
        exitProcess(0)
    }
    val mode = args["-m"]?.firstOrNull() ?: "default"
    val screen = TerminalScreen(
        when (mode) {
            "default" -> DefaultTerminalFactory().createTerminal()
            "headless" -> DefaultTerminalFactory().createHeadlessTerminal()
            "swing" -> DefaultTerminalFactory().createSwingTerminal()
            "awt" -> DefaultTerminalFactory().createAWTTerminal()
            "telnet" -> DefaultTerminalFactory().also {
                it.setTelnetPort(args["-port"]?.firstOrNull()?.toIntOrNull() ?: 23)
            }.createTelnetTerminal()
            else -> throw IllegalArgumentException("Unknown mode $mode")
        }
    )

    fun exit(code: Int = 0): Nothing {
        screen.stopScreen()
        exitProcess(code)
    }

    val gui = MultiWindowTextGUI(screen)

    screen.startScreen()

    val win = WindowGPUSelect(lshwFetch(), screen.terminalSize)
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