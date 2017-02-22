package net.namekdev.entity_tracker.model

import com.artemis.utils.BitVector

class AspectInfo {
    var allTypes: BitVector? = null
    var oneTypes: BitVector? = null
    var exclusionTypes: BitVector? = null


    constructor() {}

    constructor(allTypes: BitVector?, oneTypes: BitVector?, exclusionTypes: BitVector?) {
        this.allTypes = allTypes
        this.oneTypes = oneTypes
        this.exclusionTypes = exclusionTypes
    }
}