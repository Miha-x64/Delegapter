@file:JvmName("ItemEvents")
package net.aquadc.delegapter1

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.Named
import net.aquadc.delegapter.eatUnavailableReflectComplaint
import net.aquadc.delegapter.internal.WeakIdentityHashMap
import kotlin.reflect.typeOf


@PublishedApi internal class MakeVHClickable(
    private val clickListener: View.OnClickListener,
    name: Any?,
) : Named(name), (RecyclerView.ViewHolder) -> Unit {
    override fun invoke(vh: RecyclerView.ViewHolder) {
        require(!vh.itemView.hasOnClickListeners() && !vh.itemView.isClickable) {
            "$vh's root view ${vh.itemView} is already clickable. " +
                "Use AdapterDelegate.onClick with views which don't have " +
                "their own OnClickListener and are not #isClickable yet."
        }
        vh.itemView.setOnClickListener(clickListener)
    }
}

/**
 * Create new [ViewHolderFactory] by setting [inAdapterAtPosition] callback
 * wrapped in [View.OnClickListener] to [RecyclerView.ViewHolder.itemView].
 * Will crash if it already [has onClick listener][View.hasOnClickListeners] or [is clickable][View.isClickable].
 * The callback won't be triggered if [RecyclerView.ViewHolder.getBindingAdapterPosition] is invalid.
 */
@JvmName("onClickInAdapterAtPosition") inline fun ViewHolderFactory.onClick(
    crossinline inAdapterAtPosition: RecyclerView.ViewHolder.(RecyclerView.Adapter<*>, position: Int) -> Unit,
): ViewHolderFactory {
    val clickListener = View.OnClickListener {
        val vh = (it.parent as RecyclerView).getChildViewHolder(it)
        val pos = vh.bindingAdapterPosition
        if (pos >= 0)
            vh.inAdapterAtPosition(vh.bindingAdapter!!, pos)
    }
    return then(MakeVHClickable(clickListener, "onClick(crossinlined)"))
}

/**
 * Create new [AdapterDelegate] and [ViewHolderFactory] by setting [inAdapterAtPosition] callback
 * wrapped in [View.OnClickListener] to [RecyclerView.ViewHolder.itemView].
 * Will crash if it already [has onClick listener][View.hasOnClickListeners] or [is clickable][View.isClickable].
 * The callback won't be triggered if [RecyclerView.ViewHolder.getBindingAdapterPosition] is invalid.
 */
@JvmName("onClickInAdapterAtPosition") inline fun <T, D : Diff<T>?> AdapterDelegate<T, D>.onClick(
    crossinline inAdapterAtPosition: RecyclerView.ViewHolder.(RecyclerView.Adapter<*>, position: Int) -> Unit,
): AdapterDelegate<T, D> =
    copy(create = create.onClick(inAdapterAtPosition))


/**
 * Create new [ViewHolderFactory] by setting [atPosition] callback
 * wrapped in [View.OnClickListener] to [RecyclerView.ViewHolder.itemView].
 * Will crash if it already [has onClick listener][View.hasOnClickListeners] or [is clickable][View.isClickable].
 * The callback won't be triggered if [RecyclerView.ViewHolder.getBindingAdapterPosition] is invalid.
 */
@JvmName("onClickAtPosition") inline fun ViewHolderFactory.onClick(
    crossinline atPosition: RecyclerView.ViewHolder.(position: Int) -> Unit,
): ViewHolderFactory =
    onClick(inAdapterAtPosition = { _, pos -> atPosition(pos) })

/**
 * Create new [ViewHolderFactory] by setting [atPosition] callback
 * wrapped in [View.OnClickListener] to [RecyclerView.ViewHolder.itemView].
 * Will crash if it already [has onClick listener][View.hasOnClickListeners] or [is clickable][View.isClickable].
 * The callback won't be triggered if [RecyclerView.ViewHolder.getBindingAdapterPosition] is invalid.
 */
@JvmName("onClickAtPosition") inline fun <T, D : Diff<T>?> AdapterDelegate<T, D>.onClick(
    crossinline atPosition: RecyclerView.ViewHolder.(position: Int) -> Unit,
): AdapterDelegate<T, D> =
    copy(create = create.onClick(atPosition))


@PublishedApi internal class BoundItemSpy<T, D : Diff<T>?>(
    create: ViewHolderFactory,
    delegate: AdapterDelegate<T, D>,
    private val bound: WeakIdentityHashMap<RecyclerView.ViewHolder, T>,
    private val typeProvider: Any,
) : AdapterDelegateDecorator<T, D>(create, delegate.diff, delegate) {
    override fun bind(viewHolder: RecyclerView.ViewHolder, item: T, payloads: List<Any>) {
        super.bind(viewHolder, item, payloads)
        bound[viewHolder] = item
    }
    override fun recycled(viewHolder: RecyclerView.ViewHolder) {
        bound.remove(viewHolder)
        super.recycled(viewHolder)
    }
    override fun toString(create: ViewHolderFactory, diff: Diff<*>?): String =
        buildString {
            val ts = typeProvider.toString()
            append(super.toString(create, diff)).append(", ")
                .append("spy").append('<').append(ts, 0, ts.eatUnavailableReflectComplaint).append(">")
        }
}

/**
 * Create new [AdapterDelegate] and [ViewHolderFactory] by setting [withItem] callback
 * wrapped in [View.OnClickListener] to [RecyclerView.ViewHolder.itemView].
 * Has some extra overhead for tracking bound items.
 */
@JvmName("onClickWithItem4K") inline fun <reified T, D : Diff<in T>?> AdapterDelegate<T, D>.onClick(
    crossinline withItem: RecyclerView.ViewHolder.(item: T) -> Unit,
): AdapterDelegate<T, D> {
    val bound = WeakIdentityHashMap<RecyclerView.ViewHolder, T>()
    val clickListener = object : View.OnClickListener {
        override fun onClick(it: View) {
            val vh = (it.parent as RecyclerView).getChildViewHolder(it)
            vh.withItem(bound[vh] as T)
        }
        override fun toString(): String =
            typeOf<T>().toString() // make typeOf() invocation lazy also avoiding second anonymous class for `{ typeOf<T>() }`
    }
    return BoundItemSpy(create.then(MakeVHClickable(clickListener, "onClick(crossinlined)")), this, bound, clickListener)
}

/**
 * Create new [AdapterDelegate] and [ViewHolderFactory] by setting [withItem] callback
 * wrapped in [View.OnClickListener] to [RecyclerView.ViewHolder.itemView].
 * Has some extra overhead for tracking bound items.
 */
@JvmName("onClickWithItem") fun <T, D : Diff<in T>?> AdapterDelegate<T, D>.`onClickWithItem for Java`(
    withItem: RecyclerView.ViewHolder.(item: T) -> Unit,
): AdapterDelegate<T, D> {
    val bound = WeakIdentityHashMap<RecyclerView.ViewHolder, T>()
    val clickListener = object : View.OnClickListener {
        override fun onClick(v: View) {
            val vh = (v.parent as RecyclerView).getChildViewHolder(v)
            @Suppress("UNCHECKED_CAST")
            vh.withItem(bound[vh] as T)
        }
        override fun toString(): String =
            "onClick($withItem)"
    }
    return BoundItemSpy(create.then(MakeVHClickable(clickListener, clickListener)), this, bound, "erased T")
}
