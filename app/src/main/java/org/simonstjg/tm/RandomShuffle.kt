package org.simonstjg.tm

/**
 * A random number generator which is less random
 */
class RandomShuffle(from: Int, to: Int, maxConsecutiveElements: Int) {
    private val orderedRange: List<Int> =
        (0..maxConsecutiveElements).flatMap { (from..to).toList() }
    private var shuffledRange: MutableList<Int> = mutableListOf()

    fun next(): Int {
        if (shuffledRange.isEmpty()) {
            shuffledRange = orderedRange.toMutableList()
            shuffledRange.shuffle()
        }
        return shuffledRange.removeLast()
    }
}