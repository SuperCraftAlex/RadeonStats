package components

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.AbstractComponent
import com.googlecode.lanterna.gui2.ComponentRenderer
import com.googlecode.lanterna.gui2.TextGUIGraphics

class HorizontalBar(
    val max: Number,
    var value: Number,
    sizeIn: TerminalSize,
): AbstractComponent<HorizontalBar>() {

    init {
        size = sizeIn
    }

    override fun createDefaultRenderer(): ComponentRenderer<HorizontalBar> =
        Renderer()

    private class Renderer: ComponentRenderer<HorizontalBar> {
        override fun getPreferredSize(component: HorizontalBar?): TerminalSize =
            component!!.size

        override fun drawComponent(g: TextGUIGraphics?, component: HorizontalBar?) {
            g!!.apply {
                val max = component!!.max.toDouble()
                val value = component.value.toDouble()
                val size = component.size
                val width = size.columns
                val height = size.rows
                val step = max / width
                val x = ((value / step).toInt() - 1).coerceAtLeast(0)
                fillRectangle(
                    TerminalPosition.TOP_LEFT_CORNER,
                    TerminalSize(x, width),
                    Symbols.BLOCK_SPARSE
                )
            }
        }
    }

}