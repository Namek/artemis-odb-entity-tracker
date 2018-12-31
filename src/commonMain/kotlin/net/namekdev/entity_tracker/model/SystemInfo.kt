package net.namekdev.entity_tracker.model

open class SystemInfo_Common<TBitVector>(
    val index: Int,
    val name: String,
    val aspectInfo: AspectInfo_Common<TBitVector>,
    val actives: TBitVector?
) {
    var entitiesCount = 0
    var maxEntitiesCount = 0

    val hasAspect
        get() = !aspectInfo.isEmpty
}
