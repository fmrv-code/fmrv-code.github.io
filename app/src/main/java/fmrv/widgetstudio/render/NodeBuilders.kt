package fmrv.widgetstudio.render

/** Constructeurs concis : `column(h = CENTER) { +text(...); +space(h = 4f) }`. */
class Children {
    internal val list = ArrayList<Node>()

    operator fun Node?.unaryPlus() {
        if (this != null) list += this
    }

    fun add(nodes: List<Node>) {
        list += nodes
    }
}

fun column(
    mod: Mod = Mod(),
    h: HAlign = HAlign.START,
    v: VAlign = VAlign.TOP,
    build: Children.() -> Unit,
): Column = Column(mod, h, v, Children().apply(build).list)

fun row(
    mod: Mod = Mod(),
    h: HAlign = HAlign.START,
    v: VAlign = VAlign.TOP,
    build: Children.() -> Unit,
): Row = Row(mod, h, v, Children().apply(build).list)

fun box(
    mod: Mod = Mod(),
    h: HAlign = HAlign.CENTER,
    v: VAlign = VAlign.CENTER,
    build: Children.() -> Unit,
): Box = Box(mod, h, v, Children().apply(build).list)

fun space(w: Float = 0f, h: Float = 0f): Space = Space(w, h)

/** Tous les nœuds de l'arbre (parcours en profondeur). */
fun Node.walk(): Sequence<Node> = sequence {
    yield(this@walk)
    when (val node = this@walk) {
        is Box -> node.children.forEach { yieldAll(it.walk()) }
        is Column -> node.children.forEach { yieldAll(it.walk()) }
        is Row -> node.children.forEach { yieldAll(it.walk()) }
        is ScrollList -> node.items.forEach { yieldAll(it.walk()) }
        else -> Unit
    }
}

/** Images à charger avant le dessin (icônes d'applications, photos), avec leur taille maximale en dp. */
fun Node.imageRequests(): Map<ImgSource, Float> {
    val requests = LinkedHashMap<ImgSource, Float>()
    walk().filterIsInstance<Img>()
        .filter { it.source !is ImgSource.Res }
        .forEach { img ->
            val size = maxOf(img.widthDp, img.heightDp)
            requests[img.source] = maxOf(requests[img.source] ?: 0f, size)
        }
    return requests
}
