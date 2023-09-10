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
import net.aquadc.delegapter.Delegapter
import net.aquadc.delegapter.VH
import net.aquadc.delegapter.adapter.DelegatedAdapter
import net.aquadc.delegapter.decor.BoundsNegotiation
import net.aquadc.delegapter.decor.decor
import net.aquadc.delegapter.diff
import net.aquadc.delegapter.equate
import net.aquadc.delegapter.equateBy
import net.aquadc.delegapter.invoke
import net.aquadc.delegapter.spanSizeLookup
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
                    spanSizeLookup = adapter.data.spanSizeLookup { _, _, delegate ->
                        if (delegate == titleDelegate) spanCount else 1
                    }

                    // Typically detach means exiting the screen.
                    // Reuse viewHolders, if shared.
                    recycleChildrenOnDetach = true
                }
                addItemDecoration(adapter.data.decor(orientation, debugSpaces = true) {
                    around({ it === titleDelegate }, size = 24) // 24dp before and after each title

                    between( // divider after title
                        { it === titleDelegate }, { it !== titleDelegate },
                        size = 1, drawable = ColorDrawable(Color.BLACK),
                        viewBoundsNegotiation = BoundsNegotiation.Outer,
                    )

                    between({ true }, size = 8) // 8dp between any two items

                    between( // divider before footer
                        { it !== titleDelegate }, { it === titleDelegate },
                        drawable = GradientDrawable().apply {
                            setColor(Color.BLACK)
                            setSize(0, max(1, resources.displayMetrics.density.roundToInt()))
                        },
                        viewBoundsNegotiation = BoundsNegotiation.Outer,
                    )
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

val titleDelegate = "title" { parent: ViewGroup ->
    VH<TextView, CharSequence>(TextView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        textSize = 18f
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER_HORIZONTAL
    }, TextView::setText)
}.diff(equate())

val iconDelegate = "icon" { parent: ViewGroup ->
    val d = GradientDrawable().apply {
        val dp = parent.resources.displayMetrics.density
        setSize((64 * dp).toInt(), (64 * dp).toInt())
    }
    VH<ImageView, GradientDrawable, Pair<Float, Int>>(ImageView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setImageDrawable(d)
    }, d) { (radius: Float, color: Int), _, _ ->
        val dp = parent.resources.displayMetrics.density
        binding.cornerRadius = radius * dp
        binding.setColor(color)
    }
}.diff(equateBy(Pair<Float, *>::first))
