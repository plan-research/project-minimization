package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.settings.StateDelegate

import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty

import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel

typealias PanelWithList<V> = Pair<JPanel, DefaultListModel<V>>

fun <T : JComponent, V> Cell<T>.bindList(
    listModel: DefaultListModel<V>,
    delegate: StateDelegate<List<V>>,
    onChange: () -> Unit = {},
): Cell<T> =
    bind(
        componentGet = { listModel.toList() },
        componentSet = { _, list ->
            listModel.clear()
            listModel.addAll(list)
            onChange()
        },
        MutableProperty(delegate::get, delegate::set),
    )

fun <V> DefaultListModel<V>.toList() = let { listModel ->
    buildList {
        for (i in 0 until listModel.size) {
            add(listModel.getElementAt(i))
        }
    }
}
