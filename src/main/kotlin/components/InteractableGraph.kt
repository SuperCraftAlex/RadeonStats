package components

import com.googlecode.lanterna.Symbols
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.AbstractInteractableComponent
import com.googlecode.lanterna.gui2.Interactable
import com.googlecode.lanterna.gui2.InteractableRenderer
import com.googlecode.lanterna.gui2.TextGUIGraphics
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import java.util.*

class InteractableGraph(
    val data: Queue<Pair<Long, Number>>,
    val min: Number,
    var max: Number,
    val pos: Int = 0,
    sizeIn: TerminalSize,
    val currentProcessor: (Pair<Long, Number>) -> String = { it.second.toString() },
): AbstractInteractableComponent<InteractableGraph>() {

    init {
        size = sizeIn
    }

    fun step(v: Number) {
        if (data.size >= size.columns) {
            data.remove()
        }
        data += System.currentTimeMillis() to v
    }

    operator fun plusAssign(v: Number) =
        step(v)

    override fun createDefaultRenderer(): InteractableRenderer<InteractableGraph> =
        Renderer()

    @Synchronized
    public override fun handleKeyStroke(keyStroke: KeyStroke?): Interactable.Result =
        when (keyStroke?.keyType) {
            KeyType.ArrowRight -> {
                if (cursorX > size.columns - 2)
                    super.handleKeyStroke(keyStroke)
                else {
                    cursorX++
                    Interactable.Result.HANDLED
                }
            }
            KeyType.ArrowLeft -> {
                if (cursorX < 1)
                    super.handleKeyStroke(keyStroke)
                else {
                    cursorX--
                    Interactable.Result.HANDLED
                }
            }
            else -> {
                super.handleKeyStroke(keyStroke)
            }
        }

    @Synchronized
    override fun getCursorLocation(): TerminalPosition {
        return renderer.getCursorLocation(this)
    }

    var cursorX = 0

    private class Renderer: InteractableRenderer<InteractableGraph> {
        override fun getPreferredSize(component: InteractableGraph?): TerminalSize =
            component!!.size

        override fun drawComponent(graphics: TextGUIGraphics?, component: InteractableGraph?) { // NOT using drawLine
            graphics!!.apply {
                component!!

                applyThemeStyle(component.themeDefinition.normal)

                val data = component.data
                val min = component.min.toDouble()
                val max = component.max.toDouble()
                val size = component.size
                val width = size.columns
                val height = size.rows
                val step = (max - min) / height

                for (x in 0..<width) {
                    val current = data.elementAtOrNull(x + component.pos)

                    val cur = (component.cursorX == x && component.isFocused)
                    if (cur) {
                        if (current != null) {
                            putString(TerminalPosition.TOP_LEFT_CORNER, component.currentProcessor(current))
                        }
                        applyThemeStyle(component.themeDefinition.selected)
                    }

                    if (current != null) {
                        val y = height - ((current.second.toDouble() - min) / step).toInt() - 1
                        drawLine(x, height, x, y, Symbols.BLOCK_DENSE)
                    }

                    if (cur) {
                        applyThemeStyle(component.themeDefinition.normal)
                    }
                }
            }
        }

        override fun getCursorLocation(component: InteractableGraph?): TerminalPosition? {
            return if (component!!.themeDefinition.isCursorVisible) {
                TerminalPosition(component.cursorX, component.size.rows - 1)
            } else {
                null
            }
        }
    }


}