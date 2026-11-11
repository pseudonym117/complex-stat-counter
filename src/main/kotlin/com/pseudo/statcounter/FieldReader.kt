package com.pseudo.statcounter

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Message


sealed interface FieldReader<out FieldType> {
    fun read(item: Message): FieldType?

    class LongReader(private val fieldDescriptor: FieldDescriptor) : FieldReader<Long> {
        init {
            check(
                when (fieldDescriptor.javaType) {
                    FieldDescriptor.JavaType.INT,
                    FieldDescriptor.JavaType.LONG -> true

                    else -> false
                }
            )
        }

        override fun read(item: Message): Long? {
            return if (item.hasField(fieldDescriptor)) {
                when (val read = item.getField(fieldDescriptor)) {
                    is Int -> read.toLong()
                    is Long -> read
                    else -> 0
                }
            } else {
                null
            }
        }
    }

    class BoolReader(private val fieldDescriptor: FieldDescriptor) : FieldReader<Boolean> {
        init {
            check(fieldDescriptor.javaType == FieldDescriptor.JavaType.BOOLEAN)
        }

        override fun read(item: Message): Boolean? {
            return if (item.hasField(fieldDescriptor)) {
                when (val read = item.getField(fieldDescriptor)) {
                    is Boolean -> read
                    else -> if (fieldDescriptor.hasDefaultValue()) {
                        fieldDescriptor.defaultValue as? Boolean ?: false
                    } else {
                        false
                    }
                }
            } else {
                null
            }
        }
    }

    companion object {
        private val protoCache = mutableMapOf<Class<out Message>, MutableMap<String, FieldReader<*>?>>()

        fun <T : Message> T.fieldReader(fieldName: String): FieldReader<*>? {
            val descriptor = descriptorForType.findFieldByName(fieldName)
            return if (descriptor != null) {
                val readerCache = synchronized(protoCache) {
                    protoCache.computeIfAbsent(javaClass) { mutableMapOf() }
                }
                synchronized(readerCache) {
                    readerCache.computeIfAbsent(descriptor.name) {
                        when (descriptor.javaType) {
                            FieldDescriptor.JavaType.INT,
                            FieldDescriptor.JavaType.LONG -> FieldReader.LongReader(descriptor)

                            FieldDescriptor.JavaType.BOOLEAN -> FieldReader.BoolReader(descriptor)
                            else -> null
                        }
                    }
                }
            } else {
                null
            }
        }
    }
}
