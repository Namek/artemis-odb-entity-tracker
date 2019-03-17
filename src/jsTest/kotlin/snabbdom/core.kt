@file:Suppress("MoveLambdaOutsideParentheses")

package snabbdom

import org.w3c.dom.*
import snabbdom.modules.*
import kotlin.browser.*
import kotlin.test.*

//var assert = require("assert")
//var shuffle = require("knuth-shuffle").knuthShuffle;

//var snabbdom = require("../snabbdom")
val patch = Snabbdom.init(
    arrayOf(ClassModule(), PropsModule(), EventListenersModule())
)
//var h = require("../h").default;
//var toVNode = require("../tovnode").default;
//val vnode = require("../vnode").default;
//var htmlDomApi = require("../htmldomapi").htmlDomApi;

fun prop(name: String): (obj: dynamic) -> dynamic {
    return { obj ->
        obj[name]
    }
}

fun map(fn: (dynamic) -> dynamic, list: ItemArrayLike<dynamic>): Array<dynamic> =
    list.asList().map(fn).toTypedArray()

val inner = prop("innerHTML")


fun describe(name: String, action: () -> Unit) {
    action()
}

abstract class TestBase {
    lateinit var elm: Element
    lateinit var vnode0: Element

    @BeforeTest
    fun beforeEach() {
        elm = document.createElement("div")
        vnode0 = elm;
    }
}

class SnabbdomTest : TestBase() {
    fun _it(descr: String, action: () -> Unit) {
        elm = document.createElement("div")
        vnode0 = elm as Element
        action()
    }


    @Test
    fun hyperscript() {
        _it("can create vnode with proper tag", {
            assertEquals("div", h("div").sel)
            assertEquals("a", h("a").sel)
        })
        _it("can create vnode with children", {
            val vnode = h("div", arrayOf(h("span#hello"), h("b.world")))
            assertEquals("div", vnode.sel)
            assertEquals("span#hello", vnode.children!![0].sel)
            assertEquals("b.world", vnode.children!![1].sel)
        })
        _it("can create vnode with one child vnode", {
            val vnode = h("div", h("span#hello"))
            assertEquals("div", vnode.sel)
            assertEquals("span#hello", vnode.children!![0].sel)
        })
        _it("can create vnode with props and one child vnode", {
            val vnode = h("div", VNodeData(), h("span#hello"))
            assertEquals(vnode.sel, "div")
            assertEquals(vnode.children!![0].sel, "span#hello")
        })
        _it("can create vnode with text content", {
            val vnode = h("a", "I am a string")
            assertEquals("I am a string", vnode.children!![0].text)
        })
        _it("can create vnode with text content in string", {
            val vnode = h("a", "I am a string")
            assertEquals("I am a string", vnode.text)
        })
        _it("can create vnode with props and text content in string", {
            val vnode = h("a", VNodeData(), "I am a string")
            assertEquals("I am a string", vnode.text)
        })
        _it("can create vnode for comment", {
            val vnode = h("!", "test")
            assertEquals("!", vnode.sel)
            assertEquals("test", vnode.text)
        })
    }

    @Test
    fun created_element() {
        _it("has tag", {
            elm = patch(vnode0, h("div")).elm as Element
            assertEquals("DIV", elm.tagName)
        })
        _it("has different tag and id", {
            var elm = document.createElement("div")
            vnode0.appendChild(elm)
            val vnode1 = h("span#id")
            elm = patch(elm, vnode1).elm as Element
            assertEquals("SPAN", elm.tagName)
            assertEquals("id", elm.id)
        })
        _it("has id", {
            elm = patch(vnode0, h("div", arrayOf(h("div#unique")))).elm as Element
            assertEquals("unique", (elm.firstChild as Element).id)
        })
        _it("has correct namespace", {
            val SVGNamespace = "http://www.w3.org/2000/svg"
            val XHTMLNamespace = "http://www.w3.org/1999/xhtml"

            elm = patch(vnode0, h("div", arrayOf(h("div", VNodeData(ns = SVGNamespace))))).elm as Element
            assertEquals(SVGNamespace, (elm.firstChild as Element).namespaceURI)

            // verify that svg tag automatically gets svg namespace
            elm = patch(
                vnode0, h(
                    "svg", arrayOf(
                        h(
                            "foreignObject", arrayOf(
                                h("div", arrayOf("I am HTML embedded in SVG"))
                            )
                        )
                    )
                )
            ).elm as Element
            assertEquals(SVGNamespace, elm.namespaceURI)
            assertEquals(SVGNamespace, (elm.firstChild as Element).namespaceURI)
            assertEquals(XHTMLNamespace, ((elm.firstChild as Element).firstChild as Element).namespaceURI)

            // verify that svg tag with extra selectors gets svg namespace
            elm = patch(vnode0, h("svg#some-id")).elm as Element
            assertEquals(SVGNamespace, elm.namespaceURI)

            // verify that non-svg tag beginning with "svg" does NOT get namespace
            elm = patch(vnode0, h("svg-custom-el")).elm as Element
            assertNotEquals(elm.namespaceURI, SVGNamespace)
        })
        _it("receives classes in selector", {
            elm = patch(vnode0, h("div", arrayOf(h("i.am.a.class")))).elm as Element
            assertTrue((elm.firstChild as Element).classList.contains("am"))
            assertTrue((elm.firstChild as Element).classList.contains("a"))
            assertTrue((elm.firstChild as Element).classList.contains("class"))
        })
        _it("receives classes in class property", {
            elm = patch(
                vnode0,
                h("i", VNodeData(`class` = j("am" to true, "a" to true, "class" to true, "not" to false)))
            ).elm as Element
            assertTrue(elm.classList.contains("am"))
            assertTrue(elm.classList.contains("a"))
            assertTrue(elm.classList.contains("class"))
            assertFalse(elm.classList.contains("not"))
        })
        _it("receives classes in selector when namespaced", {
            elm = patch(
                vnode0,
                h(
                    "svg", arrayOf(
                        h("g.am.a.class.too")
                    )
                )
            ).elm as Element
            assertTrue((elm.firstChild as Element).classList.contains("am"))
            assertTrue((elm.firstChild as Element).classList.contains("a"))
            assertTrue((elm.firstChild as Element).classList.contains("class"))
        })
        _it("receives classes in class property when namespaced", {
            elm = patch(
                vnode0,
                h(
                    "svg", arrayOf<VNode>(
                        h(
                            "g",
                            VNodeData(
                                `class` = j(
                                    "am" to true,
                                    "a" to true,
                                    "class" to true,
                                    "not" to false,
                                    "too" to true
                                )
                            )
                        )
                    )
                )
            ).elm as Element
            assertTrue((elm.firstChild as Element).classList.contains("am"))
            assertTrue((elm.firstChild as Element).classList.contains("a"))
            assertTrue((elm.firstChild as Element).classList.contains("class"))
            assertFalse((elm.firstChild as Element).classList.contains("not"))
        })
        _it("handles classes from both selector and property", {
            elm = patch(vnode0, h("div", arrayOf(h("i.has", VNodeData(`class` = j("classes" to true)))))).elm as Element
            assertTrue((elm.firstChild as Element).classList.contains("has"))
            assertTrue((elm.firstChild as Element).classList.contains("classes"))
        })
        _it("can create elements with text content", {
            elm = patch(vnode0, h("div", arrayOf("I am a string"))).elm as Element
            assertEquals("I am a string", elm.innerHTML)
        })
        /*_it("can create elements with span and text content", {
            elm = patch(vnode0, h("a", arrayOf(h("span"), "I am a string"))).elm as Element
            assertEquals("SPAN", (elm.childNodes[0] as Element).tagName)
            assertEquals("I am a string", (elm.childNodes[1] as Element).textContent)
        })*/
        _it("can create elements with props", {
            elm = patch(vnode0, h("a", VNodeData(props = j("src" to "http://localhost/")))).elm as Element
            assertEquals("http://localhost/", elm.unsafeCast<dynamic>().src)
        })
        /*_it("can create an element created inside an iframe", function(done) {
            // Only run if srcdoc is supported.
            var frame = document.createElement("iframe") as HTMLIFrameElement
            if (typeof frame.srcdoc !== "undefined") {
                frame.srcdoc = "<div>Thing 1</div>";
                frame.onload = {
                    patch(frame.contentDocument.body.querySelector("div"), h("div", "Thing 2"))
                    assertEquals("Thing 2", frame.contentDocument.body.querySelector("div").textContent)
                    frame.remove()
                    done()
                };
                document.body.appendChild(frame)
            } else {
                done()
            }
        })
        _it("is a patch of the root element", {
            var elmWithIdAndClass = document.createElement("div")
            elmWithIdAndClass.id = "id";
            elmWithIdAndClass.className = "class";
            val vnode1 = h("div#id.class", arrayOf(h("span", "Hi")))
            elm = patch(elmWithIdAndClass, vnode1).elm as Element;
            assertEquals("DIV", elm.tagName)
            assertEquals("id", elm.id)
            assertEquals("class", elm.className)

            // original: assert.strictEqual(elm, elmWithIdAndClass)
            assertTrue(elm === elmWithIdAndClass)
        })*/
        _it("can create comments", {
            val node = patch(vnode0, h("!", "test")).elm as Node
            assertEquals(8/*document.COMMENT_NODE*/, node.nodeType)
            assertEquals("test", node.textContent)
        })
    }

    class patching_an_element : TestBase() {
        @Test fun changes_the_elements_classes() {
            val vnode1 = h("i", VNodeData(`class` = j("i" to true, "am" to true, "horse" to true)))
            val vnode2 = h("i", VNodeData(`class` = j("i" to true, "am" to true, "horse" to false)))
            patch(vnode0, vnode1)
            elm = patch(vnode1, vnode2).elm as Element
            assertTrue(elm.classList.contains("i"))
            assertTrue(elm.classList.contains("am"))
            assertFalse(elm.classList.contains("horse"))
        }
        
        @Test fun changes_classes_in_selector() {
            val vnode1 = h("i", VNodeData(`class` = j("i" to true, "am" to true, "horse" to true)))
            val vnode2 = h("i", VNodeData(`class` = j("i" to true, "am" to true, "horse" to false)))
            patch(vnode0, vnode1)
            elm = patch(vnode1, vnode2).elm as Element
            assertTrue(elm.classList.contains("i"))
            assertTrue(elm.classList.contains("am"))
            assertFalse(elm.classList.contains("horse"))
        }

        @Test fun preserves_memoized_classes() {
            val cachedClass = j<Boolean>("i" to true, "am" to true, "horse" to false).unsafeCast<Classes>()
            val vnode1 = h("i", VNodeData(`class` = cachedClass))
            val vnode2 = h("i", VNodeData(`class` = cachedClass))
            elm = patch(vnode0, vnode1).elm as Element
            assertTrue(elm.classList.contains("i"))
            assertTrue(elm.classList.contains("am"))
            assertFalse(elm.classList.contains("horse"))
            elm = patch(vnode1, vnode2).elm as Element
            assertTrue(elm.classList.contains("i"))
            assertTrue(elm.classList.contains("am"))
            assertFalse(elm.classList.contains("horse"))
        }
        @Test fun removes_missing_classes() {
            val vnode1 = h("i", VNodeData(`class` = j("i" to true, "am" to true, "horse" to true)))
            val vnode2 = h("i", VNodeData(`class` = j("i" to true, "am" to true)))
            patch(vnode0, vnode1)
            elm = patch(vnode1, vnode2).elm as Element
            assertTrue(elm.classList.contains("i"))
            assertTrue(elm.classList.contains("am"))
            assertFalse(elm.classList.contains("horse"))
        }
        @Test fun changes_an_elements_props() {
            val vnode1 = h("a", VNodeData(props = j("src" to "http://other/")))
            val vnode2 = h("a", VNodeData(props = j("src" to "http://localhost/")))
            patch(vnode0, vnode1)
            elm = patch(vnode1, vnode2).elm as Element
            assertEquals("http://localhost/", elm.unsafeCast<dynamic>().src)
        }
        @Test fun preserves_memoized_props() {
            val cachedProps = j<String>("src" to "http://other/").unsafeCast<Props>()
            val vnode1 = h("a", VNodeData(props = cachedProps))
            val vnode2 = h("a", VNodeData(props = cachedProps))
            elm = patch(vnode0, vnode1).elm as Element
            assertEquals("http://other/", elm.unsafeCast<dynamic>().src)
            elm = patch(vnode1, vnode2).elm as Element
            assertEquals("http://other/", elm.unsafeCast<dynamic>().src)
        }
        @Test fun removes_an_elements_props() {
            val vnode1 = h("a", VNodeData(props = j("src" to "http://other/")))
            val vnode2 = h("a")
            patch(vnode0, vnode1)
            patch(vnode1, vnode2)
            assertEquals(undefined, elm.unsafeCast<dynamic>().src)
        }

        // well, i have no such function implemented
        class using_toVNode_function : TestBase() {
            /*@Test fun can_remove_previous_children_of_the_root_element() {
                var h2 = document.createElement("h2")
                h2.textContent = "Hello"
                var prevElm = document.createElement("div")
                prevElm.id = "id";
                prevElm.className = "class";
                prevElm.appendChild(h2)
                var nextVNode = h("div#id.class", [h("span", "Hi")])
                elm = patch(toVNode(prevElm), nextVNode).elm as Element
                assert.strictEqual(elm, prevElm)
                assertEquals("DIV", elm.tagName)
                assertEquals("id", elm.id)
                assertEquals("class", elm.className)
                assert.strictEqual(elm.childNodes.length, 1)
                assert.strictEqual(elm.childNodes[0].tagName, "SPAN")
                assert.strictEqual(elm.childNodes[0].textContent, "Hi")
            }*/
            /*
            _it("can support patching in a DocumentFragment", function () {
                var prevElm = document.createDocumentFragment()
                var nextVNode = vnode("", {}, [
                    h("div#id.class", [h("span", "Hi")])
                ], undefined, prevElm)
                elm = patch(toVNode(prevElm), nextVNode).elm as Element
                assert.strictEqual(elm, prevElm)
                assertEquals(11, elm.nodeType)
                assertEquals(1, elm.childNodes.length)
                assertEquals("DIV", elm.childNodes[0].tagName)
                assertEquals("id", elm.childNodes[0].id)
                assertEquals("class", elm.childNodes[0].className)
                assert.strictEqual(elm.childNodes[0].childNodes.length, 1)
                assert.strictEqual(elm.childNodes[0].childNodes[0].tagName, "SPAN")
                assert.strictEqual(elm.childNodes[0].childNodes[0].textContent, "Hi")
            })
            _it("can remove some children of the root element", function () {
                var h2 = document.createElement("h2")
                h2.textContent = "Hello"
                var prevElm = document.createElement("div")
                prevElm.id = "id";
                prevElm.className = "class";
                var text = new Text("Foobar")
                text.testProperty = function () {}; // ensures we dont recreate the Text Node
                prevElm.appendChild(text)
                prevElm.appendChild(h2)
                var nextVNode = h("div#id.class", ["Foobar"])
                elm = patch(toVNode(prevElm), nextVNode).elm as Element
                assert.strictEqual(elm, prevElm)
                assertEquals("DIV", elm.tagName)
                assertEquals("id", elm.id)
                assertEquals("class", elm.className)
                assert.strictEqual(elm.childNodes.length, 1)
                assert.strictEqual(elm.childNodes[0].nodeType, 3)
                assert.strictEqual(elm.childNodes[0].wholeText, "Foobar")
                assert.strictEqual(typeof elm.childNodes[0].testProperty, "function")
            })
            _it("can remove text elements", function () {
                var h2 = document.createElement("h2")
                h2.textContent = "Hello"
                var prevElm = document.createElement("div")
                prevElm.id = "id";
                prevElm.className = "class";
                var text = new Text("Foobar")
                prevElm.appendChild(text)
                prevElm.appendChild(h2)
                var nextVNode = h("div#id.class", [h("h2", "Hello")])
                elm = patch(toVNode(prevElm), nextVNode).elm as Element
                assert.strictEqual(elm, prevElm)
                assertEquals("DIV", elm.tagName)
                assertEquals("id", elm.id)
                assertEquals("class", elm.className)
                assert.strictEqual(elm.childNodes.length, 1)
                assert.strictEqual(elm.childNodes[0].nodeType, 1)
                assert.strictEqual(elm.childNodes[0].textContent, "Hello")
            })
            _it("can work with domApi", function () {
                var domApi = Object.assign({}, htmlDomApi, {
                    tagName: function(elm) { return "x-" + elm.tagName.toUpperCase() }
                })
                var h2 = document.createElement("h2")
                h2.id = "hx";
                h2.setAttribute("data-env", "xyz")
                var text = document.createTextNode("Foobar")
                var elm = document.createElement("div")
                elm.id = "id";
                elm.className = "class other";
                elm.setAttribute("data", "value")
                elm.appendChild(h2)
                elm.appendChild(text)
                val vnode = toVNode(elm, domApi)
                assertEquals("x-div#id.class.other", vnode.sel)
                assert.deepEqual(vnode.data, {attrs: {"data": "value"}})
                assertEquals("x-h2#hx", vnode.children[0].sel)
                assert.deepEqual(vnode.children[0].data, {attrs: {"data-env": "xyz"}})
                assertEquals("Foobar", vnode.children[1].text)
            })
             */
        }

        object updating_children_with_keys : TestBase() {
            fun spanNum(n: dynamic): dynamic =
                if (n == null) {
                    n
                } else if (jsTypeOf(n) === "string") {
                    h("span", VNodeData(), n as String)
                } else {
                    h("span", VNodeData(key = n), n.toString())
                }

            fun spanNums(arr: Array<dynamic>): Array<dynamic> =
                arr.map(::spanNum).toTypedArray()


            class addition_of_elements : TestBase() {
                @Test fun appends_elements() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(1, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("2", elm.children[1]!!.innerHTML)
                    assertEquals("3", elm.children[2]!!.innerHTML)
                }
                @Test fun prepends_elements() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(2, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
//                    assert.deepEqual(map(inner, elm.children), ["1", "2", "3", "4", "5"])
                }
                @Test fun add_elements_in_the_middle() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3", "4", "5"))
                }
                @Test fun add_elements_at_begin_and_end() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(2, 3, 4)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3", "4", "5"))
                }
//                @Test fun adds_children_to_parent_with_no_children() {
//                    val vnode1 = h("span", {key: "span"})
//                    val vnode2 = h("span", {key: "span"}, nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3)))
//                    elm = patch(vnode0, vnode1).elm as Element
//                    assertEquals(0, elm.children.length)
//                    elm = patch(vnode1, vnode2).elm as Element
//                    assert.deepEqual(map(inner, elm.children), ["1", "2", "3"])
//                }
            }
        }
    }
/*
    describe("patching an element", {

        describe("using toVNode()", function () {

        })
        describe("updating children with keys", {

            describe("addition of elements", {

                

                @Test fun removes all children from parent() {
                    val vnode1 = h("span", {key: "span"}, nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3)))
                    val vnode2 = h("span", {key: "span"})
                    elm = patch(vnode0, vnode1).elm as Element
                    assert.deepEqual(map(inner, elm.children), ["1", "2", "3"])
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(0, elm.children.length)
                })
                @Test fun update one child with same key but different sel() {
                    val vnode1 = h("span", {key: "span"}, nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3)))
                    val vnode2 = h("span", {key: "span"}, [spanNum(1), h("i", {key: 2}, "2"), spanNum(3)])
                    elm = patch(vnode0, vnode1).elm as Element
                    assert.deepEqual(map(inner, elm.children), ["1", "2", "3"])
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), ["1", "2", "3"])
                    assertEquals(3, elm.children.length)
                    assertEquals("I", elm.children[1].tagName)
                })
            })
            describe("removal of elements", {
                @Test fun removes elements from the beginning() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), ["3", "4", "5"])
                })
                @Test fun removes elements from the end() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("1", elm.children[0].innerHTML)
                    assertEquals("2", elm.children[1].innerHTML)
                    assertEquals("3", elm.children[2].innerHTML)
                })
                @Test fun removes elements from the middle() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assert.deepEqual(elm.children[0].innerHTML, "1")
                    assertEquals("1", elm.children[0].innerHTML)
                    assertEquals("2", elm.children[1].innerHTML)
                    assertEquals("4", elm.children[2].innerHTML)
                    assertEquals("5", elm.children[3].innerHTML)
                })
            })
            describe("element reordering", {
                @Test fun moves element forward() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(2, 3, 1, 4)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals("2", elm.children[0].innerHTML)
                    assertEquals("3", elm.children[1].innerHTML)
                    assertEquals("1", elm.children[2].innerHTML)
                    assertEquals("4", elm.children[3].innerHTML)
                })
                @Test fun moves element to end() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(2, 3, 1)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("2", elm.children[0].innerHTML)
                    assertEquals("3", elm.children[1].innerHTML)
                    assertEquals("1", elm.children[2].innerHTML)
                })
                @Test fun moves element backwards() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 4, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals("1", elm.children[0].innerHTML)
                    assertEquals("4", elm.children[1].innerHTML)
                    assertEquals("2", elm.children[2].innerHTML)
                    assertEquals("3", elm.children[3].innerHTML)
                })
                @Test fun swaps first and last() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(4, 2, 3, 1)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals("4", elm.children[0].innerHTML)
                    assertEquals("2", elm.children[1].innerHTML)
                    assertEquals("3", elm.children[2].innerHTML)
                    assertEquals("1", elm.children[3].innerHTML)
                })
            })
            describe("combinations of additions, removals and reorderings", {
                @Test fun move to left and replace() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(4, 1, 2, 3, 6)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(5, elm.children.length)
                    assertEquals("4", elm.children[0].innerHTML)
                    assertEquals("1", elm.children[1].innerHTML)
                    assertEquals("2", elm.children[2].innerHTML)
                    assertEquals("3", elm.children[3].innerHTML)
                    assertEquals("6", elm.children[4].innerHTML)
                })
                @Test fun moves to left and leaves hole() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(4, 6)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), ["4", "6"])
                })
                @Test fun handles moved and set to undefined element ending at the end() {
                    val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(2, 4, 5)))
                    val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(4, 5, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("4", elm.children[0].innerHTML)
                    assertEquals("5", elm.children[1].innerHTML)
                    assertEquals("3", elm.children[2].innerHTML)
                })
                @Test fun moves a key in non-keyed nodes with a size up {
                    val vnode1 = h("span", [1, "a", "b", "c"].map(spanNum))
                    val vnode2 = h("span", ["d", "a", "b", "c", 1, "e"].map(spanNum))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.childNodes.length)
                    assertEquals("1abc", elm.textContent)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(6, elm.childNodes.length)
                    assertEquals("dabc1e", elm.textContent)
                })
            })
            @Test fun reverses elements() {
                val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(1, 2, 3, 4, 5, 6, 7, 8)))
                val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(8, 7, 6, 5, 4, 3, 2, 1)))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(8, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["8", "7", "6", "5", "4", "3", "2", "1"])
            })
            @Test fun something() {
                val vnode1 = h("span", nodesOrPrimitives = *spanNums(arrayOf(0, 1, 2, 3, 4, 5)))
                val vnode2 = h("span", nodesOrPrimitives = *spanNums(arrayOf(4, 3, 2, 1, 5, 0)))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(6, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["4", "3", "2", "1", "5", "0"])
            })
            @Test fun handles random shuffles() {
                var n, i, arr = [], opacities = [], elms = 14, samples = 5;
                function spanNumWithOpacity(n, o) {
                return h("span", {key: n, style: {opacity: o}}, n.toString())
            }
                for (n = 0; n < elms; ++n) { arr[n] = n; }
                for (n = 0; n < samples; ++n) {
                val vnode1 = h("span", arr.map(function(n) {
                    return spanNumWithOpacity(n, "1")
                }))
                var shufArr = shuffle(arr.slice(0))
                var elm = document.createElement("div")
                elm = patch(elm, vnode1).elm as Element
                for (i = 0; i < elms; ++i) {
                assertEquals(elm.children[i].innerHTML, i.toString())
                opacities[i] = Math.random().toFixed(5).toString()
            }
                val vnode2 = h("span", arr.map(function(n) {
                    return spanNumWithOpacity(shufArr[n], opacities[n])
                }))
                elm = patch(vnode1, vnode2).elm as Element
                for (i = 0; i < elms; ++i) {
                assertEquals(elm.children[i].innerHTML, shufArr[i].toString())
                assertEquals(0, opacities[i].indexOf(elm.children[i].style.opacity))
            }
            }
            })
            @Test fun supports null/undefined children {
                val vnode1 = h("i", nodesOrPrimitives = *spanNums(arrayOf(0, 1, 2, 3, 4, 5)))
                val vnode2 = h("i", [null, 2, undefined, null, 1, 0, null, 5, 4, null, 3, undefined].map(spanNum))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(6, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["2", "1", "0", "5", "4", "3"])
            })
            @Test fun supports all null/undefined children {
                val vnode1 = h("i", nodesOrPrimitives = *spanNums(arrayOf(0, 1, 2, 3, 4, 5)))
                val vnode2 = h("i", [null, null, undefined, null, null, undefined])
                val vnode3 = h("i", nodesOrPrimitives = *spanNums(arrayOf(5, 4, 3, 2, 1, 0)))
                patch(vnode0, vnode1)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals(0, elm.children.length)
                elm = patch(vnode2, vnode3).elm as Element
                assert.deepEqual(map(inner, elm.children), ["5", "4", "3", "2", "1", "0"])
            })
            @Test fun handles random shuffles with null/undefined children {
                var i, j, r, len, arr, maxArrLen = 15, samples = 5, vnode1 = vnode0, vnode2;
                for (i = 0; i < samples; ++i, vnode1 = vnode2) {
                len = Math.floor(Math.random() * maxArrLen)
                arr = [];
                for (j = 0; j < len; ++j) {
                if ((r = Math.random()) < 0.5) arr[j] = String(j)
                else if (r < 0.75) arr[j] = null;
                else arr[j] = undefined;
            }
                shuffle(arr)
                vnode2 = h("div", arr.map(spanNum))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arr.filter(function(x) {return x != null;}))
            }
            })
        })
        describe("updating children without keys", {
            @Test fun appends elements() {
                val vnode1 = h("div", [h("span", "Hello")])
                val vnode2 = h("div", [h("span", "Hello"), h("span", "World")])
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), ["Hello"])
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["Hello", "World"])
            })
            @Test fun handles unmoved text nodes() {
                val vnode1 = h("div", ["Text", h("span", "Span")])
                val vnode2 = h("div", ["Text", h("span", "Span")])
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0].textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text", elm.childNodes[0].textContent)
            })
            @Test fun handles changing text children() {
                val vnode1 = h("div", ["Text", h("span", "Span")])
                val vnode2 = h("div", ["Text2", h("span", "Span")])
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0].textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text2", elm.childNodes[0].textContent)
            })
            @Test fun handles unmoved comment nodes() {
                val vnode1 = h("div", [h("!", "Text"), h("span", "Span")])
                val vnode2 = h("div", [h("!", "Text"), h("span", "Span")])
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0].textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text", elm.childNodes[0].textContent)
            })
            @Test fun handles changing comment text() {
                val vnode1 = h("div", [h("!", "Text"), h("span", "Span")])
                val vnode2 = h("div", [h("!", "Text2"), h("span", "Span")])
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0].textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text2", elm.childNodes[0].textContent)
            })
            @Test fun handles changing empty comment() {
                val vnode1 = h("div", [h("!"), h("span", "Span")])
                val vnode2 = h("div", [h("!", "Test"), h("span", "Span")])
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("", elm.childNodes[0].textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Test", elm.childNodes[0].textContent)
            })
            @Test fun prepends element() {
                val vnode1 = h("div", [h("span", "World")])
                val vnode2 = h("div", [h("span", "Hello"), h("span", "World")])
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), ["World"])
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["Hello", "World"])
            })
            @Test fun prepends element of different tag type() {
                val vnode1 = h("div", [h("span", "World")])
                val vnode2 = h("div", [h("div", "Hello"), h("span", "World")])
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), ["World"])
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(prop("tagName"), elm.children), ["DIV", "SPAN"])
                assert.deepEqual(map(inner, elm.children), ["Hello", "World"])
            })
            @Test fun removes elements() {
                val vnode1 = h("div", [h("span", "One"), h("span", "Two"), h("span", "Three")])
                val vnode2 = h("div", [h("span", "One"), h("span", "Three")])
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), ["One", "Two", "Three"])
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["One", "Three"])
            })
            @Test fun removes a single text node() {
                val vnode1 = h("div", "One")
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                assertEquals("One", elm.textContent)
                patch(vnode1, vnode2)
                assertEquals("", elm.textContent)
            })
            @Test fun removes a single text node when children are updated() {
                val vnode1 = h("div", "One")
                val vnode2 = h("div", [ h("div", "Two"), h("span", "Three") ])
                patch(vnode0, vnode1)
                assertEquals("One", elm.textContent)
                patch(vnode1, vnode2)
                assert.deepEqual(map(prop("textContent"), elm.childNodes), ["Two", "Three"])
            })
            @Test fun removes a text node among other elements() {
                val vnode1 = h("div", [ "One", h("span", "Two") ])
                val vnode2 = h("div", [ h("div", "Three")])
                patch(vnode0, vnode1)
                assert.deepEqual(map(prop("textContent"), elm.childNodes), ["One", "Two"])
                patch(vnode1, vnode2)
                assertEquals(1, elm.childNodes.length)
                assertEquals("DIV", elm.childNodes[0].tagName)
                assertEquals("Three", elm.childNodes[0].textContent)
            })
            @Test fun reorders elements() {
                val vnode1 = h("div", [h("span", "One"), h("div", "Two"), h("b", "Three")])
                val vnode2 = h("div", [h("b", "Three"), h("span", "One"), h("div", "Two")])
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), ["One", "Two", "Three"])
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(prop("tagName"), elm.children), ["B", "SPAN", "DIV"])
                assert.deepEqual(map(inner, elm.children), ["Three", "One", "Two"])
            })
            @Test fun supports null/undefined children {
                val vnode1 = h("i", [null, h("i", "1"), h("i", "2"), null])
                val vnode2 = h("i", [h("i", "2"), undefined, undefined, h("i", "1"), undefined])
                val vnode3 = h("i", [null, h("i", "1"), undefined, null, h("i", "2"), undefined, null])
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), ["1", "2"])
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), ["2", "1"])
                elm = patch(vnode2, vnode3).elm as Element
                assert.deepEqual(map(inner, elm.children), ["1", "2"])
            })
            @Test fun supports all null/undefined children {
                val vnode1 = h("i", [h("i", "1"), h("i", "2")])
                val vnode2 = h("i", [null, undefined])
                val vnode3 = h("i", [h("i", "2"), h("i", "1")])
                patch(vnode0, vnode1)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals(0, elm.children.length)
                elm = patch(vnode2, vnode3).elm as Element
                assert.deepEqual(map(inner, elm.children), ["2", "1"])
            })
        })
    })
    describe("hooks", {
        describe("element hooks", {
            @Test fun calls `create` listener before inserted into parent but after children {
                var result = [];
                function cb(empty, vnode) {
                assertTrue(vnode.elm instanceof Element)
                assertEquals(2, vnode.elm.children.length)
                assert.strictEqual(vnode.elm.parentNode, null)
                result.push(vnode)
            }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {create: cb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                    h("span", "Can\"t touch me"),
                ])
                patch(vnode0, vnode1)
                assertEquals(result.length, 1)
            })
            @Test fun calls `insert` listener after both parents, siblings and children have been inserted {
                var result = [];
                function cb(vnode) {
                    assertTrue(vnode.elm instanceof Element)
                    assertEquals(2, vnode.elm.children.length)
                    assertEquals(3, vnode.elm.parentNode.children.length)
                    result.push(vnode)
                }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {insert: cb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                    h("span", "Can touch me"),
                ])
                patch(vnode0, vnode1)
                assertEquals(result.length, 1)
            })
            @Test fun calls `prepatch` listener {
                var result = [];
                function cb(oldVnode, vnode) {
                assert.strictEqual(oldVnode, vnode1.children[1])
                assert.strictEqual(vnode, vnode2.children[1])
                result.push(vnode)
            }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {prepatch: cb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                val vnode2 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {prepatch: cb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, result.length)
            })
            @Test fun calls `postpatch` after `prepatch` listener {
                var pre = [], post = [];
                function preCb(oldVnode, vnode) {
                pre.push(pre)
            }
                function postCb(oldVnode, vnode) {
                assertEquals(post.length + 1, pre.length)
                post.push(post)
            }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {prepatch: preCb, postpatch: postCb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                val vnode2 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {prepatch: preCb, postpatch: postCb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, pre.length)
                assertEquals(1, post.length)
            })
            @Test fun calls `update` listener {
                var result1 = [];
                var result2 = [];
                function cb(result, oldVnode, vnode) {
                if (result.length > 0) {
                    console.log(result[result.length-1])
                    console.log(oldVnode)
                    assert.strictEqual(result[result.length-1], oldVnode)
                }
                result.push(vnode)
            }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {update: cb.bind(null, result1)}}, [
                        h("span", "Child 1"),
                        h("span", {hook: {update: cb.bind(null, result2)}}, "Child 2"),
                    ]),
                ])
                val vnode2 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {update: cb.bind(null, result1)}}, [
                        h("span", "Child 1"),
                        h("span", {hook: {update: cb.bind(null, result2)}}, "Child 2"),
                    ]),
                ])
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, result1.length)
                assertEquals(1, result2.length)
            })
            @Test fun calls `remove` listener {
                var result = [];
                function cb(vnode, rm) {
                var parent = vnode.elm.parentNode;
                assertTrue(vnode.elm instanceof Element)
                assertEquals(2, vnode.elm.children.length)
                assertEquals(2, parent.children.length)
                result.push(vnode)
                rm()
                assertEquals(1, parent.children.length)
            }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", {hook: {remove: cb}}, [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                val vnode2 = h("div", [
                    h("span", "First sibling"),
                ])
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(result.length, 1)
            })
            @Test fun calls `destroy` listener when patching text node over node with children {
                var calls = 0;
                function cb(vnode) {
                    calls++;
                }
                val vnode1 = h("div", [
                    h("div", {hook: {destroy: cb}}, [
                        h("span", "Child 1"),
                    ]),
                ])
                val vnode2 = h("div", "Text node")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, calls)
            })
            @Test fun calls `init` and `prepatch` listeners on root {
                var count = 0;
                function in_it(vnode) {
                    assert.strictEqual(vnode, vnode2)
                    count += 1;
                }
                function prepatch(oldVnode, vnode) {
                assert.strictEqual(vnode, vnode1)
                count += 1;
            }
                val vnode1 = h("div", {hook: {init: init, prepatch: prepatch}})
                patch(vnode0, vnode1)
                assertEquals(count, 1)
                val vnode2 = h("span", {hook: {init: init, prepatch: prepatch}})
                patch(vnode1, vnode2)
                assertEquals(count, 2)
            })
            @Test fun removes element when all remove listeners are done() {
                var rm1, rm2, rm3;
                var patch = snabbdom.in_it([
                    {remove: function(_, rm) { rm1 = rm; }},
                    {remove: function(_, rm) { rm2 = rm; }},
                ])
                val vnode1 = h("div", [h("a", {hook: {remove: function(_, rm) { rm3 = rm; }}})])
                val vnode2 = h("div", [])
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(1, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals(1, elm.children.length)
                rm1()
                assertEquals(1, elm.children.length)
                rm3()
                assertEquals(1, elm.children.length)
                rm2()
                assertEquals(0, elm.children.length)
            })
            @Test fun invokes remove hook on replaced root() {
                var result = [];
                var parent = document.createElement("div")
                val vnode0 = document.createElement("div")
                parent.appendChild(vnode0)
                function cb(vnode, rm) {
                result.push(vnode)
                rm()
            }
                val vnode1 = h("div", {hook: {remove: cb}}, [
                    h("b", "Child 1"),
                    h("i", "Child 2"),
                ])
                val vnode2 = h("span", [
                    h("b", "Child 1"),
                    h("i", "Child 2"),
                ])
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(result.length, 1)
            })
        })
        describe("module hooks", {
            @Test fun invokes `pre` and `post` hook {
                var result = [];
                var patch = snabbdom.in_it([
                    {pre: { result.push("pre") }},
                    {post: { result.push("post") }},
                ])
                val vnode1 = h("div")
                patch(vnode0, vnode1)
                assert.deepEqual(result, ["pre", "post"])
            })
            @Test fun invokes global `destroy` hook for all removed children {
                var result = [];
                function cb(vnode) { result.push(vnode) }
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", [
                        h("span", {hook: {destroy: cb}}, "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, result.length)
            })
            @Test fun handles text vnodes with `undefined` `data` property {
                val vnode1 = h("div", [
                    " "
                ])
                val vnode2 = h("div", [])
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
            })
            @Test fun invokes `destroy` module hook for all removed children {
                var created = 0;
                var destroyed = 0;
                var patch = snabbdom.in_it([
                    {create: { created++; }},
                    {destroy: { destroyed++; }},
                ])
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", [
                        h("span", "Child 1"),
                        h("span", "Child 2"),
                    ]),
                ])
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(4, created)
                assertEquals(4, destroyed)
            })
            @Test fun does not invoke `create` and `remove` module hook for text nodes {
                var created = 0;
                var removed = 0;
                var patch = snabbdom.in_it([
                    {create: { created++; }},
                    {remove: { removed++; }},
                ])
                val vnode1 = h("div", [
                    h("span", "First child"),
                    "",
                    h("span", "Third child"),
                ])
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(2, created)
                assertEquals(2, removed)
            })
            @Test fun does not invoke `destroy` module hook for text nodes {
                var created = 0;
                var destroyed = 0;
                var patch = snabbdom.in_it([
                    {create: { created++; }},
                    {destroy: { destroyed++; }},
                ])
                val vnode1 = h("div", [
                    h("span", "First sibling"),
                    h("div", [
                        h("span", "Child 1"),
                        h("span", ["Text 1", "Text 2"]),
                    ]),
                ])
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(4, created)
                assertEquals(4, destroyed)
            })
        })
    })
    describe("short circuiting", {
        @Test fun does not update strictly equal vnodes() {
            var result = [];
            function cb(vnode) { result.push(vnode) }
            val vnode1 = h("div", [
                h("span", {hook: {update: cb}}, "Hello"),
                h("span", "there"),
            ])
            patch(vnode0, vnode1)
            patch(vnode1, vnode1)
            assertEquals(0, result.length)
        })
        @Test fun does not update strictly equal children() {
            var result = [];
            function cb(vnode) { result.push(vnode) }
            val vnode1 = h("div", [
                h("span", {hook: {patch: cb}}, "Hello"),
                h("span", "there"),
            ])
            val vnode2 = h("div")
            vnode2.children = vnode1.children;
            patch(vnode0, vnode1)
            patch(vnode1, vnode2)
            assertEquals(0, result.length)
        })
    })*/
}