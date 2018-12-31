package net.namekdev.entity_tracker.model

import com.artemis.Aspect
import com.artemis.BaseSystem
import com.artemis.EntitySubscription
import com.artemis.utils.BitVector

class SystemInfo(
    systemIndex: Int,
    systemName: String,
    val system: BaseSystem,
    val aspect: Aspect?,
    aspectInfo: AspectInfo_Common<BitVector>,
    actives: BitVector?,
    val subscription: EntitySubscription?
) : SystemInfo_Common<BitVector>(
    systemIndex,
    systemName,
    aspectInfo,
    actives
)
