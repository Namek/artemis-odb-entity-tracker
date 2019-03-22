package snabbdom.modules

import org.w3c.dom.*
import org.w3c.dom.css.CSSStyleDeclaration
import snabbdom.*
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Json
import kotlin.math.max

//export type Hero = { id: string }
interface Hero : Json {
    val id: String?
}


private fun getTextNodeRect(textNode: Text): DOMRect? {
    var rect: DOMRect? = null
    if (document::createRange != undefined) {
        val range = document.createRange()
        range.selectNodeContents(textNode)
        if (Range::getBoundingClientRect != undefined) {
            rect = range.getBoundingClientRect()
        }
    }
    return rect
}

private fun calcTransformOrigin(isTextNode: Boolean, textRect: DOMRect?, boundingRect: DOMRect): String {
    if (isTextNode) {
        if (textRect != null) {
            //calculate pixels to center of text from left edge of bounding box
            var relativeCenterX = textRect.left + textRect.width/2 - boundingRect.left
            var relativeCenterY = textRect.top + textRect.height/2 - boundingRect.top
            return relativeCenterX.toString() + "px " + relativeCenterY + "px"
        }
    }
    return "0 0" //top left
}

private fun getTextDx(oldTextRect: DOMRect?, newTextRect: DOMRect?): Double {
    if (oldTextRect != null && newTextRect != null) {
        return ((oldTextRect.left + oldTextRect.width/2) - (newTextRect.left + newTextRect.width/2))
    }
    return 0.0
}
private fun getTextDy(oldTextRect: DOMRect?, newTextRect: DOMRect?): Double {
    if (oldTextRect != null && newTextRect != null) {
        return ((oldTextRect.top + oldTextRect.height/2) - (newTextRect.top + newTextRect.height/2))
    }
    return 0.0
}

private fun isTextElement(elm: Element): Boolean {
    return elm.childNodes.length == 1 && elm.childNodes[0]?.nodeType == 3.toShort()
}
private fun isTextElement(elm: Text): Boolean {
    return elm.childNodes.length == 1 && elm.childNodes[0]?.nodeType == 3.toShort()
}

class HeroModule : Module() {
    var removed: Json = newObj()
    var created = mutableListOf<dynamic>()

    init {
        pre = {
            removed = newObj()
            created = mutableListOf<dynamic>()
        } as PreHook

        create = { oldVnode: VNode, vnode: VNode ->
            val hero = vnode.data?.hero
            if (hero?.id != null) {
                created.add(hero.id)
                created.add(vnode)
            }
        }

        destroy = { vnode: VNode ->
            val hero = vnode.data?.hero
            if (hero?.id != null) {
                val elm = vnode.elm
                val vnode_obj = vnode.asDynamic()
                vnode_obj.isTextNode = isTextElement(elm as Element) //is this a text node?
                vnode_obj.boundingRect = (elm as Element).getBoundingClientRect() //save the bounding rectangle to a new property on the vnode
                vnode_obj.textRect = if (vnode_obj.isTextNode) getTextNodeRect((elm as Element).childNodes[0] as Text) else null //save bounding rect of inner text node
                val computedStyle = window.getComputedStyle(elm as Element, null) //get current styles (includes inherited properties)
                vnode_obj.savedStyle = JSON.parse(JSON.stringify(computedStyle)) //save a copy of computed style values
                removed[hero.id.toString()] = vnode
            }
        }

        post = {
            var newElm: Element
            var oldVnode: VNode?
            var oldElm: Element
            var hRatio: Double
            var wRatio: Double
            var oldRect: DOMRect
            var newRect: DOMRect
            var dx: Double
            var dy: Double
            var origTransform: String?
            var origTransition: String?
            var newStyle: CSSStyleDeclaration
            var oldStyle: CSSStyleDeclaration
            var newComputedStyle: CSSStyleDeclaration
            var isTextNode: Boolean
            var newTextRect: DOMRect? = null
            var oldTextRect: DOMRect? = null
            for (i in 0 until created.size step 2) {
                val id = created[i]
                newElm = created[i+1].elm
                oldVnode = removed[id].unsafeCast<VNode?>()
                if (oldVnode != null) {
                    isTextNode = oldVnode.asDynamic().isTextNode && isTextElement(newElm as Element) //Are old & new both text?
                    newStyle = (newElm as HTMLElement).style
                    newComputedStyle = window.getComputedStyle(newElm, undefined) //get full computed style for new element
                    oldElm = oldVnode.elm as Element
                    oldStyle = (oldElm as HTMLElement).style
                    //Overall element bounding boxes
                    newRect = newElm.getBoundingClientRect()
                    oldRect = (oldVnode.asDynamic()).boundingRect //previously saved bounding rect
                    //Text node bounding boxes & distances
                    if (isTextNode) {
                        newTextRect = getTextNodeRect(newElm.childNodes[0] as Text)
                        oldTextRect = (oldVnode.asDynamic()).textRect
                        dx = getTextDx(oldTextRect, newTextRect)
                        dy = getTextDy(oldTextRect, newTextRect)
                    } else {
                        //Calculate distances between old & new positions
                        dx = oldRect.left - newRect.left
                        dy = oldRect.top - newRect.top
                    }
                    hRatio = newRect.height / (max(oldRect.height, 1.0))
                    wRatio = if (isTextNode) hRatio else newRect.width / (max(oldRect.width, 1.0)) //text scales based on hRatio
                    // Animate new element
                    origTransform = newStyle.transform
                    origTransition = newStyle.transition
                    if (newComputedStyle.display === "inline") //inline elements cannot be transformed
                        newStyle.display = "inline-block"        //this does not appear to have any negative side effects
                    newStyle.transition = origTransition + "transform 0s"
                    newStyle.transformOrigin = calcTransformOrigin(isTextNode, newTextRect, newRect)
                    newStyle.opacity = "0"
                    newStyle.transform = origTransform + "translate("+dx+"px, "+dy+"px) " +
                            "scale("+1/wRatio+", "+1/hRatio+")"
                    setNextFrame(newStyle, "transition", origTransition)
                    setNextFrame(newStyle, "transform", origTransform)
                    setNextFrame(newStyle, "opacity", "1")
                    // Animate old element
                    for (key in jsObjKeys(oldVnode.asDynamic().savedStyle)) { //re-apply saved inherited properties
                        if (key.toIntOrNull() == null) {
                            val ms = key.substring(0,2) === "ms"
                            val moz = key.substring(0,3) === "moz"
                            val webkit = key.substring(0,6) === "webkit"
                            if (!ms && !moz && !webkit) //ignore prefixed style properties
                                (oldStyle.asDynamic())[key] = (oldVnode.asDynamic()).savedStyle[key]
                        }
                    }
                    oldStyle.position = "absolute"
                    oldStyle.top = oldRect.top.toString() + "px" //start at existing position
                    oldStyle.left = oldRect.left.toString() + "px"
                    oldStyle.width = oldRect.width.toString() + "px" //Needed for elements who were sized relative to their parents
                    oldStyle.height = oldRect.height.toString() + "px" //Needed for elements who were sized relative to their parents
                    oldStyle.margin = "0" //Margin on hero element leads to incorrect positioning
                    oldStyle.transformOrigin = calcTransformOrigin(isTextNode, oldTextRect, oldRect)
                    oldStyle.transform = ""
                    oldStyle.opacity = "1"
                    document.body!!.appendChild(oldElm)
                    setNextFrame(oldStyle, "transform", "translate("+ -dx +"px, "+ -dy +"px) scale("+wRatio+", "+hRatio+")") //scale must be on far right for translate to be correct
                    setNextFrame(oldStyle, "opacity", "0")
                    oldElm.addEventListener("transitionend", fun (ev: dynamic/*TransitionEvent*/) {
                        if (ev.propertyName === "transform")
                            document.body!!.removeChild(ev.target as Node)
                    })
                }
            }
            this.created.clear()
            (this.asDynamic()).created = undefined
            (this.asDynamic()).removed = undefined
        } as PostHook
    }
}