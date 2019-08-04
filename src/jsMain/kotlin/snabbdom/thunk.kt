package snabbdom

interface ThunkFn {
    operator fun invoke(vararg args: dynamic): VNode

    companion object {
        fun args1(fn: (dynamic) -> VNode): ThunkFn {
            return object : ThunkFn {
                override fun invoke(vararg args: dynamic): VNode =
                    fn(args[0])
            }
        }

        fun args2(fn: (dynamic, dynamic) -> VNode): ThunkFn {
            return object : ThunkFn {
                override fun invoke(vararg args: dynamic): VNode =
                    fn(args[0], args[1])
            }
        }

        fun args3(fn: (dynamic, dynamic, dynamic) -> VNode): ThunkFn {
            return object : ThunkFn {
                override fun invoke(vararg args: dynamic): VNode =
                    fn(args[0], args[1], args[2])
            }
        }
    }
}

private fun copyToThunk(vnode: VNode, thunk: VNode) {
    thunk.elm = vnode.elm
    (vnode.data as VNodeData).fn = (thunk.data as VNodeData).fn
    (vnode.data as VNodeData).args = (thunk.data as VNodeData).args
    thunk.data = vnode.data
    thunk.children = vnode.children
    thunk.text = vnode.text
    thunk.elm = vnode.elm
}

private fun init(thunk: VNode) {
    val cur = thunk.data as VNodeData
    val vnode = cur.fn!!.invoke(*cur.args!!)
    copyToThunk(vnode, thunk)
}

private fun prepatch(oldVnode: VNode, thunk: VNode) {
    val old = oldVnode.data as VNodeData
    val cur = thunk.data as VNodeData
    val oldArgs = old.args!!
    val args = cur.args!!

    if (old.fn !== cur.fn || oldArgs.size != args.size) {
        copyToThunk(cur.fn!!.invoke(*args), thunk)
        return
    }
    for (i in 0 until (args.size ?: 0)) {
        if (oldArgs[i] !== args[i]) {
            copyToThunk(cur.fn!!.invoke(*args), thunk)
            return
        }
    }
    copyToThunk(oldVnode, thunk)
}

inline fun thunk(sel: String, fn: ThunkFn, args: Array<Any?>): VNode {
    return thunk(sel, null, fn, args)
}

fun thunk(sel: String, key: Any?, fn: ThunkFn, args: Array<Any?>): VNode {
    val hooks: Hooks = j()
    hooks.init = ::init
    hooks.prepatch = ::prepatch

    return h(sel, VNodeData(key = key, hook = hooks, fn = fn, args = args))
}
