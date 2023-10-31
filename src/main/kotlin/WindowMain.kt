import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.screen.Screen
import components.HorizontalBar
import components.InteractableGraph
import java.util.*
import java.util.ArrayDeque

class WindowMain(
    theme: Theme,
    private val dataInit: GPUData,
    screen: Screen,
    private val bus: Int,
    private val driver: String,
    private val gui: MultiWindowTextGUI,
    exitFun: () -> Nothing
): BasicWindow() {

    private var timer = Timer()
    private val task: () -> kotlin.Unit

    init {
        val percentCProc = { it: Pair<Long, Number> ->
            "At C: ${it.second}%"
        }

        val panel = Panel()
        panel.layoutManager = GridLayout(2)
        val size = TerminalSize(
            (screen.terminalSize.columns / 2.4).toInt(),
            (screen.terminalSize.rows / 2.4).toInt()
        )

        val gpuUsageGraph = InteractableGraph(
            ArrayDeque(size.columns),
            0,
            100,
            sizeIn = size,
            currentProcessor = percentCProc
        )
        val gpuUsagePanel = Panel()
        gpuUsagePanel.layoutManager = LinearLayout(Direction.VERTICAL)
        val gpuUsageLabel = Label("0%")
        gpuUsagePanel += gpuUsageLabel
        gpuUsagePanel += gpuUsageGraph
        panel += gpuUsagePanel.withBorder(Borders.singleLine("Usage"))

        val gpuTempGraph = InteractableGraph(
            ArrayDeque(size.columns),
            0,
            100,
            sizeIn = size
        ) { "At C: ${it.second}°C" }
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
        val vramClockGraph = InteractableGraph(
            ArrayDeque(size.columns),
            0,
            100,
            sizeIn = size mulRows 0.4,
            currentProcessor = percentCProc
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
        val advancedButton = Button("Advanced")
        vramPanel += advancedButton
        panel += vramPanel

        val gpuClockLabel = Label("0mhz / 0mhz")
        val gpuClockGraph = InteractableGraph(
            ArrayDeque(size.columns),
            0,
            100,
            sizeIn = size mulRows 0.4,
            currentProcessor = percentCProc
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

        panel.theme = theme
        component = panel
        setCloseWindowWithEscape(true)

        task = {
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
        timer.scheduleAtFixedRate(object: TimerTask() { override fun run() = task() }, 0, 200)

        advancedButton.addListener {
            gui.addWindowAndWait(WindowAdvanced(theme, dataInit, screen, bus, driver, gui, this))
        }
    }

    override fun close() {
        timer.cancel()
        super.close()
    }

}