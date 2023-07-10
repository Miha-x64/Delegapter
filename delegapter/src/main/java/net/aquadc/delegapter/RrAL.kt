package net.aquadc.delegapter

/**
 * ArrayList with public removeRange().
 * @author Mike Gorünóv
 */
class RrAL<E> : ArrayList<E> {
    private constructor() : super()
    private constructor(initialCapacity: Int) : super(initialCapacity)
    internal constructor(copyFrom: Collection<E>?) : super(copyFrom)

    public override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
    }

    internal companion object {
        fun <E> create(initialCapacity: Int): RrAL<E> {
            return if (initialCapacity < 0) RrAL() else RrAL(initialCapacity)
        }
    }
}
