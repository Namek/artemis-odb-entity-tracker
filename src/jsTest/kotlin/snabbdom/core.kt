@file:Suppress("MoveLambdaOutsideParentheses")

package snabbdom

import org.w3c.dom.*
import snabbdom.modules.*
import kotlin.browser.*
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.*

private val patch = Snabbdom.init(
    arrayOf(ClassModule(), PropsModule(), EventListenersModule())
)

fun prop(name: String): (obj: dynamic) -> dynamic {
    return { obj ->
        obj[name]
    }
}

fun map(fn: (dynamic) -> dynamic, list: ItemArrayLike<dynamic>): Array<dynamic> =
    list.asList().map(fn).toTypedArray()

val inner = prop("innerHTML")


private class Core : TestBase() {
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
            val vnode = h("a", arrayOf("I am a string"))
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
                var nextVNode = h("div#id.class", arrayOf(h("span", "Hi")))
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
                    h("div#id.class", arrayOf(h("span", "Hi")))
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
                var nextVNode = h("div#id.class", ["Foobar"))
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
                var nextVNode = h("div#id.class", arrayOf(h("h2", "Hello")))
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
                    val vnode1 = h_("span", spanNums(arrayOf(1)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(1, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("2", elm.children[1]!!.innerHTML)
                    assertEquals("3", elm.children[2]!!.innerHTML)
                }
                @Test fun prepends_elements() {
                    val vnode1 = h_("span", spanNums(arrayOf(4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(2, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3", "4", "5"))
                }
                @Test fun add_elements_in_the_middle() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3", "4", "5"))
                }
                @Test fun add_elements_at_begin_and_end() {
                    val vnode1 = h_("span", spanNums(arrayOf(2, 3, 4)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3", "4", "5"))
                }
                @Test fun adds_children_to_parent_with_no_children() {
                    val vnode1 = h("span", VNodeData(key = "span"))
                    val vnode2 = h_("span", VNodeData(key = "span"), spanNums(arrayOf(1, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(0, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3"))
                }
                @Test fun removes_all_children_from_parent() {
                    val vnode1 = h_("span", VNodeData(key = "span"), spanNums(arrayOf(1, 2, 3)))
                    val vnode2 = h("span", VNodeData(key = "span"))
                    elm = patch(vnode0, vnode1).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3"))
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(0, elm.children.length)
                }
                @Test fun update_one_child_with_same_key_but_different_sel() {
                    val vnode1 = h_("span", VNodeData(key = "span"), spanNums(arrayOf(1, 2, 3)))
                    val vnode2 = h_("span", VNodeData(key = "span"), arrayOf(spanNum(1), h("i", VNodeData(key = 2), "2"), spanNum(3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3"))
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("1", "2", "3"))
                    assertEquals(3, elm.children.length)
                    assertEquals("I", elm.children[1]!!.tagName)
                }
            }

            class removal_of_elements : TestBase() {
                @Test fun removes_elements_from_the_beginning() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(3, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("3", "4", "5"))
                }
                @Test fun removes_elements_from_the_end() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("1", elm.children[0]!!.innerHTML)
                    assertEquals("2", elm.children[1]!!.innerHTML)
                    assertEquals("3", elm.children[2]!!.innerHTML)
                }
                @Test fun removes_elements_from_the_middle() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 2, 4, 5)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assert.deepEqual(elm.children[0]!!.innerHTML, "1")
                    assertEquals("1", elm.children[0]!!.innerHTML)
                    assertEquals("2", elm.children[1]!!.innerHTML)
                    assertEquals("4", elm.children[2]!!.innerHTML)
                    assertEquals("5", elm.children[3]!!.innerHTML)
                }
            }

            class element_reordering : TestBase() {
                @Test fun moves_element_forward() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4)))
                    val vnode2 = h_("span", spanNums(arrayOf(2, 3, 1, 4)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals("2", elm.children[0]!!.innerHTML)
                    assertEquals("3", elm.children[1]!!.innerHTML)
                    assertEquals("1", elm.children[2]!!.innerHTML)
                    assertEquals("4", elm.children[3]!!.innerHTML)
                }
                @Test fun moves_element_to_end() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3)))
                    val vnode2 = h_("span", spanNums(arrayOf(2, 3, 1)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("2", elm.children[0]!!.innerHTML)
                    assertEquals("3", elm.children[1]!!.innerHTML)
                    assertEquals("1", elm.children[2]!!.innerHTML)
                }
                @Test fun moves_element_backwards() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4)))
                    val vnode2 = h_("span", spanNums(arrayOf(1, 4, 2, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals("1", elm.children[0]!!.innerHTML)
                    assertEquals("4", elm.children[1]!!.innerHTML)
                    assertEquals("2", elm.children[2]!!.innerHTML)
                    assertEquals("3", elm.children[3]!!.innerHTML)
                }
                @Test fun swaps_first_and_last() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4)))
                    val vnode2 = h_("span", spanNums(arrayOf(4, 2, 3, 1)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(4, elm.children.length)
                    assertEquals("4", elm.children[0]!!.innerHTML)
                    assertEquals("2", elm.children[1]!!.innerHTML)
                    assertEquals("3", elm.children[2]!!.innerHTML)
                    assertEquals("1", elm.children[3]!!.innerHTML)
                }
            }

            class combinations_of_additions_and_removals_and_reorderings : TestBase() {
                @Test fun move_to_left_and_replace() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(4, 1, 2, 3, 6)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(5, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(5, elm.children.length)
                    assertEquals("4", elm.children[0]!!.innerHTML)
                    assertEquals("1", elm.children[1]!!.innerHTML)
                    assertEquals("2", elm.children[2]!!.innerHTML)
                    assertEquals("3", elm.children[3]!!.innerHTML)
                    assertEquals("6", elm.children[4]!!.innerHTML)
                }
                @Test fun moves_to_left_and_leaves_hole() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(4, 6)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assert.deepEqual(map(inner, elm.children), arrayOf("4", "6"))
                }
                @Test fun handles_moved_and_set_to_undefined_element_ending_at_the_end() {
                    val vnode1 = h_("span", spanNums(arrayOf(2, 4, 5)))
                    val vnode2 = h_("span", spanNums(arrayOf(4, 5, 3)))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(3, elm.children.length)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(3, elm.children.length)
                    assertEquals("4", elm.children[0]!!.innerHTML)
                    assertEquals("5", elm.children[1]!!.innerHTML)
                    assertEquals("3", elm.children[2]!!.innerHTML)
                }
                @Test fun moves_a_key_in_non_keyed_nodes_with_a_size_up() {
                    val vnode1 = h_("span", spanNums(arrayOf(1, "a", "b", "c")))
                    val vnode2 = h_("span", spanNums(arrayOf("d", "a", "b", "c", 1, "e")))
                    elm = patch(vnode0, vnode1).elm as Element
                    assertEquals(4, elm.childNodes.length)
                    assertEquals("1abc", elm.textContent)
                    elm = patch(vnode1, vnode2).elm as Element
                    assertEquals(6, elm.childNodes.length)
                    assertEquals("dabc1e", elm.textContent)
                }
            }

            @Test fun reverses_elements() {
                val vnode1 = h_("span", spanNums(arrayOf(1, 2, 3, 4, 5, 6, 7, 8)))
                val vnode2 = h_("span", spanNums(arrayOf(8, 7, 6, 5, 4, 3, 2, 1)))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(8, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("8", "7", "6", "5", "4", "3", "2", "1"))
            }
            @Test fun something() {
                val vnode1 = h_("span", spanNums(arrayOf(0, 1, 2, 3, 4, 5)))
                val vnode2 = h_("span", spanNums(arrayOf(4, 3, 2, 1, 5, 0)))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(6, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("4", "3", "2", "1", "5", "0"))
            }
            @Test fun handles_random_shuffles() {
                val elms = 14
                val samples = 4

                fun spanNumWithOpacity(n: Int, o: String): VNode =
                    h("span", VNodeData(key = n, style = j("opacity" to o)), text = n.toString())

                val arr = Array(elms) {n -> n}
                val opacities = Array(elms) {n -> "0"}

                for (n in 0 until samples) {
                    val vnode1 = h_("span", arr.map {n -> spanNumWithOpacity(n, "1")}.toTypedArray())
                    var shufArr = arr.asIterable().shuffled()
                    var elm = document.createElement("div")
                    elm = patch(elm, vnode1).elm as Element
                    for (i in 0 until elms) {
                        assertEquals(elm.children[i]!!.innerHTML, i.toString())
                        opacities[i] = ((Random.nextFloat()*100000).roundToInt() / 100000f).toString()
                    }
                    val vnode2 = h("span", Array(elms) {n -> spanNumWithOpacity(shufArr[n], opacities[n])})
                    elm = patch(vnode1, vnode2).elm as Element
                    for (i in 0 until elms) {
                        assertEquals(elm.children[i]!!.innerHTML, shufArr[i].toString())
                        assertEquals(0, opacities[i].indexOf((elm.children[i]!! as HTMLElement).style.opacity))
                    }
                }
            }
            @Test fun supports_null_or_undefined_children() {
                val vnode1 = h_("i", spanNums(arrayOf(0, 1, 2, 3, 4, 5)))
                val vnode2 = h_("i", spanNums(arrayOf(null, 2, undefined, null, 1, 0, null, 5, 4, null, 3, undefined)))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals(6, elm.children.length)
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("2", "1", "0", "5", "4", "3"))
            }
            @Test fun supports_all_null_or_undefined_children() {
                val vnode1 = h_("i", spanNums(arrayOf(0, 1, 2, 3, 4, 5)))
                val vnode2 = h_("i", arrayOf(null, null, undefined, null, null, undefined))
                val vnode3 = h_("i", spanNums(arrayOf(5, 4, 3, 2, 1, 0)))
                patch(vnode0, vnode1)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals(0, elm.children.length)
                elm = patch(vnode2, vnode3).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("5", "4", "3", "2", "1", "0"))
            }
            @Test fun handles_random_shuffles_with_null_or_undefined_children() {
                val maxArrLen = 15
                val samples = 5
                var vnode1: VNode? = null
                lateinit var vnode2: VNode

                for (i in 0 until samples) {
                    val len = Random.nextInt(maxArrLen + 1)
                    val arr = Array(len) { j ->
                        if (Random.nextBoolean()) j.toString()
                        else if (Random.nextFloat() < 0.75) null
                        else undefined
                    }.asIterable().shuffled().toTypedArray<dynamic>()

                    vnode2 = h_("div", arr.map(::spanNum).toTypedArray())
                    elm = patch(if (vnode1 == null) vnode0 else vnode1, vnode2).elm as Element
                    assert.deepEqual(
                        map(inner, elm.children),
                        arr.filter {x -> x != null}.toTypedArray()
                    )

                    vnode1 = vnode2
                }
            }
        }

        object updating_children_without_keys : TestBase() {
            @Test fun appends_elements() {
                val vnode1 = h("div", arrayOf(h("span", "Hello")))
                val vnode2 = h("div", arrayOf(h("span", "Hello"), h("span", "World")))
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("Hello"))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("Hello", "World"))
            }
            @Test fun handles_unmoved_text_nodes() {
                val vnode1 = h_("div", arrayOf("Text", h("span", "Span")))
                val vnode2 = h_("div", arrayOf("Text", h("span", "Span")))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0]!!.textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text", elm.childNodes[0]!!.textContent)
            }
            @Test fun handles_changing_text_children() {
                val vnode1 = h_("div", arrayOf("Text", h("span", "Span")))
                val vnode2 = h_("div", arrayOf("Text2", h("span", "Span")))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0]!!.textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text2", elm.childNodes[0]!!.textContent)
            }
            @Test fun handles_unmoved_comment_nodes() {
                val vnode1 = h("div", arrayOf(h("!", "Text"), h("span", "Span")))
                val vnode2 = h("div", arrayOf(h("!", "Text"), h("span", "Span")))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0]!!.textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text", elm.childNodes[0]!!.textContent)
            }
            @Test fun handles_changing_comment_text() {
                val vnode1 = h("div", arrayOf(h("!", "Text"), h("span", "Span")))
                val vnode2 = h("div", arrayOf(h("!", "Text2"), h("span", "Span")))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("Text", elm.childNodes[0]!!.textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Text2", elm.childNodes[0]!!.textContent)
            }
            @Test fun handles_changing_empty_comment() {
                val vnode1 = h("div", arrayOf(h("!"), h("span", "Span")))
                val vnode2 = h("div", arrayOf(h("!", "Test"), h("span", "Span")))
                elm = patch(vnode0, vnode1).elm as Element
                assertEquals("", elm.childNodes[0]!!.textContent)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals("Test", elm.childNodes[0]!!.textContent)
            }
            @Test fun prepends_element() {
                val vnode1 = h("div", arrayOf(h("span", "World")))
                val vnode2 = h("div", arrayOf(h("span", "Hello"), h("span", "World")))
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("World"))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("Hello", "World"))
            }
            @Test fun prepends_element_of_different_tag_type() {
                val vnode1 = h("div", arrayOf(h("span", "World")))
                val vnode2 = h("div", arrayOf(h("div", "Hello"), h("span", "World")))
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("World"))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(prop("tagName"), elm.children), arrayOf("DIV", "SPAN"))
                assert.deepEqual(map(inner, elm.children), arrayOf("Hello", "World"))
            }
            @Test fun removes_elements() {
                val vnode1 = h("div", arrayOf(h("span", "One"), h("span", "Two"), h("span", "Three")))
                val vnode2 = h("div", arrayOf(h("span", "One"), h("span", "Three")))
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("One", "Two", "Three"))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("One", "Three"))
            }
            @Test fun removes_a_single_text_node() {
                val vnode1 = h("div", "One")
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                assertEquals("One", elm.textContent)
                patch(vnode1, vnode2)
                assertEquals("", elm.textContent)
            }
            @Test fun removes_a_single_text_node_when_children_are_updated() {
                val vnode1 = h("div", "One")
                val vnode2 = h("div", arrayOf( h("div", "Two"), h("span", "Three") ))
                patch(vnode0, vnode1)
                assertEquals("One", elm.textContent)
                patch(vnode1, vnode2)
                assert.deepEqual(map(prop("textContent"), elm.childNodes), arrayOf("Two", "Three"))
            }
            @Test fun removes_a_text_node_among_other_elements() {
                val vnode1 = h_("div", arrayOf( "One", h("span", "Two") ))
                val vnode2 = h_("div", arrayOf( h("div", "Three")))
                patch(vnode0, vnode1)
                assert.deepEqual(map(prop("textContent"), elm.childNodes), arrayOf("One", "Two"))
                patch(vnode1, vnode2)
                assertEquals(1, elm.childNodes.length)
                assertEquals("DIV", (elm.childNodes[0]!! as Element).tagName)
                assertEquals("Three", (elm.childNodes[0]!! as Element).textContent)
            }
            @Test fun reorders_elements() {
                val vnode1 = h_("div", arrayOf(h("span", "One"), h("div", "Two"), h("b", "Three")))
                val vnode2 = h_("div", arrayOf(h("b", "Three"), h("span", "One"), h("div", "Two")))
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("One", "Two", "Three"))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(prop("tagName"), elm.children), arrayOf("B", "SPAN", "DIV"))
                assert.deepEqual(map(inner, elm.children), arrayOf("Three", "One", "Two"))
            }
            @Test fun supports_null_or_undefined_children() {
                val vnode1 = h_("i", arrayOf(null, h("i", "1"), h("i", "2"), null))
                val vnode2 = h_("i", arrayOf(h("i", "2"), undefined, undefined, h("i", "1"), undefined))
                val vnode3 = h_("i", arrayOf(null, h("i", "1"), undefined, null, h("i", "2"), undefined, null))
                elm = patch(vnode0, vnode1).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("1", "2"))
                elm = patch(vnode1, vnode2).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("2", "1"))
                elm = patch(vnode2, vnode3).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("1", "2"))
            }
            @Test fun supports_all_null_or_undefined_children() {
                val vnode1 = h_("i", arrayOf(h("i", "1"), h("i", "2")))
                val vnode2 = h_("i", arrayOf(null, undefined))
                val vnode3 = h_("i", arrayOf(h("i", "2"), h("i", "1")))
                patch(vnode0, vnode1)
                elm = patch(vnode1, vnode2).elm as Element
                assertEquals(0, elm.children.length)
                elm = patch(vnode2, vnode3).elm as Element
                assert.deepEqual(map(inner, elm.children), arrayOf("2", "1"))
            }
        }
    }

    class hooks {
        class element_hooks : TestBase() {
            @Test fun calls_create_listener_before_inserted_into_parent_but_after_children() {
                val result = mutableListOf<dynamic>()
                val cb = { empty: dynamic, vnode: VNode ->
                    assertTrue(vnode.elm is Element)
                    assertEquals(2, (vnode.elm as Element).children.length)
                    assert.strictEqual((vnode.elm as Element).parentNode, null)
                    result.add(vnode)
                }
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j<dynamic>("create" to cb).unsafeCast<Hooks>()), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    )),
                    h("span", "Can\"t touch me")
                ))
                patch(vnode0, vnode1)
                assertEquals(result.size, 1)
            }
            @Test fun calls_insert_listener_after_both_parents_and_siblings_and_children_have_been_inserted() {
                val result = mutableListOf<dynamic>()
                val cb = { vnode: VNode ->
                    assertTrue(vnode.elm is Element)
                    assertEquals(2, (vnode.elm as Element).children.length)
                    assertEquals(3, ((vnode.elm as Element).parentNode as Element).children.length)
                    result.add(vnode)
                }
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("insert" to cb as InsertHook)), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    )),
                    h("span", "Can touch me")
                ))
                patch(vnode0, vnode1)
                assertEquals(result.size, 1)
            }
            @Test fun calls_prepatch_listener() {
                val result = mutableListOf<VNode>()
                lateinit var vnode1: VNode
                lateinit var vnode2: VNode

                val cb = { oldVnode: VNode, vnode: VNode ->
                    assert.strictEqual(oldVnode, vnode1.children!![1])
                    assert.strictEqual(vnode, vnode2.children!![1])
                    result.add(vnode)
                }
                vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("prepatch" to cb)), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                vnode2 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("prepatch" to cb)), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, result.size)
            }
            @Test fun calls_postpatch_after_prepatch_listener() {
                val pre = mutableListOf<dynamic>()
                val post = mutableListOf<dynamic>()
                val preCb = {oldVnode: VNode, vnode: VNode ->
                    pre.add(pre)
                }
                val postCb = {oldVnode: VNode, vnode: VNode ->
                    assertEquals(post.size + 1, pre.size)
                    post.add(post)
                }
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("prepatch" to preCb, "postpatch" to postCb)), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                val vnode2 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("prepatch" to preCb, "postpatch" to postCb)), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, pre.size)
                assertEquals(1, post.size)
            }
            @Test fun calls_update_listener() {
                val result1 = mutableListOf<VNode>()
                val result2 = mutableListOf<VNode>()
                val cb = { result: MutableList<VNode> ->
                    { oldVnode: VNode, vnode: VNode ->
                        if (result.size > 0) {
                            console.log(result[result.size - 1])
                            console.log(oldVnode)
                            assert.strictEqual(result[result.size - 1], oldVnode)
                        }
                        result.add(vnode)
                    }
                }
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("update" to cb(result1))), arrayOf(
                        h("span", "Child 1"),
                        h("span", VNodeData(hook = j("update" to cb(result2))), "Child 2")
                    ))
                ))
                val vnode2 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("update" to cb(result1))), arrayOf(
                        h("span", "Child 1"),
                        h("span", VNodeData(hook = j("update" to cb(result2))), "Child 2")
                    ))
                ))
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, result1.size)
                assertEquals(1, result2.size)
            }
            @Test fun calls_remove_listener() {
                val result = mutableListOf<VNode>()
                val cb = { vnode: VNode, rm: dynamic ->
                    val parent = vnode.elm!!.parentNode as Element
                    assertTrue(vnode.elm is Element)
                    assertEquals(2, (vnode.elm as Element).children.length)
                    assertEquals(2, parent.children.length)
                    result.add(vnode)
                    rm()
                    assertEquals(1, parent.children.length)
                }
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", VNodeData(hook = j("remove" to cb)), arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                val vnode2 = h("div", arrayOf(
                    h("span", "First sibling")
                ))
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(result.size, 1)
            }
            @Test fun calls_destroy_listener_when_patching_text_node_over_node_with_children() {
                var calls = 0
                val cb = {vnode: VNode ->
                    calls++
                }
                val vnode1 = h("div", arrayOf(
                    h("div", VNodeData(hook =  j("destroy" to cb)), arrayOf(
                        h("span", "Child 1")
                    ))
                ))
                val vnode2 = h("div", "Text node")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, calls)
            }
            @Test fun calls_init_and_prepatch_listeners_on_root() {
                var count = 0
                lateinit var vnode1: VNode
                lateinit var vnode2: VNode
                val init = { vnode: VNode ->
                    assert.strictEqual(vnode, vnode2)
                    count += 1
                }
                val prepatch = { oldVnode: VNode, vnode: VNode ->
                    assert.strictEqual(vnode, vnode1)
                    count += 1
                }
                vnode1 = h("div", VNodeData(hook = j("init" to init, "prepatch" to prepatch)))
                patch(vnode0, vnode1)
                assertEquals(count, 1)
                vnode2 = h("span", VNodeData(hook = j("init" to init, "prepatch" to prepatch)))
                patch(vnode1, vnode2)
                assertEquals(count, 2)
            }
            @Test fun removes_element_when_all_remove_listeners_are_done() {
                lateinit var rm1: () -> Unit
                lateinit var rm2 : () -> Unit
                lateinit var rm3 : () -> Unit
                val patch = Snabbdom.init(arrayOf(
                    Module(remove = { _: VNode, rm: () -> Unit -> rm1 = rm; undefined }),
                    Module(remove = { _: VNode, rm: () -> Unit -> rm2 = rm } as RemoveHook)
                ))
                val vnode1 = h("div", arrayOf(h("a", VNodeData(hook = j("remove" to { _: VNode, rm: () -> Unit -> rm3 = rm } as RemoveHook)))))
                val vnode2 = h("div", arrayOf<VNode>())
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
            }
            @Test fun invokes_remove_hook_on_replaced_root() {
                val result = mutableListOf<VNode>()
                val parent = document.createElement("div")
                val vnode0 = document.createElement("div")
                parent.appendChild(vnode0)
                val cb = {vnode: VNode, rm: dynamic ->
                    result.add(vnode)
                    rm()
                }
                val vnode1 = h("div", VNodeData(hook = j("remove" to cb)), arrayOf(
                    h("b", "Child 1"),
                    h("i", "Child 2")
                ))
                val vnode2 = h("span", arrayOf(
                    h("b", "Child 1"),
                    h("i", "Child 2")
                ))
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(result.size, 1)
            }
        }
        class module_hooks : TestBase() {
            @Test fun invokes_pre_and_post_hook() {
                val result = mutableListOf<String>()
                val patch = Snabbdom.init(arrayOf(
                    Module(pre = { result.add("pre") }),
                    Module(post = { result.add("post") })
                ))
                val vnode1 = h("div")
                patch(vnode0, vnode1)
                assert.deepEqual(result.toTypedArray(), arrayOf("pre", "post"))
            }
            @Test fun invokes_global_destroy_hook_for_all_removed_children() {
                val result = mutableListOf<VNode>()
                val cb = { vnode: VNode -> result.add(vnode) }
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", arrayOf(
                        h("span", VNodeData(hook = j("destroy" to cb)), "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(1, result.size)
            }
            @Test fun handles_text_vnodes_with_undefined_data_property() {
                val vnode1 = h("div", arrayOf(
                    " "
                ))
                val vnode2 = h("div", arrayOf<VNode>())
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
            }
            @Test fun invokes_destroy_module_hook_for_all_removed_children() {
                var created = 0
                var destroyed = 0
                val patch = Snabbdom.init(arrayOf(
                    Module(create = { created++ } as CreateHook),
                    Module(destroy = { destroyed++ })
                ))
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                    h("div", arrayOf(
                        h("span", "Child 1"),
                        h("span", "Child 2")
                    ))
                ))
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(4, created)
                assertEquals(4, destroyed)
            }
            @Test fun does_not_invoke_create_and_remove_module_hook_for_text_nodes() {
                var created = 0
                var removed = 0
                val patch = Snabbdom.init(arrayOf(
                    Module(create = { _, _ -> created++ }),
                    Module(remove = { _, _ -> removed++ })
                ))
                val vnode1 = h_("div", arrayOf(
                    h("span", "First child"),
                    "",
                    h("span", "Third child")
                ))
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(2, created)
                assertEquals(2, removed)
            }
            @Test fun does_not_invoke_destroy_module_hook_for_text_nodes() {
                var created = 0
                var destroyed = 0
                val patch = Snabbdom.init(arrayOf(
                    Module(create = { _, _ -> created++ }),
                    Module(destroy = { destroyed++; })
                ))
                val vnode1 = h("div", arrayOf(
                    h("span", "First sibling"),
                        h("div", arrayOf(
                            h("span", "Child 1"),
                            h("span", arrayOf("Text 1", "Text 2"))
                        ))
                    ))
                val vnode2 = h("div")
                patch(vnode0, vnode1)
                patch(vnode1, vnode2)
                assertEquals(4, created)
                assertEquals(4, destroyed)
            }
        }
    }
    class short_circuiting : TestBase() {
        @Test fun does_not_update_strictly_equal_vnodes() {
            val result = mutableListOf<VNode>()
            val cb = { vnode: VNode -> result.add(vnode) }
            val vnode1 = h("div", arrayOf(
                h("span", VNodeData(hook = j("update" to cb)), "Hello"),
                h("span", "there")
            ))
            patch(vnode0, vnode1)
            patch(vnode1, vnode1)
            assertEquals(0, result.size)
        }
        @Test fun does_not_update_strictly_equal_children() {
            val result = mutableListOf<VNode>()
            val cb = { vnode: VNode -> result.add(vnode) }
            val vnode1 = h("div", arrayOf(
                h("span", VNodeData(hook = j("patch" to cb)), "Hello"),
                h("span", "there")
            ))
            val vnode2 = h("div")
            vnode2.children = vnode1.children
            patch(vnode0, vnode1)
            patch(vnode1, vnode2)
            assertEquals(0, result.size)
        }
    }
}