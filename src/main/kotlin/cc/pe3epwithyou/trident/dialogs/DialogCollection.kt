package cc.pe3epwithyou.trident.dialogs

import com.noxcrew.sheeplib.DialogContainer
import net.minecraft.client.Minecraft

/**
 * Collection that manages a collection of opened dialogs identified by unique keys.
 *
 * This object maintains a map of currently opened dialogs and provides methods to open,
 * close, refresh, and clear dialogs.
 */
object DialogCollection {
    private val openedDialogs = hashMapOf<String, TridentDialog>()
    private const val DIALOG_GAP = 5

    /**
     * Opens a dialog with the specified [key] if it is not already opened.
     *
     * @param key Unique identifier for the dialog.
     * @param dialog The [TridentDialog] instance to open.
     */
    fun open(key: String, dialog: TridentDialog) {
        if (openedDialogs.containsKey(key)) return
        DialogContainer += dialog
        val (x, y) = findPositionForNewDialog(dialog.width, dialog.height)
        dialog.x = x
        dialog.y = y
        openedDialogs.putIfAbsent(key, dialog)
    }

    /**
     * Gets a dialog with the specified [key]. Returns null if it is not opened.
     *
     * @param key Unique identifier for the dialog.
     * @return TridentDialog, or null if not found
     */
    fun get(key: String): TridentDialog? {
        if (!openedDialogs.containsKey(key)) return null
        return openedDialogs[key]
    }

    /**
     * Removes the dialog with the specified [key] from internal map.
     * Used to clean up the map when user closes the dialog from UI
     */
    fun remove(key: String) {
        openedDialogs.remove(key)
    }

    /**
     * Closes the dialog associated with the specified [key] if it is currently opened.
     *
     * @param key Unique identifier for the dialog to close.
     */
    fun close(key: String) {
        if (!openedDialogs.containsKey(key)) return

        openedDialogs[key]?.close()
        openedDialogs.remove(key)
    }

    /**
     * Refreshes the dialog layout associated with the specified [key] if it is currently opened.
     *
     * @param key Unique identifier for the dialog to refresh.
     */
    fun refreshDialog(key: String) {
        if (!openedDialogs.containsKey(key)) return
        openedDialogs[key]?.refresh()
    }

    /**
     * Closes all opened dialogs and clears the internal collection.
     */
    fun clear() {
        openedDialogs.forEach { (key, _) ->
            close(key)
        }
    }

    /**
     * Finds a position (x, y) to place a new dialog of given width and height,
     * next to existing dialogs, with the configured gap, avoiding overlaps,
     * and as close as possible to the top-left corner.
     *
     * @param newWidth Width of the new dialog to place.
     * @param newHeight Height of the new dialog to place.
     * @return Pair of (x, y) coordinates for the new dialog.
     */
    fun findPositionForNewDialog(newWidth: Int, newHeight: Int): Pair<Int, Int> {
        val existingDialogs = openedDialogs.values.map {
            Dialog(it.x, it.y, it.width, it.height)
        }

        val cornerPos = Pair(10, 10)

        if (existingDialogs.isEmpty()) return cornerPos

        val candidates = mutableSetOf<Pair<Int, Int>>()
        candidates.add(cornerPos)

        for (dialog in existingDialogs) {
            candidates.add(Pair(dialog.x + dialog.width + DIALOG_GAP, dialog.y)) // Right
            candidates.add(Pair(maxOf(cornerPos.first, dialog.x - newWidth - DIALOG_GAP), dialog.y)) // Left
            candidates.add(Pair(dialog.x, dialog.y + dialog.height + DIALOG_GAP)) // Below
            candidates.add(Pair(dialog.x, maxOf(cornerPos.second, dialog.y - newHeight - DIALOG_GAP))) // Above
            candidates.add(Pair(dialog.x + dialog.width + DIALOG_GAP, dialog.y + dialog.height + DIALOG_GAP)) // Bottom-right
            candidates.add(Pair(maxOf(cornerPos.first, dialog.x - newWidth - DIALOG_GAP), dialog.y + dialog.height + DIALOG_GAP)) // Bottom-left
            candidates.add(Pair(dialog.x + dialog.width + DIALOG_GAP, maxOf(cornerPos.second, dialog.y - newHeight - DIALOG_GAP))) // Top-right
        }

        // Filter out negative positions (should be none after clamping)
        val filteredCandidates = candidates.filter { it.first >= 0 && it.second >= 0 }

        // Sort candidates by distance to cornerPos (closest first)
        val sortedCandidates = filteredCandidates.sortedBy { candidate ->
            val dx = candidate.first - cornerPos.first
            val dy = candidate.second - cornerPos.second
            dx * dx + dy * dy // squared Euclidean distance
        }

        for ((x, y) in sortedCandidates) {
            if (!isOverlapping(existingDialogs, x, y, newWidth, newHeight)) {
                return Pair(x, y)
            }
        }

        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = Minecraft.getInstance().window.guiScaledHeight

        // Fallback grid scan
        var y = cornerPos.second
        while (y < screenHeight) {
            var x = cornerPos.first
            while (x < screenWidth) {
                if (!isOverlapping(existingDialogs, x, y, newWidth, newHeight)) {
                    return Pair(x, y)
                }
                x += DIALOG_GAP + newWidth
            }
            y += DIALOG_GAP + newHeight
        }

        return cornerPos
    }

    private fun isOverlapping(
        existingDialogs: List<Dialog>,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Boolean {
        for (dialog in existingDialogs) {
            if (rectanglesOverlap(dialog.x, dialog.y, dialog.width, dialog.height, x, y, width, height)) {
                return true
            }
        }
        return false
    }

    private fun rectanglesOverlap(
        x1: Int, y1: Int, w1: Int, h1: Int,
        x2: Int, y2: Int, w2: Int, h2: Int
    ): Boolean {
        return !(x1 + w1 <= x2 ||
                x2 + w2 <= x1 ||
                y1 + h1 <= y2 ||
                y2 + h2 <= y1)
    }

    // Internal data class to simplify overlap checks
    private data class Dialog(val x: Int, val y: Int, val width: Int, val height: Int)
}