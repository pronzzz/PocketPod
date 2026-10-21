package io.github.pronzzz.wheelhouse.ui.screen

import android.view.View

/**
 * A list screen. Rows are rebuilt from [build] whenever the screen comes back into view
 * after the library changed, or when [refresh] is called (e.g. after toggling a setting).
 */
open class MenuScreen(
    ui: Ui,
    private val titleText: String,
    private val emptyMessage: String = "No Items",
    private val build: () -> List<Row>,
) : Screen(ui) {

    override val title: String get() = titleText

    protected lateinit var list: MenuListView
    private var dirty = false
    private var justCreated = false

    override fun createView(): View {
        list = MenuListView(ui).also { l ->
            l.emptyMessage = emptyMessage
            l.setRows(build(), keepSelection = false)
            l.onActivate = { i -> l.rows.getOrNull(i)?.onSelect?.invoke() }
            l.onLongActivate = { i -> l.rows.getOrNull(i)?.onLong?.invoke() }
        }
        justCreated = true
        return list
    }

    fun refresh() {
        if (!::list.isInitialized) return
        list.setRows(build(), keepSelection = true)
        dirty = false
    }

    override fun onRotate(steps: Int) { list.rotate(steps) }
    override fun onSelect() { list.selectedRow()?.onSelect?.invoke() }
    override fun onSelectLong() { list.selectedRow()?.onLong?.invoke() }

    // Rebuild on every return so values (settings, "now playing" marks, counts) are never stale.
    override fun onShown() {
        if (justCreated) justCreated = false else refresh()
    }

    override fun onLibraryChanged() {
        if (::list.isInitialized && list.isAttachedToWindow && list.translationX == 0f) refresh() else dirty = true
    }

    override fun onPaletteChanged() { if (::list.isInitialized) list.paletteChanged() }
}
