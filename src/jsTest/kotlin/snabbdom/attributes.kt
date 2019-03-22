package snabbdom

import org.w3c.dom.Element
import snabbdom.modules.*
import kotlin.browser.document
import kotlin.test.Test

private val patch = Snabbdom.init(
    arrayOf(AttributesModule())
)


private class Attributes : TestBase() {
    @Test fun have_their_provided_values() {
        val vnode1 = h("div", VNodeData(attrs = j("href" to "/foo", "minlength" to 1, "selected" to true, "disabled" to false)))
        elm = patch(vnode0, vnode1).elm as Element
        assert.strictEqual(elm.getAttribute("href"), "/foo")
        assert.strictEqual(elm.getAttribute("minlength"), "1")
        assert.strictEqual(elm.hasAttribute("selected"), true)
        assert.strictEqual(elm.getAttribute("selected"), "")
        assert.strictEqual(elm.hasAttribute("disabled"), false)
    }
    @Test fun can_be_memoized() {
        val cachedAttrs = j<Attrs>("href" to "/foo", "minlength" to 1, "selected" to true)
        val vnode1 = h("div", VNodeData(attrs = cachedAttrs))
        val vnode2 = h("div", VNodeData(attrs = cachedAttrs))
        elm = patch(vnode0, vnode1).elm as Element
        assert.strictEqual(elm.getAttribute("href"), "/foo")
        assert.strictEqual(elm.getAttribute("minlength"), "1")
        assert.strictEqual(elm.getAttribute("selected"), "")
        elm = patch(vnode1, vnode2).elm as Element
        assert.strictEqual(elm.getAttribute("href"), "/foo")
        assert.strictEqual(elm.getAttribute("minlength"), "1")
        assert.strictEqual(elm.getAttribute("selected"), "")
    }
    @Test fun are_not_omitted_when_falsy_values_are_provided() {
        val vnode1 = h("div", VNodeData(attrs = j("href" to null, "minlength" to 0, "value" to "", "title" to "undefined")))
        elm = patch(vnode0, vnode1).elm as Element
        assert.strictEqual(elm.getAttribute("href"), "null")
        assert.strictEqual(elm.getAttribute("minlength"), "0")
        assert.strictEqual(elm.getAttribute("value"), "")
        assert.strictEqual(elm.getAttribute("title"), "undefined")
    }
    @Test fun are_set_correctly_when_namespaced() {
        val vnode1 = h("div", VNodeData(attrs = j("xlink:href" to "#foo")))
        elm = patch(vnode0, vnode1).elm as Element
        assert.strictEqual(elm.getAttributeNS("http://www.w3.org/1999/xlink", "href"), "#foo")
    }
    @Test fun should_not_touch_class_nor_id_fields() {
        elm = document.createElement("div")
        elm.id = "myId"
        elm.className = "myClass"
        vnode0 = elm
        val vnode1 = h("div#myId.myClass", VNodeData(attrs = j()), arrayOf("Hello"))
        elm = patch(vnode0, vnode1).elm as Element
        assert.strictEqual(elm.tagName, "DIV")
        assert.strictEqual(elm.id, "myId")
        assert.strictEqual(elm.className, "myClass")
        assert.strictEqual(elm.textContent, "Hello")
    }

    class boolean_attribute : TestBase() {
        @Test fun is_present_and_empty_string_if_the_value_is_truthy() {
            val vnode1 = h("div", VNodeData(attrs = j("required" to true, "readonly" to 1, "noresize" to "truthy")))
            elm = patch(vnode0, vnode1).elm as Element
            assert.strictEqual(elm.hasAttribute("required"), true)
            assert.strictEqual(elm.getAttribute("required"), "")
            assert.strictEqual(elm.hasAttribute("readonly"), true)
            assert.strictEqual(elm.getAttribute("readonly"), "1")
            assert.strictEqual(elm.hasAttribute("noresize"), true)
            assert.strictEqual(elm.getAttribute("noresize"), "truthy")
        }
        @Test fun is_omitted_if_the_value_is_false() {
            val vnode1 = h("div", VNodeData(attrs = j("required" to false)))
            elm = patch(vnode0, vnode1).elm as Element
            assert.strictEqual(elm.hasAttribute("required"), false)
            assert.strictEqual(elm.getAttribute("required"), null)
        }
        @Test fun is_not_omitted_if_the_value_is_falsy_but_casted_to_string() {
            val vnode1 = h("div", VNodeData(attrs = j("readonly" to 0, "noresize" to null)))
            elm = patch(vnode0, vnode1).elm as Element
            assert.strictEqual(elm.getAttribute("readonly"), "0")
            assert.strictEqual(elm.getAttribute("noresize"), "null")
        }
    }

    class Object_prototype_property : TestBase() {
        @Test fun is_not_considered_as_a_boolean_attribute_and_shouldnt_be_omitted() {
            val vnode1 = h("div", VNodeData(attrs = j("constructor" to true)))
            elm = patch(vnode0, vnode1).elm as Element
            assert.strictEqual(elm.hasAttribute("constructor"), true)
            assert.strictEqual(elm.getAttribute("constructor"), "")
            val vnode2 = h("div", VNodeData(attrs = j("constructor" to false)))
            elm = patch(vnode0, vnode2).elm as Element
            assert.strictEqual(elm.hasAttribute("constructor"), false)
        }
    }
}