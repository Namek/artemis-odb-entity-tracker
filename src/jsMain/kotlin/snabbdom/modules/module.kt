package snabbdom.modules

import snabbdom.*

abstract class Module {
    open val pre: PreHook? = null
    open val create: CreateHook? = null
    open val update: UpdateHook? = null
    open val destroy: DestroyHook? = null
    open val remove: RemoveHook? = null
    open val post: PostHook? = null
}

class ModuleHooks(
    val pre: Array<PreHook>,
    val create: Array<CreateHook>,
    val update: Array<UpdateHook>,
    val destroy: Array<DestroyHook>,
    val remove: Array<RemoveHook>,
    val post: Array<PostHook>
)
