package net.aquadc.delegapter

interface RemoveRangeMutableList<T> : MutableList<T> {
    fun removeRange(fromIndex: Int, toIndex: Int)
}

/**
 * ArrayList with public removeRange().
 * @author Mike Gorünóv
 */
class RemoveRangeArrayList<E> : ArrayList<E>, RemoveRangeMutableList<E> {
    private constructor() : super()
    internal constructor(initialCapacity: Int) : super(initialCapacity)
    internal constructor(copyFrom: Collection<E>?) : super(copyFrom)

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
    }

    internal companion object {
        fun <E> create(initialCapacity: Int): RemoveRangeArrayList<E> {
            return if (initialCapacity < 0) RemoveRangeArrayList() else RemoveRangeArrayList(initialCapacity)
        }
    }
}
