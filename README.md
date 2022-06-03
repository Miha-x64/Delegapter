
Yet another adapter delegate library.

```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

...

dependencies {
    implementation("com.github.Miha-x64:Delegapter:6c6574a8")
}

```

### Why?

The idea of registering a delegate for a certain item type is flawed:
* one could forget to register a delegate (runtime crash),
* or to unregister a useless one (dead code);
* having items of the same type with different `viewTypes` and `ViewHolder`s is impossible.

The concept of this library is to make everything clear and explicit. No binding a delegate to certain item type, no fallback delegates.

### ViewHolder

We use our own ViewHolder class (called just `VH`) for a bunch of reasons:
* `RecyclerView.ViewHolder` is abstract, but it's sometimes necessary to create a “dumb” holder without any special fields or behaviour, thus `VH` is `open`
* There's `RecyclerView.ViewHolder.itemView: View`, but `VH` is generic, and has a property `VH<V, …>.view: V`
* When using viewBinding, all `ViewHolder`s look the same: they have `binding` field. `VH` supports an attachment of any type which is typically `ViewBinding`: `VH<*, B, …>.binding: B`
* Delegapter needs to tie certain `ViewHolder` type with the corresponding data type for type safety: `VH<V : View, B, D>`
* Therefore, `VH<*, *, D>` has its own `bind(D)` method which is a common practice (but not forced by the library)

There's a lot of factory functions for creating ViewHolders:
```kotlin
VH(TextView(parent.context).apply {
    layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    fontRes = R.font.roboto
    textSize = 17f
}, TextView::setText) // VH<TextView, Nothing?, CharSequence>

inflateVH(parent, ItemUserBinding::inflate) { user: User ->
    imageLoader.load(user.photo).into(photoView)
    nameView.text = user.name
} // VH<View, ItemUserBinding, User>

// and more…
```

### Delegate

Delegate is just a ViewHolder factory:
```kotlin
typealias Delegate<D> = (parent: ViewGroup) -> VH<*, *, D>
```
`VH::V` and `VH::B` are actually implementation details of a certain `VH`, Delegapter does not need them after instantiation, thus `<*, *`.

A typical Delegate declaration looks like this:
```kotlin
val userDelegate = ::userHolder
private fun userHolder(parent: ViewGroup): Delegate<User> =
    inflateVH(…) { … }
```
In this example, `userDelegate` property guarantees object identity (`::userHolder` expression could give out different instances between invocations). You can just write `val userDelegate = { parent -> … }`, of course, but method reference, unlike lambda, gives meaningful `toString()` and helps debugging.

### Delegapter

Delegapter is basically a list of (item, delegate) tuples, but their type agreement is guaranteed, like it was a `List<<D> Pair<D, Delegate<D>>` (non-denotable type in Java/Kotlin). 

Delegapter is not an `Adapter` itself, just a special data structure. Let's use base `VHAdapter` for convenience, it already delegates `onBindViewHolder` to `VH` for you:

```kotlin
class SomeAdapter : VHAdapter<VH<*, *, *>>() {

    init { stateRestorationPolicy = … }

    private val d = Delegapter(this /* pass self to get notified */)

    override fun getItemCount(): Int =
        d.size

    override fun getItemViewType(position: Int): Int =
        d.viewTypeAt(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH<*, *, *> =
        d.createViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: VH<*, *, *>, position: Int, payloads: List<Any>): Unit =
        d.bindViewHolder(holder, position, payloads)

    fun update(data: Data) {
        d.clear()
        d.add(data.header, headerDelegate)
        d.addAll(data.recommended, recommendationDelegate)
        d.addAll(data.posts, postDelegate)
        // use autocomplete to see all available functions
    }

}
```

This gives you some flexibility for advanced usage scenarios:
* Insert items not handled by Delegapter (headers, footers, ads 🤮).
  (Instead of passing `this` to the constructor, use custom `ListUpdateCallback` implementation to correct `notify*()` calls.)
* Filter out some items without removing them.
  (This requires a corrected `ListUpdateCallback`, too.)
* Use several Delegapters in a single Adapter (IDK why but this should happen at some point).

In order to share `RecycledViewPool` between several `RecyclerView`s, you need to preserve the same `viewType` to `Delegate` mapping across adapters. This can be achieved using “parent” `Delegapter`:

```kotlin
val delegapterFather = Delegapter(NullListUpdateCallback)

…

class SomeAdapter : RecyclerView.Adapter<…>() {
    private val d = Delegapter(this, delegapterFather)
    …
}
```

Apart from skeletal `VHAdapter`, there are two more: `RepeatAdapter` and `SingleTypeAdapter`. They don't use Delegapter but employ `VH` and `Delegate` for the ease of use.

### DiffUtil

In order to use `DiffUtil`, you need to call replace() function on a `Delegapter` instance:

```kotlin
d.replace {
    d.add(...)
}
```

A temporary instance of `Delegapter` subclass will be passed to the lambda. Its mutation API is quite similar but requires all your delegates to be `DiffDelegate`. Apart from implementing this interface directly (which is boring), there are two more ways:
```kotlin
val someDelegate = ::someDelegate.diff(
  areItemsTheSame = equateBy(SomeItem::id), /*
  areContentsTheSame = Any::equals,
  getChangePayload = { _, _ -> null },
*/)


val otherDelegate = ::otherDelegate + object : DiffUtil.ItemCallback() {
    override fun are...TheSame(...) = ...
}
```

### SpanSizeLookup

This utility is super simple:

```kotlin
layoutManager = GridLayoutManager(context, spanCount, orientation, false).apply {
    spanSizeLookup = delegapter.spanSizeLookup { position, item, delegate ->
        if (delegate == wideDelegate) spanCount else 1
    }
}
```

### ItemDecoration

Decorating different viewTypes is a stressful job. Here's how Delegapter helps you to add spaces between items of certain types:

```kotlin
data.decor(RecyclerView.VERTICAL) {
  // keep 16dp after title, before user
  between({ it === headerDelegate }, { it === userDelegate }, spaceSize = 16f)

  // keep 30dp between any two users
  between({ it === userDelegate }, spaceSize = 30f)
  
  // text units for text items!
  between({ it === textDelegate }, spaceSize = 16f, spaceUnit = COMPLEX_UNIT_SP)
}
```

Predicates like `{ it === headerDelegate }` look clumsy but are very flexible because you can check for several conditions there, for example, match any type (`{ true }`) or check for external conditions (`{ useTextSpaces && it === textDelegate }`).

Any tool can make you happy until it works fine. And make you hate your job when something gets screwed up. A virtue of any abstraction level is an ability to peek into and see what actually happens. If you feel sad, just pass some booleans around: `decor(orientation, debugDelegates = true, debugSpaces = true)`. This will show you which delegate is used for each item (that's where `::function.toString()` helps!), or highlight spaces, accordingly.

![Screenshot](screenshot.png)
