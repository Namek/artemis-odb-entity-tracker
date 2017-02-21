package net.namekdev.entity_tracker.model

import com.artemis.Aspect
import com.artemis.BaseSystem
import com.artemis.EntitySubscription
import com.artemis.utils.BitVector

data class SystemInfo(
    val systemIndex: Int,
    val systemName: String,
    val system: BaseSystem,
    val aspect: Aspect?,
    val aspectInfo: AspectInfo,
    val actives: BitVector?,
    val subscription: EntitySubscription?
) {
    var entitiesCount = 0
    var maxEntitiesCount = 0
}