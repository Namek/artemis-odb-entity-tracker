package snabbdom

import org.w3c.dom.Element
import kotlin.test.Test

private val patch = Snabbdom.init(arrayOf())

private class Thunk : TestBase() {
    @Test fun returns_vnode_with_data_and_render_function() {
        val numberInSpan = ThunkFn.args1 { n: Int ->
            h("span", "Number is " + n)
        }

        val vnode = thunk("span", "num", numberInSpan, arrayOf(22))
        assert.deepEqual(vnode.sel, "span")
        assert.deepEqual(vnode.data!!.key, "num")
        assert.deepEqual(vnode.data!!.args, arrayOf(22))
    }

    @Test fun calls_render_function_once_on_data_change() {
        var called = 0
        val numberInSpan = ThunkFn.args1 { n: Int ->
          called++
          h("span", VNodeData(key = "num"), "Number is " + n)
        }
        val vnode1 = h("div", arrayOf(
          thunk("span", "num", numberInSpan, arrayOf(1))
        ))
        val vnode2 = h("div", arrayOf(
          thunk("span", "num", numberInSpan, arrayOf(2))
        ))
        patch(vnode0, vnode1)
        assert.equal(called, 1)
        patch(vnode1, vnode2)
        assert.equal(called, 2)
  }
    
  @Test fun does_not_call_render_function_on_data_unchanged() {
    var called = 0
    val numberInSpan = ThunkFn.args1 { n: Int ->
      called++
      h("span", VNodeData(key = "num"), "Number is " + n)
    }
    val vnode1 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1))
    ))
    val vnode2 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1))
    ))
    patch(vnode0, vnode1)
    assert.equal(called, 1)
    patch(vnode1, vnode2)
    assert.equal(called, 1)
  }

  @Test fun calls_render_function_once_on_data_length_change() {
    var called = 0
    val numberInSpan = ThunkFn.args1 { n: Int ->
      called++
      h("span", VNodeData(key = "num"), "Number is " + n)
    }
    val vnode1 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1))
    ))
    val vnode2 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1, 2))
    ))
    patch(vnode0, vnode1)
    assert.equal(called, 1)
    patch(vnode1, vnode2)
    assert.equal(called, 2)
  }

  @Test fun calls_render_val_once_on_val_change() {
    var called = 0
    val numberInSpan = ThunkFn.args1 { n: Int ->
      called++
      h("span", VNodeData(key = "num"), "Number is " + n)
    }
    val numberInSpan2 = ThunkFn.args1 { n: Int ->
      called++
      h("span", VNodeData(key = "num"), "Number really is " + n)
    }
    val vnode1 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1))
    ))
    val vnode2 = h("div", arrayOf(
      thunk("span", "num", numberInSpan2, arrayOf(1))
    ))
    patch(vnode0, vnode1)
    assert.equal(called, 1)
    patch(vnode1, vnode2)
    assert.equal(called, 2)
  }

  @Test fun renders_correctly() {
    var called = 0
    val numberInSpan = ThunkFn.args1 { n: Int ->
      called++
      h("span", VNodeData(key = "num"), "Number is " + n)
    }
    val vnode1 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1))
    ))
    val vnode2 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(1))
    ))
    val vnode3 = h("div", arrayOf(
      thunk("span", "num", numberInSpan, arrayOf(2))
    ))
    elm = patch(vnode0, vnode1).elm as Element
    assert.equal((elm.firstChild as Element).tagName.toLowerCase(), "span")
    assert.equal((elm.firstChild as Element).innerHTML, "Number is 1")
    elm = patch(vnode1, vnode2).elm as Element
    assert.equal((elm.firstChild as Element).tagName.toLowerCase(), "span")
    assert.equal((elm.firstChild as Element).innerHTML, "Number is 1")
    elm = patch(vnode2, vnode3).elm as Element
    assert.equal((elm.firstChild as Element).tagName.toLowerCase(), "span")
    assert.equal((elm.firstChild as Element).innerHTML, "Number is 2")
    assert.equal(called, 2)
  }
  @Test fun supports_leaving_out_the__key__argument() {
    val vnodeFn = ThunkFn.args1 { s: String ->
      h("span.number", "Hello " + s)
    }
    val vnode1 = thunk("span.number", vnodeFn, arrayOf("World!"))
    elm = patch(vnode0, vnode1).elm as Element
    assert.equal(elm.asDynamic().innerText, "Hello World!")
  }

  @Test fun renders_correctly_when_root() {
    var called = 0
    val numberInSpan = ThunkFn.args1 { n: Int ->
      called++
      h("span", VNodeData(key = "num"), "Number is " + n)
    }
    val vnode1 = thunk("span", "num", numberInSpan, arrayOf(1))
    val vnode2 = thunk("span", "num", numberInSpan, arrayOf(1))
    val vnode3 = thunk("span", "num", numberInSpan, arrayOf(2))

    elm = patch(vnode0, vnode1).elm as Element
    assert.equal(elm.tagName.toLowerCase(), "span")
    assert.equal(elm.innerHTML, "Number is 1")

    elm = patch(vnode1, vnode2).elm as Element
    assert.equal(elm.tagName.toLowerCase(), "span")
    assert.equal(elm.innerHTML, "Number is 1")

    elm = patch(vnode2, vnode3).elm as Element
    assert.equal(elm.tagName.toLowerCase(), "span")
    assert.equal(elm.innerHTML, "Number is 2")
    assert.equal(called, 2)
  }

  @Test fun can_be_replaced_and_removed() {
    val numberInSpan = ThunkFn.args1 { n ->
      h("span", VNodeData(key = "num"), "Number is " + n)
    }

    lateinit var oddEven: ThunkFn
    oddEven = ThunkFn.args1 { n ->
      val prefix = if ((n % 2) === 0) "Even" else "Odd"
      h("div", VNodeData(key = (oddEven).asDynamic()), prefix + ": " + n)
    }

    val vnode1 = h("div", arrayOf(thunk("span", "num", numberInSpan, arrayOf(1))))
    val vnode2 = h("div", arrayOf(thunk("div", "oddEven", oddEven, arrayOf(4))))

    elm = patch(vnode0, vnode1).elm as Element
    assert.equal((elm.firstChild as Element).tagName.toLowerCase(), "span")
    assert.equal((elm.firstChild as Element).innerHTML, "Number is 1")

    elm = patch(vnode1, vnode2).elm as Element
    assert.equal((elm.firstChild as Element).tagName.toLowerCase(), "div")
    assert.equal((elm.firstChild as Element).innerHTML, "Even: 4")
  }

  @Test fun can_be_replaced_and_removed_when_root() {
    val numberInSpan = ThunkFn.args1 { n ->
      h("span", VNodeData(key = "num"), "Number is " + n)
    }
    lateinit var oddEven: ThunkFn
    oddEven = ThunkFn.args1 {n ->
      val prefix = if ((n % 2) === 0) "Even" else "Odd"
      h("div", VNodeData(key = oddEven), prefix + ": " + n)
    }
    val vnode1 = thunk("span", "num", numberInSpan, arrayOf(1))
    val vnode2 = thunk("div", "oddEven", oddEven, arrayOf(4))

    elm = patch(vnode0, vnode1).elm as Element
    assert.equal(elm.tagName.toLowerCase(), "span")
    assert.equal(elm.innerHTML, "Number is 1")

    elm = patch(vnode1, vnode2).elm as Element
    assert.equal(elm.tagName.toLowerCase(), "div")
    assert.equal(elm.innerHTML, "Even: 4")
  }

  @Test fun invokes_destroy_hook_on_thunks() {
    var called = 0
    val destroyHook: DestroyHook = { _ ->
      called++
    }
    val numberInSpan = ThunkFn.args1 { n ->
      val hooks: Hooks = j()
      hooks.destroy = destroyHook
      h("span", VNodeData(key = "num", hook = hooks), "Number is " + n)
    }
    val vnode1 = h("div", arrayOf(
      h("div", "Foo"),
      thunk("span", "num", numberInSpan, arrayOf(1)),
      h("div", "Foo")
    ))
    val vnode2 = h("div", arrayOf(
      h("div", "Foo"),
      h("div", "Foo")
    ))
    patch(vnode0, vnode1)
    patch(vnode1, vnode2)
    assert.equal(called, 1)
  }

  @Test fun invokes_remove_hook_on_thunks() {
    var called = 0
    val hook: RemoveHook = { _, _ ->
      called++
    }
    val numberInSpan = ThunkFn.args1 { n ->
      val hooks: Hooks = j()
      hooks.remove = hook
      h("span", VNodeData(key = "num", hook = hooks), "Number is " + n)
    }
    val vnode1 = h("div", arrayOf(
      h("div", "Foo"),
      thunk("span", "num", numberInSpan, arrayOf(1)),
      h("div", "Foo")
    ))
    val vnode2 = h("div", arrayOf(
      h("div", "Foo"),
      h("div", "Foo")
    ))
    patch(vnode0, vnode1)
    patch(vnode1, vnode2)
    assert.equal(called, 1)
  }
}