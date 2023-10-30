import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.SimpleTheme
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import components.Graph
import components.HorizontalBar
import components.VerticalBar
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

    val bus = gpu.getBusID()
    val dataInit = createGPUData()
    dataInit += pciInfo(bus)
    val driver = (dataInit[StatTypes.DRIVER]!!.first() as StringData).value
    dataInit += sensorsInfo(driver, bus)
    dataInit += radeontopInfo(bus)

    val win2 = BasicWindow()
    val panel = Panel()
    panel.layoutManager = GridLayout(2)
    val size = TerminalSize(
        (screen.terminalSize.columns / 2.4).toInt(),
        (screen.terminalSize.rows / 2.4).toInt()
    )

    val gpuUsageGraph = Graph(
        ArrayDeque(size.columns),
        0,
        100,
        sizeIn = size
    )
    val gpuUsagePanel = Panel()
    gpuUsagePanel.layoutManager = LinearLayout(Direction.VERTICAL)
    val gpuUsageLabel = Label("0%")
    gpuUsagePanel += gpuUsageLabel
    gpuUsagePanel += gpuUsageGraph
    panel += gpuUsagePanel.withBorder(Borders.singleLine("Usage"))

    val gpuTempGraph = Graph(
        ArrayDeque(size.columns),
        0,
        100,
        sizeIn = size
    )
    (sensorsInfo(driver, bus)[StatTypes.JUNCTION]?.firstOrNull() as? TemperatureData)?.input?.let {
        gpuTempGraph.max = it
    }
    val gpuTempPanel = Panel()
    gpuTempPanel.layoutManager = LinearLayout(Direction.VERTICAL)
    val gpuTempLabel = Label("0°C / 0°C")
    gpuTempPanel += gpuTempLabel
    gpuTempPanel += gpuTempGraph
    panel += gpuTempPanel.withBorder(Borders.singleLine("Temperature"))

    val vramUsageBar = HorizontalBar(
        100,
        0,
        sizeIn = size mulRows 0.15
    )
    val vramUsagePanel = Panel()
    vramUsagePanel.layoutManager = LinearLayout(Direction.VERTICAL)
    val vramUsageLabel = Label("0mb / 0mb")
    vramUsagePanel += vramUsageLabel
    vramUsagePanel += vramUsageBar
    val vramClockGraph = Graph(
        ArrayDeque(size.columns),
        0,
        100,
        sizeIn = size mulRows 0.4
    )
    val vramClockPanel = Panel()
    vramClockPanel.layoutManager = LinearLayout(Direction.VERTICAL)
    val vramClockLabel = Label("0mhz / 0mhz")
    vramClockPanel += vramClockLabel
    vramClockPanel += vramClockGraph
    val vramPanel = Panel()
    vramPanel.layoutManager = LinearLayout(Direction.VERTICAL)
    vramPanel += vramUsagePanel.withBorder(Borders.singleLine("VRAM Usage"))
    vramPanel += vramClockPanel.withBorder(Borders.singleLine("VRAM Clock"))
    panel += vramPanel

    val gpuClockLabel = Label("0mhz / 0mhz")
    val gpuClockGraph = Graph(
        ArrayDeque(size.columns),
        0,
        100,
        sizeIn = size mulRows 0.4
    )
    val gpuClockPanel = Panel()
    gpuClockPanel.layoutManager = LinearLayout(Direction.VERTICAL)
    gpuClockPanel += gpuClockLabel
    gpuClockPanel += gpuClockGraph
    val rightBottomPanel = Panel()
    rightBottomPanel.layoutManager = LinearLayout(Direction.VERTICAL)
    rightBottomPanel += gpuClockPanel.withBorder(Borders.singleLine("GPU Clock"))
    fun addl(key: StatTypes): Label {
        val x = dataInit[key]?.firstOrNull()?.toString() ?: key.toString()
        val l = Label(x)
        rightBottomPanel += l
        return l
    }
    rightBottomPanel += Label((dataInit[StatTypes.VENDOR]?.first()?.toString() ?: "Unknown").substringAfterLast('[').substringBefore(']'))
    rightBottomPanel += Label((dataInit[StatTypes.DEVICE]?.first()?.toString() ?: "Unknown").replace('[', '\n').removeSuffix("]"))
    addl(StatTypes.SVENDOR)
    rightBottomPanel += Label("Rev. ${dataInit[StatTypes.REVISION]?.first()?.toString() ?: "Unknown"}")
    rightBottomPanel += Label("$driver / ${dataInit[StatTypes.MODULE]?.first()?.toString() ?: "Unknown"}")
    panel += rightBottomPanel

    win2.component = panel
    win2.setCloseWindowWithEscape(true)
    gui.addWindow(win2)

    val timer = Timer()
    timer.scheduleAtFixedRate(object: TimerTask() {
        override fun run() {
            val radeontop = try { radeontopInfo(bus) } catch (_: Exception) { null }
            val sensors = sensorsInfo(driver, bus)

            if (radeontop != null) {
                val usage = radeontop[StatTypes.GPU]!!.first() as PercentData
                gpuUsageGraph += usage.value
                gpuUsageLabel.text = "${usage.value.roundToString(2)}%"

                val vram = radeontop[StatTypes.VRAM]!!.first() as MemoryData
                vramUsageBar.value = vram.percent
                vramUsageLabel.text = "${vram.used.roundToString(2)}${vram.unit}B / ${vram.max.roundToString(2)}${vram.unit}B"

                val vramClock = radeontop[StatTypes.MEM_CLK]!!.first() as ClockData
                vramClockGraph += vramClock.percent
                vramClockLabel.text = "${vramClock.speed.roundToString(2)}${vramClock.unit}hz / ${vramClock.max.roundToString(2)}${vramClock.unit}hz"

                val gpuClock = radeontop[StatTypes.SHADER_CLK]!!.first() as ClockData
                gpuClockGraph += gpuClock.percent
                gpuClockLabel.text = "${gpuClock.speed.roundToString(2)}${gpuClock.unit}hz / ${gpuClock.max.roundToString(2)}${gpuClock.unit}hz"
            }

            val temp = sensors[StatTypes.JUNCTION]?.firstOrNull() as? TemperatureData
            temp?.let {
                gpuTempGraph.max = it.crit
                gpuTempGraph += it.input
            }
            gpuTempLabel.text = "${temp?.input?.roundToString(2) ?: "Unknown"}°C / ${temp?.crit?.roundToString(2) ?: "Unknown"}°C"

            gui.updateScreen()
        }
    }, 0, 200)

    gui.waitForWindowToClose(win2)

    exit()
}