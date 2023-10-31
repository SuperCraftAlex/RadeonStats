package components

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.AbstractComponent
import com.googlecode.lanterna.gui2.ComponentRenderer
import com.googlecode.lanterna.gui2.TextGUIGraphics
import java.util.*

open class Graph(
    val data: Queue<Number>,
    var min: Number,
    var max: Number,
    var pos: Int = 0,
    sizeIn: TerminalSize
): AbstractComponent<Graph>() {

    init {
        size = sizeIn
    }

    fun step(v: Number) {
        if (data.size >= size.columns) {
            data.remove()
        }
        data += v
    }

    operator fun plusAssign(v: Number) =
        step(v)

    override fun createDefaultRenderer(): ComponentRenderer<Graph> =
        Renderer()

    private class Renderer: ComponentRenderer<Graph> {
        override fun getPreferredSize(component: Graph?): TerminalSize =
            component!!.size

        override fun drawComponent(graphics: TextGUIGraphics?, component: Graph?) { // NOT using drawLine
            graphics!!.apply {
                val data = component!!.data
                val min = component.min.toDouble()
                val max = component.max.toDouble()
                val size = component.size
                val width = size.columns
                val height = size.rows
                val step = (max - min) / height
                var x = 0
                for (yIt in data) {
                    val y = height - ((yIt.toDouble() - min) / step).toInt() - 1
                    drawLine(x, height, x, y, Symbols.BLOCK_DENSE)
                    x++
                    if (x > width - 1)
                        break
                }
            }
        }
    }

}