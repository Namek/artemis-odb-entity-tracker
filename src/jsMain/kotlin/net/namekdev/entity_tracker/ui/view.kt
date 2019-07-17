package net.namekdev.entity_tracker.ui

interface IView {
    fun view(): RNode
    fun dispose() { }
}