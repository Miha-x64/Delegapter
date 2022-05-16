package net.aquadc.delegapter.sample

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.delegapter.Delegapter
import net.aquadc.delegapter.VH
import net.aquadc.delegapter.adapter.VHAdapter
import net.aquadc.delegapter.decor
import kotlin.math.pow
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(RecyclerView(this).apply {
            val adapter = object : VHAdapter<VH<*, *, *>>() {

                // container for all data items and delegates
                val d = Delegapter(this).apply {
                    // title / header
                    add("Delegapter", titleDelegate)

                    // items / tiles
                    val dp = resources.displayMetrics.density
                    repeat(Random.nextInt(24, 48)) {
                        add(GradientDrawable().apply {
                            cornerRadius = 2.0.pow(Random.nextInt(6)).toFloat() * dp
                            setColor(0xFFDDDDDD.toInt() and COLORS.random())
                            setSize((64 * dp).toInt(), (64 * dp).toInt())
                        }, iconDelegate)
                    }

                    // footer
                    add(SpannableStringBuilder("item with spanSize=4 for symmetry 🤔").apply { setSpan(
                        TypefaceSpan("monospace"), "item with ".length, "item with spanSize=4".length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    ) }, titleDelegate)
                }

                override fun getItemCount(): Int =
                    d.size
                override fun getItemViewType(position: Int): Int =
                    d.viewTypeAt(position)
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, *> =
                    d.createViewHolder(parent, viewType)
                override fun onBindViewHolder(holder: VH<*, *, *>, position: Int, payloads: List<Any>) =
                    d.bindViewHolder(holder, position, payloads)
            }.also { adapter = it }
            val orientation = RecyclerView.VERTICAL
            layoutManager = GridLayoutManager(context, 4, orientation, false).also { it.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    if (position == 0 || position == adapter.itemCount - 1) it.spanCount else 1
            } }
            addItemDecoration(adapter.d.decor(orientation, debugSpaces = true) {
                around({ it == titleDelegate }, spaceSize = 24f) // 24dp before and after each title
                between({ true }, spaceSize = 8f) // 8dp between any two items
            })
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

val titleDelegate = ::titleItem
fun titleItem(parent: ViewGroup): VH<TextView, Nothing?, CharSequence> =
    VH(TextView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        textSize = 18f
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER_HORIZONTAL
    }, TextView::setText)

val iconDelegate = ::iconItem
fun iconItem(parent: ViewGroup): VH<ImageView, Nothing?, Drawable> =
    VH(ImageView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }, ImageView::setImageDrawable)
