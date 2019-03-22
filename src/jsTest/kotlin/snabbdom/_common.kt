package snabbdom

import org.w3c.dom.Element
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window
import kotlin.test.BeforeTest

val assert = window["chai"].unsafeCast<dynamic>().assert

abstract class TestBase {
    lateinit var elm: Element
    lateinit var vnode0: Element

    @BeforeTest
    fun beforeEach() {
        elm = document.createElement("div")
        vnode0 = elm
    }
}