import com.googlecode.lanterna.graphics.Theme
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.screen.Screen
import java.util.*

class WindowAdvanced(
    theme: Theme,
    private val dataInit: GPUData,
    screen: Screen,
    private val bus: Int,
    private val driver: String,
    private val gui: MultiWindowTextGUI,
    val parent: WindowMain
): BasicWindow() {

    private val timer2 = Timer()
    val task2: () -> kotlin.Unit

    init {
        val panel3 = Panel()
        panel3.layoutManager = LinearLayout(Direction.HORIZONTAL)
        component = panel3
        val left = Panel()
        left.layoutManager = LinearLayout(Direction.VERTICAL)
        val right = Panel()
        right.layoutManager = LinearLayout(Direction.VERTICAL)
        panel3 += left
        panel3 += right

        val backBtn = Button("Back")
        left += backBtn

        fun constLabel(key: StatTypes, prefix: String? = null) {
            val x = dataInit[key]?.firstOrNull()?.toString() ?: key.toString()
            val l = Label("${prefix ?: "$key: "}$x")
            right += l
        }

        val dynamicLabels = mutableMapOf<Label, Pair<String, StatTypes>>()
        fun dynLabel(key: StatTypes, prefix: String = "$key: ") {
            val x = dataInit[key]?.firstOrNull()?.toString() ?: key.toString()
            val l = Label("$prefix$x")
            left += l
            dynamicLabels[l] = prefix to key
        }

        constLabel(StatTypes.VENDOR)
        constLabel(StatTypes.DEVICE)
        constLabel(StatTypes.SVENDOR)
        constLabel(StatTypes.SDEVICE)
        constLabel(StatTypes.CLASS)
        constLabel(StatTypes.REVISION, "Rev. ")
        constLabel(StatTypes.DRIVER)
        constLabel(StatTypes.MODULE)
        constLabel(StatTypes.BUS)

        dynLabel(StatTypes.FAN)
        dynLabel(StatTypes.EDGE)
        dynLabel(StatTypes.JUNCTION)
        dynLabel(StatTypes.MEM)
        dynLabel(StatTypes.PPT)
        left += Separator(Direction.HORIZONTAL)

        dynLabel(StatTypes.GPU)
        dynLabel(StatTypes.EVENT_ENGINE)
        dynLabel(StatTypes.VERTEX_GROUP_TESSELATOR)
        dynLabel(StatTypes.TEXTURE_ADDRESSER)
        dynLabel(StatTypes.SHADER_EXPORT)
        dynLabel(StatTypes.SEQUENCER_INST_CACHE)
        dynLabel(StatTypes.SHADER_INTERPOLATOR)
        dynLabel(StatTypes.SCAN_CONVERTER)
        dynLabel(StatTypes.PRIMITIVE_ASSEMBLY)
        dynLabel(StatTypes.DEPTH_BLOCK)
        dynLabel(StatTypes.COLOR_BLOCK)
        dynLabel(StatTypes.VRAM)
        dynLabel(StatTypes.GTT)
        dynLabel(StatTypes.MEM_CLK)
        dynLabel(StatTypes.SHADER_CLK)

        task2 = {
            val dat = sensorsInfo(driver, bus) + radeontopInfo(bus)
            for ((label, i) in dynamicLabels) {
                val (prefix, key) = i
                val x = dat[key]?.firstOrNull()?.toString() ?: "[$key]"
                label.text = "$prefix$x"
            }
        }

        timer2.scheduleAtFixedRate(object : TimerTask() { override fun run() = task2() }, 0, 200)

        setCloseWindowWithEscape(true)

        backBtn.addListener {
            gui.removeWindow(this)
            parent.unpause()
            gui.addWindowAndWait(parent)
        }
    }

    override fun close() {
        timer2.cancel()
        super.close()
    }

    fun pause() {
        timer2.cancel()
        timer2.purge()
    }

    fun unpause() {
        timer2.scheduleAtFixedRate(object: TimerTask() { override fun run() = task2() }, 0, 200)
    }

}