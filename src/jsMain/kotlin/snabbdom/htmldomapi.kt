package snabbdom

import org.w3c.dom.*
import kotlin.browser.document

interface DOMAPI {
    fun createElement(tagName: dynamic): HTMLElement
    fun createElementNS(namespaceURI: String, qualifiedName: String): Element
    fun createTextNode(text: String): Text
    fun createComment(text: String): Comment
    fun insertBefore(parentNode: Node, newNode: Node, referenceNode: Node?)
    fun removeChild(node: Node, child: Node)
    fun appendChild(node: Node, child: Node)
    fun parentNode(node: Node): Node?
    fun nextSibling(node: Node): Node?
    fun tagName(elm: Element): String
    fun setTextContent(node: Node, text: String?)
    fun getTextContent(node: Node): String?
    fun isElement(node: Node): Boolean
    fun isText(node: Node): Boolean
    fun isComment(node: Node): Boolean
}

object htmlDomApi : DOMAPI {
    override fun createElement(tagName: dynamic): HTMLElement =
        document.createElement(tagName) as HTMLElement

    override fun createElementNS(namespaceURI: String, qualifiedName: String): Element =
        document.createElementNS(namespaceURI, qualifiedName)

    override fun createTextNode(text: String): Text =
        document.createTextNode(text)

    override fun createComment(text: String): Comment =
        document.createComment(text)

    override fun insertBefore(parentNode: Node, newNode: Node, referenceNode: Node?) {
        parentNode.insertBefore(newNode, referenceNode)
    }

    override fun removeChild(node: Node, child: Node) {
        node.removeChild(child)
    }

    override fun appendChild(node: Node, child: Node) {
        node.appendChild(child)
    }

    override fun parentNode(node: Node): Node? = node.parentNode
    override fun nextSibling(node: Node): Node? = node.nextSibling
    override fun tagName(elm: Element): String = elm.tagName

    override fun setTextContent(node: Node, text: String?) {
        node.textContent = text
    }

    override fun getTextContent(node: Node): String? = node.textContent

    override fun isElement(node: Node): Boolean = node.nodeType == 1.toShort()
    override fun isText(node: Node): Boolean = node.nodeType == 3.toShort()
    override fun isComment(node: Node): Boolean = node.nodeType == 8.toShort()
}