package net.namekdev.entity_tracker.model

import com.artemis.utils.BitVector

data class AspectInfo(
    var allTypes: BitVector? = null,
    var oneTypes: BitVector? = null,
    var exclusionTypes: BitVector? = null
)