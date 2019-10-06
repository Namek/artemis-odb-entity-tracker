package snabbdom

interface Hooks {
    var pre: PreHook?
    var init: InitHook?
    var create: CreateHook?
    var insert: InsertHook?
    var prepatch: PrePatchHook?
    var update: UpdateHook?
    var postpatch: PostPatchHook?
    var destroy: DestroyHook?
    var remove: RemoveHook?
    var post: PostHook?
}

typealias PreHook = () -> Unit
typealias InitHook = (vNode: VNode) -> Unit
typealias CreateHook = (emptyVNode: VNode, vNode: VNode) -> Unit
typealias InsertHook = (vNode: VNode) -> Unit
typealias PrePatchHook = (oldVNode: VNode, vNode: VNode) -> Unit
typealias UpdateHook = (oldVNode: VNode, vNode: VNode) -> Unit
typealias PostPatchHook = (oldVNode: VNode, vNode: VNode) -> Unit
typealias DestroyHook = (vNode: VNode) -> Unit
typealias RemoveHook = (vNode: VNode, removeCallback: () -> Unit) -> Unit
typealias PostHook = () -> Unit
