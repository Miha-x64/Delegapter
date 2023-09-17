package net.aquadc.delegapter

/**
 * [MutableList] with public [removeRange] function.
 * @author Mike Gorünóv
 */
interface RemoveRangeMutableList<T> : MutableList<T> {
    /**
     * @see [AbstractMutableList.removeRange]
     */
    fun removeRange(fromIndex: Int, toIndex: Int)
}

/**
 * [ArrayList] with public [removeRange] function.
 * @author Mike Gorünóv
 */
class RemoveRangeArrayList<E> : ArrayList<E>, RemoveRangeMutableList<E> {
    private constructor() : super()
    internal constructor(initialCapacity: Int) : super(initialCapacity)
    internal constructor(copyFrom: Collection<E>) : super(copyFrom)

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
    }

    internal companion object {
        fun <E> create(initialCapacity: Int): RemoveRangeArrayList<E> {
            return if (initialCapacity < 0) RemoveRangeArrayList() else RemoveRangeArrayList(initialCapacity)
        }
    }
}
