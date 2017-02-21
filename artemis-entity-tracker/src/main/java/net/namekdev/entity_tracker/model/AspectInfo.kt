package net.namekdev.entity_tracker.model

import com.artemis.utils.BitVector

class AspectInfo {
    lateinit var allTypes: BitVector
    lateinit var oneTypes: BitVector
    lateinit var exclusionTypes: BitVector


    constructor() {}

    constructor(allTypes: BitVector, oneTypes: BitVector, exclusionTypes: BitVector) {
        this.allTypes = allTypes
        this.oneTypes = oneTypes
        this.exclusionTypes = exclusionTypes
    }
}