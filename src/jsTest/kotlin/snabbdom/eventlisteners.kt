package snabbdom

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import snabbdom.modules.ClassModule
import snabbdom.modules.EventListenersModule
import snabbdom.modules.On
import kotlin.browser.document
import kotlin.test.BeforeTest
import kotlin.test.Test

private val patch = Snabbdom.init(
    arrayOf(EventListenersModule())
)


private class EventListeners {
    lateinit var elm: Element
    lateinit var vnode0: Element

    @BeforeTest
    fun beforeEach() {
        elm = document.createElement("div")
        vnode0 = elm
    }

    @Test fun attaches_click_event_handler_to_element() {
        val result = mutableListOf<dynamic>()
        val clicked = { ev: dynamic -> result.add(ev) }
        var vnode = h("div", VNodeData(on = j("click" to clicked)), arrayOf(
            h("a", "Click my parent")
        ))
        elm = patch(vnode0, vnode).elm as Element
        (elm as HTMLElement).click()
        assert.equal(1, result.size)
    }
    @Test fun does_not_attach_new_listener() {
        val result = mutableListOf<Int>()
        //function clicked(ev) { result.push(ev) }
        val vnode1 = h("div", VNodeData(on = j("click" to {ev: dynamic -> result.add(1) })),
            arrayOf(h("a", "Click my parent"))
        )
        val vnode2 = h("div", VNodeData(on = j("click" to {ev: dynamic -> result.add(2) })),
            arrayOf(h("a", "Click my parent"))
        )
        elm = patch(vnode0, vnode1).elm as Element
        (elm as HTMLElement).click()
        elm = patch(vnode1, vnode2).elm as Element
        (elm as HTMLElement).click()
        assert.deepEqual(result.toTypedArray(), arrayOf(1, 2))
    }
//    @Test fun does_calls_handler_for_function_in_array() {
//        var result = mutableListOf()
//        function clicked(ev) { result.push(ev) }
//        var vnode = h("div", {on: {click: [clicked, 1]}}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode0, vnode).elm as Element
//        elm.click()
//        assert.deepEqual(result, [1])
//    }
//    @Test fun handles_changed_value_in_array() {
//        var result = mutableListOf()
//        function clicked(ev) { result.push(ev) }
//        var vnode1 = h("div", {on: {click: [clicked, 1]}}, [
//            h("a", "Click my parent"),
//        ])
//        var vnode2 = h("div", {on: {click: [clicked, 2]}}, [
//            h("a", "Click my parent"),
//        ])
//        var vnode3 = h("div", {on: {click: [clicked, 3]}}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode0, vnode1).elm
//        elm.click()
//        elm = patch(vnode1, vnode2).elm
//        elm.click()
//        elm = patch(vnode2, vnode3).elm
//        elm.click()
//        assert.deepEqual(result, [1, 2, 3])
//    }
//    @Test fun handles_changed_several_values_in_array() {
//        var result = mutableListOf()
//        function clicked() { result.push([].slice.call(arguments, 0, arguments.length-2)) }
//        var vnode1 = h("div", {on: {click: [clicked, 1, 2, 3]}}, [
//            h("a", "Click my parent"),
//        ])
//        var vnode2 = h("div", {on: {click: [clicked, 1, 2]}}, [
//            h("a", "Click my parent"),
//        ])
//        var vnode3 = h("div", {on: {click: [clicked, 2, 3]}}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode0, vnode1).elm
//        elm.click()
//        elm = patch(vnode1, vnode2).elm
//        elm.click()
//        elm = patch(vnode2, vnode3).elm
//        elm.click()
//        assert.deepEqual(result, [[1, 2, 3], [1, 2], [2, 3]])
//    }
    @Test fun detach_attached_click_event_handler_to_element() {
        val result = mutableListOf<dynamic>()
        val clicked = { ev: dynamic -> result.add(ev) }
        val vnode1 = h("div", VNodeData(on = j("click" to clicked)), arrayOf(
            h("a", "Click my parent")
        ))
        elm = patch(vnode0, vnode1).elm as Element
        (elm as HTMLElement).click()
        assert.equal(1, result.size)
        val vnode2 = h("div", VNodeData(on = j()), arrayOf(
            h("a", "Click my parent")
        ))
        elm = patch(vnode1, vnode2).elm as Element
        (elm as HTMLElement).click()
        assert.equal(1, result.size)
    }
//    @Test fun multiple_event_handlers_for_same_event_on_same_element() {
//        var called = 0
//        val clicked = {ev: dynamic, vnode: VNode ->
//            ++called
//            // Check that the first argument is an event
//            assert.equal(true, ev.target != null)
//            // Check that the second argument was a vnode
//            assert.equal(vnode.sel, "div")
//        }
//        var vnode1 = h("div", {on: {click: [[clicked], [clicked], [clicked]]}}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode0, vnode1).elm
//        elm.click()
//        assert.equal(3, called)
//        var vnode2 = h("div", {on: {click: [[clicked], [clicked]]}}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode1, vnode2).elm
//        elm.click()
//        assert.equal(5, called)
//    }
    @Test fun access_to_virtual_node_in_event_handler() {
        val result = mutableListOf<dynamic>()
        val clicked = { ev: dynamic, vnode: VNode -> result.add(this); result.add(vnode) }
        val vnode1 = h("div", VNodeData(on = j("click" to clicked )), arrayOf(
            h("a", "Click my parent")
        ))
        elm = patch(vnode0, vnode1).elm as Element
        (elm as HTMLElement).click()
        assert.equal(2, result.size)
//        assert.equal(vnode1, result[0])  // this will not success in Kotlin. We don't have a way to overwrite a value of `this`.
        assert.equal(vnode1, result[1])
    }
//    @Test fun access_to_virtual_node_in_event_handler_with_argument() {
//        var result = mutableListOf()
//        function clicked(arg, ev, vnode) { result.push(this) result.push(vnode) }
//        var vnode1 = h("div", {on: {click: [clicked, 1] }}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode0, vnode1).elm
//        elm.click()
//        assert.equal(2, result.length)
//        assert.equal(vnode1, result[0])
//        assert.equal(vnode1, result[1])
//    }
//    @Test fun access_to_virtual_node_in_event_handler_with_arguments() {
//        var result = mutableListOf()
//        function clicked(arg1, arg2, ev, vnode) { result.push(this) result.push(vnode) }
//        var vnode1 = h("div", {on: {click: [clicked, 1, "2"] }}, [
//            h("a", "Click my parent"),
//        ])
//        elm = patch(vnode0, vnode1).elm
//        elm.click()
//        assert.equal(2, result.length)
//        assert.equal(vnode1, result[0])
//        assert.equal(vnode1, result[1])
//    }
    @Test fun shared_handlers_in_parent_and_child_nodes() {
        val result = mutableListOf<dynamic>()
        val sharedHandlers = j<On>(
            "click" to { ev: dynamic -> result.add(ev) }
        )
        val vnode1 = h("div", VNodeData(on = sharedHandlers), arrayOf(
            h("a", VNodeData(on = sharedHandlers), "Click my parent")
        ))
        elm = patch(vnode0, vnode1).elm as Element
        (elm as HTMLElement).click()
        assert.equal(1, result.size)
        (elm.firstChild as HTMLElement).click()
        assert.equal(3, result.size)
    }
}
