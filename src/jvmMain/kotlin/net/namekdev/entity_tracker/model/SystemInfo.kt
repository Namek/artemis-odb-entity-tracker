package net.namekdev.entity_tracker.model

import com.artemis.Aspect
import com.artemis.BaseSystem
import com.artemis.EntitySubscription
import com.artemis.utils.BitVector

//
//open class SystemInfo_<TBitVector>(
//    val systemIndex: Int,
//    val systemName: String,
//    val actives: TBitVector
//) {
//    var entitiesCount = 0
//    var maxEntitiesCount = 0
//}
//
////class JsSystemInfo : SystemInfo_<CommonBitVector>
//
//class ServerSystemInfo(
//    systemIndex: Int,
//    systemName: String,
//    val system: BaseSystem,
//    val aspect: Aspect?,
//    val aspectInfo: AspectInfo,
//    actives: BitVector?,
//    val subscription: EntitySubscription?
//) : SystemInfo_<BitVector>(
//    systemIndex,
//    systemName,
//    actives
//) {
//
//}

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