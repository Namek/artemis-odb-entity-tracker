package net.namekdev.entity_tracker.model

data class AspectInfo_Common<TBitVector>(
    var allTypes: TBitVector? = null,
    var oneTypes: TBitVector? = null,
    var exclusionTypes: TBitVector? = null
) {
    val isEmpty
        get() = allTypes == null || oneTypes == null || exclusionTypes == null
}