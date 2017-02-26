package net.namekdev.entity_tracker.utils

import com.artemis.utils.reflect.ClassReflection
import com.artemis.utils.reflect.Field
import com.artemis.utils.reflect.Method
import com.artemis.utils.reflect.ReflectionException

object ReflectionUtils {
    fun getHiddenField(type: Class<*>, fieldName: String): Field? {
        var field: Field? = null

        try {
            field = ClassReflection.getDeclaredField(type, fieldName)
            field!!.isAccessible = true
        }
        catch (e: ReflectionException) {
            e.printStackTrace()
        }

        return field
    }

    fun getHiddenFieldValue(type: Class<*>, fieldName: String, obj: Any): Any? {
        val field = getHiddenField(type, fieldName)
        try {
            return field!!.get(obj)
        }
        catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun getHiddenMethod(type: Class<*>, methodName: String): Method {
        try {
            val method = ClassReflection.getDeclaredMethod(type, methodName)
            method.isAccessible = true
            return method
        }
        catch (e: ReflectionException) {
            throw RuntimeException(e)
        }
    }

    fun getFieldValue(field: Field, obj: Any): Any {
        try {
            field.isAccessible = true
            return field.get(obj)
        }
        catch (e: ReflectionException) {
            throw RuntimeException(e)
        }
    }

    fun setHiddenFieldValue(type: Class<out Any>, fieldName: String, obj: Any, value: Any) {
        val field = getHiddenField(type, fieldName)

        try {
            field!!.set(obj, value)
        }
        catch (e: ReflectionException) {
            throw RuntimeException(e)
        }
    }

    fun getDeclaredFields(type: Class<*>): Array<Field> =
        ClassReflection.getDeclaredFields(type)
}
