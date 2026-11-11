package com.pseudo.statcounter

import com.google.protobuf.Message
import com.pseudo.statcounter.FieldReader.Companion.fieldReader
import com.pseudo.statcounter.proto.Stat
import com.pseudo.statcounter.proto.StatKt
import com.pseudo.statcounter.proto.StatMapping
import com.pseudo.statcounter.proto.copy

sealed interface StatProcessor {
    val value: Stat
    fun process(message: Message)

    abstract class BasicMutableProcessor<T>(private val fieldName: String, private var currentValue: Stat) :
        StatProcessor {
        override val value: Stat
            get() = currentValue

        private var valueReader: FieldReader<T>? = null

        final override fun process(message: Message) {
            val valueReader = getOrCreateFieldReader(message)
            if (valueReader != null) {
                val readValue = valueReader.readCoerced(message)
                if (readValue != null) {
                    currentValue = currentValue.copy {
                        updateStat(readValue)
                    }
                }
            }
        }

        protected abstract fun FieldReader<*>.readCoerced(message: Message): T?
        protected abstract fun StatKt.Dsl.updateStat(readValue: T)

        private fun getOrCreateFieldReader(message: Message): FieldReader<T>? {
            val currentReader = valueReader
            return currentReader
                ?: (message.fieldReader(fieldName) as FieldReader<T>?).also {
                    valueReader = it
                }
        }
    }

    class CountProcessor(mapping: StatMapping) : BasicMutableProcessor<Boolean>(
        fieldName = mapping.fieldName,
        currentValue = mapping.stat,
    ) {
        init {
            check(value.hasCount())
        }

        override fun FieldReader<*>.readCoerced(message: Message): Boolean {
            return when (this) {
                is FieldReader.LongReader -> (read(item = message) ?: 0) != 0L
                is FieldReader.BoolReader -> (read(item = message) ?: false)
            }
        }

        override fun StatKt.Dsl.updateStat(readValue: Boolean) {
            if (readValue) {
                count++
            }
        }
    }

    class SumProcessor(mapping: StatMapping) : BasicMutableProcessor<Long>(
        fieldName = mapping.fieldName,
        currentValue = mapping.stat,
    ) {
        init {
            check(value.hasSum())
        }

        override fun FieldReader<*>.readCoerced(message: Message): Long? {
            return when (this) {
                is FieldReader.LongReader -> read(message)
                is FieldReader.BoolReader -> if (read(message) ?: false) 1 else 0
            }
        }

        override fun StatKt.Dsl.updateStat(readValue: Long) {
            sum += readValue
        }
    }

    companion object {
        fun StatMapping.processor(): StatProcessor? {
            return when (stat.statTypeCase) {
                Stat.StatTypeCase.COUNT -> CountProcessor(this)
                Stat.StatTypeCase.SUM -> SumProcessor(this)
                Stat.StatTypeCase.STATTYPE_NOT_SET -> null
                else -> null
            }
        }
    }
}
