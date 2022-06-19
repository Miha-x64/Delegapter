package net.aquadc.delegapter.decor

import androidx.annotation.Size

/**
 * Specifies how to negotiate two different pairs of bounds.
 * @author Mike Gorünóv
 */
enum class BoundsNegotiation {

    /**
     * Tend to left and top.
     */
    Startmost {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray = startEnd1.also {
            if (startEnd2[0] < it[0]) it[0] = startEnd2[0]
            if (startEnd2[1] < it[1]) it[1] = startEnd2[1]
        }
    },

    /**
     * Tend to right and bottom.
     */
    Endmost {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray = startEnd1.also {
            if (startEnd2[0] > it[0]) it[0] = startEnd2[0]
            if (startEnd2[1] > it[1]) it[1] = startEnd2[1]
        }
    },

    /**
     * Tend to left (LTR) or right (RTL) and top.
     */
    StartmostRelative {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray =
            Startmost.negotiate(startEnd1, startEnd2)
        override fun maybeSwap(startEnd1: IntArray) {
            val tmp = startEnd1[0]
            startEnd1[0] = startEnd1[1]
            startEnd1[1] = tmp
        }
    },

    /**
     * Tend to right (LTR) or left (RTL) and bottom.
     */
    EndmostRelative {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray =
            Endmost.negotiate(startEnd1, startEnd2)
        override fun maybeSwap(startEnd1: IntArray): Unit =
            StartmostRelative.maybeSwap(startEnd1)
    },

    /**
     * Tend to center.
     */
    Inner {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray = startEnd1.also {
            if (startEnd2[0] > it[0]) it[0] = startEnd2[0]
            if (startEnd2[1] < it[1]) it[1] = startEnd2[1]
        }
    },

    /**
     * Tend to edges.
     */
    Outer {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray = startEnd1.also {
            if (startEnd2[0] < it[0]) it[0] = startEnd2[0]
            if (startEnd2[1] > it[1]) it[1] = startEnd2[1]
        }
    },

    /**
     * A mediocre implementation.
     */
    Average {
        override fun negotiate(startEnd1: IntArray, startEnd2: IntArray): IntArray = startEnd1.also {
            it[0] = (it[0] + startEnd2[0]) shr 1
            it[1] = (it[1] + startEnd2[1]) shr 1
        }
    }

    ;

    /**
     * Do the thing.
     */
    abstract fun negotiate(@Size(2) startEnd1: IntArray, @Size(2) startEnd2: IntArray): IntArray

    /**
     * Swap start with end if applicable to this mode.
     */
    open fun maybeSwap(@Size(2) startEnd1: IntArray) {}
}
