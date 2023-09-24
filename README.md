
The only adapter delegate library which does the job.

```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

...

dependencies {
    implementation("com.github.Miha-x64:Delegapter:0.98")
}

```

### Why not [sockeqwe/AdapterDelegates](https://github.com/sockeqwe/AdapterDelegates/)?

The idea of registering a delegate for a certain item type is flawed:
* one could forget to register a delegate (runtime crash)
* or to unregister a useless one (dead code)
* having items of the same type with different `viewTypes` and `ViewHolder`s is impossible
* having items of different generic types with the same raw type is impossible

The concept of this library is to make everything clear and explicit. No binding a delegate to certain item type, no fallback delegates.

### ViewHolder

You can (but not required to) use our `VH` class for a bunch of reasons:
* `RecyclerView.ViewHolder` is `abstract`, but it's sometimes necessary to create a “dumb” holder without any special fields or behavior
* There's `RecyclerView.ViewHolder.itemView: View`, but `VH` is generic, and has a property `VH<V, *>.view: V` exposing `View` subtype
* When using viewBinding, all `ViewHolder`s look the same: they have `binding` field. `VH` supports an attachment of any type which is typically `ViewBinding`: `VH<*, B>.binding: B`

For example:
```kotlin
VH(TextView(parent.context).apply {
    layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    fontRes = R.font.roboto
    textSize = 17f
}) // VH<TextView, Nothing?>

inflateVH(parent, ItemUserBinding::inflate) // VH<View, ItemUserBinding>
```

### ViewHolder factory

Before we start, we need a `ViewHolderFactory` which is just `(parent: ViewGroup) -> ViewHolder`. 

This is as simple as wrapping VH creation in a lambda:
```kotlin
val userVHF = "user" { parent: ViewGroup ->
    inflateVH(…) { … }
}
```

The string before lambda makes it go through library's `String.invoke(lambda)` function to make it named for debugging purposes. (Unfortunately, `tagged@ { lambda }` has no effect on `toString()`.)

Of course, plain lambdas can be used, as well as `::function` references (which are named on their own).

### AdapterDelegate

Attaching a binding function to `ViewHolderFactory` yields an `AdapterDelegate`:

```kotlin
val userDelegate = "user" { parent: ViewGroup ->
    inflateVH(…) { … }
} bind { item: User, payloads: List<Any> -> // this: ViewHolder
    binding.name.text = item.name
} // AdapterDelegate<User, …>
```

<!-- TODO -->

### Delegapter

Delegapter is basically a list of (item, delegate) tuples, but their type agreement is guaranteed, like it was a `List<<D> Pair<Delegate<D>, D>` (non-denotable type in Java/Kotlin). 

Delegapter is not an `Adapter` itself, just a special data structure. Let's use `DelegatedAdapter` for convenience, it already has `val data = Delegapter(this, …)` property inside:

```kotlin
class SomeAdapter : DelegatedAdapter() {

    init { stateRestorationPolicy = … }

    fun update(item: Data) {
        data.clear()
        data.add(headerDelegate, item.header)
        data.addAll(recommendationDelegate, item.recommended)
        data.addAll(postDelegate, item.posts)
        // use autocomplete to see all available functions
    }

}
```

You may want to use `Delegapter` with a custom adapter in some advanced usage scenarios:
* Insert items not handled by `Delegapter` (headers, footers, ads 🤮).
  (Instead of passing `this` to the constructor, use custom `ListUpdateCallback` implementation to correct `notify*()` calls)
* Filter out some items without removing them
  (this requires a corrected `ListUpdateCallback`, too)
* Use several Delegapters in a single Adapter (IDK why but this should happen at some point)

In order to share `RecycledViewPool` between several `RecyclerView`s, you need to use `MutableDelegapter.recycledViewPool` and preserve the same `viewType` to `Delegate` mapping across adapters. The latter can be achieved using a shared “parent” `Delegapter`:

```kotlin
val delegapterFather = Delegapter(NullListUpdateCallback)

class SomeAdapter : RecyclerView.Adapter<…>() { // for custom adapter
    private val d = Delegapter(this, delegapterFather)
    …
}
val otherAdapter = DelegatedAdapter(delegapterFather) // using pre-baked adapter
```

Apart from skeletal `VHAdapter` and ready-to-use `DelegatedAdapter`, there are two more: `RepeatAdapter` and `SingleTypeAdapter`. They don't use Delegapter but employ `VH` and `Delegate` for the ease of reuse.

### DiffUtil

In order to use `DiffUtil`, you need to call `replace { }` function on a `Delegapter` instance:

```kotlin
data.replace {
    add(...)
}
```

A temporary instance of `Delegapter` subclass will be passed to the lambda. Its mutation API is quite similar but requires all your delegates to be diffable:
```kotlin
val someDelegate = "some" { … }.bind { … }.diff(
  areItemsTheSame = equateBy(SomeItem::id), /*
  areContentsTheSame = Any::equals,
  getChangePayload = { _, _ -> null },
*/)


val otherDelegate = "other" { … }.bind { … } + object : DiffUtil.ItemCallback() {
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

TODO

![Screenshot](screenshot.png)
