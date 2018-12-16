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

typealias PreHook = () -> dynamic
typealias InitHook = (vNode: VNode) -> dynamic
typealias CreateHook = (emptyVNode: VNode, vNode: VNode) -> dynamic
typealias InsertHook = (vNode: VNode) -> dynamic
typealias PrePatchHook = (oldVNode: VNode, vNode: VNode) -> dynamic
typealias UpdateHook = (oldVNode: VNode, vNode: VNode) -> dynamic
typealias PostPatchHook = (oldVNode: VNode, vNode: VNode) -> dynamic
typealias DestroyHook = (vNode: VNode) -> dynamic
typealias RemoveHook = (vNode: VNode, removeCallback: () -> Unit) -> dynamic
typealias PostHook = () -> dynamic
