package net.aquadc.delegapter.sample

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.aquadc.delegapter.equate
import net.aquadc.delegapter.equateBy
import net.aquadc.delegapter.invoke
import net.aquadc.delegapter1.Delegapter
import net.aquadc.delegapter1.VH
import net.aquadc.delegapter1.adapter.DelegatedAdapter
import net.aquadc.delegapter1.bind
import net.aquadc.delegapter1.decor.ItemDecoration
import net.aquadc.delegapter1.decor.anyViewHolder
import net.aquadc.delegapter1.decor.ifFollowing
import net.aquadc.delegapter1.decor.ifPreceding
import net.aquadc.delegapter1.decor.item
import net.aquadc.delegapter1.decor.viewHolderIs
import net.aquadc.delegapter1.diff
import net.aquadc.delegapter1.spanSizeLookup
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SwipeRefreshLayout(this).apply {

            val adapter = object : DelegatedAdapter() {
                // container for all data items and delegates
                init { data.fill() }
                private fun Delegapter.fill() {
                    // title / header
                    add(titleDelegate, "Delegapter")

                    // items / tiles
                    repeat(Random.nextInt(24, 48)) {
                        val rad = 2.0.pow(Random.nextInt(6)).toFloat()
                        val col = 0xFFDDDDDD.toInt() and COLORS.random()
                        add(iconDelegate, rad to col)
                    }

                    // footer
                    add(titleDelegate, SpannableStringBuilder("item with spanSize=4 for symmetry 🤔").apply {
                        setSpan(
                            TypefaceSpan("monospace"), "item with ".length, "item with spanSize=4".length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    })
                }

                fun refresh() {
                    data.replace { fill() }
                }
            }

            val recycler = RecyclerView(context).apply {
                this.adapter = adapter
                val orientation = RecyclerView.VERTICAL
                layoutManager = GridLayoutManager(context, 4, orientation, false).apply {
                    spanSizeLookup = adapter.data.spanSizeLookup { _, delegate, _ ->
                        if (delegate == titleDelegate) spanCount else 1
                    }

                    // Typically detach means exiting the screen.
                    // Reuse viewHolders, if shared.
                    recycleChildrenOnDetach = true
                }
                addItemDecoration(ItemDecoration(debugOffsets = true) {
                    size(24) // 24dp before and after each title
                        .aside(viewHolderIs<TitleVH>(), Gravity.TOP)
                        .aside(viewHolderIs<TitleVH>(), Gravity.BOTTOM)

                    size(1).draw(ColorDrawable(Color.BLACK)) // divider after title
                        .aside(item { it is TitleVH }
                            .ifFollowing({ it < adapter.data.size }, { it != null }),
                            Gravity.BOTTOM)

                    size(8) // FIXME you can't use ifFollowing with ItemDecorations!
                        .aside(anyViewHolder.ifPreceding({ it >= 0 }, { it !== null }), Gravity.BOTTOM) // 8dp between any two items

                    draw(GradientDrawable().apply {
                        setColor(Color.BLACK)
                        setSize(0, max(1, resources.displayMetrics.density.roundToInt()))
                    }) // divider before footer
                        .aside(viewHolderIs<TitleVH>()
                            .ifPreceding({ it >= 0 }, { it != null }), Gravity.TOP)
                })
            }

            addView(recycler)
            setOnRefreshListener {
                adapter.refresh()
                postDelayed({
                    isRefreshing = false
                }, recycler.itemAnimator!!.run {
                    max(max(addDuration, removeDuration), max(changeDuration, moveDuration))
                })
            }
        })
    }
}

private val COLORS = intArrayOf(
    Color.GREEN,
    Color.BLUE,
    Color.CYAN,
    Color.MAGENTA,
    Color.RED,
    Color.YELLOW,
)

class TitleVH(view: TextView) : VH<TextView, Nothing?>(view, null)
val titleDelegate = "title" { parent: ViewGroup ->
    TitleVH(TextView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        textSize = 18f
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER_HORIZONTAL
    })
}.bind { item: CharSequence, _ ->
    view.setText(item)
}.diff(areItemsTheSame = equate())

val iconDelegate = "icon" { parent: ViewGroup ->
    val d = GradientDrawable().apply {
        val dp = parent.resources.displayMetrics.density
        setSize((64 * dp).toInt(), (64 * dp).toInt())
    }
    VH(ImageView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setImageDrawable(d)
    }, d)
}.bind { (radius: Float, color: Int): Pair<Float, Int>, _ ->
    val dp = view.resources.displayMetrics.density
    binding.cornerRadius = radius * dp
    binding.setColor(color)
}.diff(equateBy(Pair<Float, *>::first))
