package snabbdom.modules

import snabbdom.*

open class Module(
    var pre: PreHook? = null,
    var create: CreateHook? = null,
    var update: UpdateHook? = null,
    var destroy: DestroyHook? = null,
    var remove: RemoveHook? = null,
    var post: PostHook? = null
)

class ModuleHooks(
    val pre: Array<PreHook>,
    val create: Array<CreateHook>,
    val update: Array<UpdateHook>,
    val destroy: Array<DestroyHook>,
    val remove: Array<RemoveHook>,
    val post: Array<PostHook>
)
