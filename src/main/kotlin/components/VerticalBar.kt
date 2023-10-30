package components

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.AbstractComponent
import com.googlecode.lanterna.gui2.ComponentRenderer
import com.googlecode.lanterna.gui2.TextGUIGraphics

class VerticalBar(
    val max: Number,
    var value: Number,
    sizeIn: TerminalSize,
): AbstractComponent<VerticalBar>() {

    init {
        size = sizeIn
    }

    override fun createDefaultRenderer(): ComponentRenderer<VerticalBar> =
        Renderer()

    private class Renderer: ComponentRenderer<VerticalBar> {
        override fun getPreferredSize(component: VerticalBar?): TerminalSize =
            component!!.size

        override fun drawComponent(g: TextGUIGraphics?, component: VerticalBar?) {
            g!!.apply {
                val max = component!!.max.toDouble()
                val value = component.value.toDouble()
                val size = component.size
                val width = size.columns
                val height = size.rows
                val step = max / height
                val y = ((value / step).toInt() - 1).coerceAtLeast(0)
                fillRectangle(
                    TerminalPosition(0, height - y),
                    TerminalSize(width, y),
                    Symbols.BLOCK_SPARSE
                )
            }
        }
    }

}