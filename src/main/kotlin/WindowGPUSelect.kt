import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.ActionListBox
import com.googlecode.lanterna.gui2.BasicWindow
import kotlin.math.max

class WindowGPUSelect(
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