package com.emarsys.testUtil

import java.lang.reflect.Field

object ReflectionTestUtils {

    @JvmStatic
    fun setInstanceField(instance: Any, fieldName: String, value: Any?) =
            setField(instance, instance.javaClass, fieldName, value)

    @JvmStatic
    fun setStaticField(type: Class<*>, fieldName: String, value: Any?) =
            setField(null, type, fieldName, value)

    private fun setField(instance: Any?, type: Class<*>, fieldName: String, value: Any?) {
        val containerField = searchForField(type, fieldName)
        containerField.isAccessible = true
        containerField.set(instance, value)
    }

    @JvmStatic
    fun <T> getInstanceField(instance: Any, fieldName: String): T? =
            getField(instance, instance::class.java, fieldName)

    @JvmStatic
    fun <T> getStaticField(type: Class<*>, fieldName: String): T? =
            getField(null, type, fieldName)

    @Suppress("UNCHECKED_CAST")
    private fun <T> getField(instance: Any?, type: Class<*>, fieldName: String): T? {
        val field = searchForField(type, fieldName)
        field.isAccessible = true
        val result = field.get(instance)
        return result as T?
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> instantiate(type: Class<*>, constructorIndex: Int, vararg args: Any): T {
        val constructor = type.declaredConstructors[constructorIndex]
        constructor.isAccessible = true
        return constructor.newInstance(*args) as T
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun invokeStaticMethod(type: Class<*>, methodName: String, vararg parameters: Any) {
        val parameterTypes = parameters.map { it::class.javaPrimitiveType }.toTypedArray()
        val method = type.getDeclaredMethod(methodName, *parameterTypes)
        method.isAccessible = true
        method.invoke(null, *parameters)
    }

    private fun searchForField(type: Class<*>, fieldName: String): Field = try {
        type.getDeclaredField(fieldName)
    } catch (nsfe: NoSuchFieldException) {
        nsfe
    }.let { result ->
        when (result) {
            is NoSuchFieldException -> when (val superclass = type.superclass) {
                null -> throw NoSuchFieldException("Could not find field in class hierarchy!")
                else -> searchForField(superclass, fieldName)
            }
            is Field -> result
            else -> throw IllegalStateException("Unrecognized type: ${result.javaClass}")
        }
    }

}
