package com.pseudo.statcounter

import com.pseudo.statcounter.StatProcessor.Companion.processor
import com.pseudo.statcounter.proto.Stat
import com.pseudo.statcounter.proto.configuration
import com.pseudo.statcounter.proto.participant
import com.pseudo.statcounter.proto.stat
import com.pseudo.statcounter.proto.statMapping

fun main() {
    val configuration = configuration {
        participants += participant {
            name = "geo"
            someBooleanThing = true
            someIntThing = 2
        }
        participants += participant {
            name = "geo"
            someBooleanThing = false
            someIntThing = 3
        }
        participants += participant {
            name = "geo"
            someBooleanThing = true
            someIntThing = 0
        }
        participants += participant {
            name = "geo"
        }
        participants += participant {
            name = "geo"
            someBooleanThing = true
        }
        participants += participant {
            name = "geo"
            someIntThing = 8
        }
        participants += participant {
            name = "pseudo"
        }
        desiredStats += statMapping {
            stat = stat {
                name = "bool counter"
                count = 0
            }
            fieldName = "some_boolean_thing"
        }
        desiredStats += statMapping {
            stat = stat {
                name = "int counter"
                count = 0
            }
            fieldName = "some_int_thing"
        }
        desiredStats += statMapping {
            stat = stat {
                name = "int sum"
                sum = 0
            }
            fieldName = "some_int_thing"
        }
    }

    val finalPlayerStats = configuration.participantsList
        .groupBy { it.name }
        .mapValues { (playerName, participantInfo) ->
            val allProcessors = configuration.desiredStatsList.mapNotNull { it.processor() }

            for (participant in participantInfo) {
                for (processor in allProcessors) {
                    processor.process(participant)
                }
            }

            allProcessors.map { it.value }
        }

    for ((player, stats) in finalPlayerStats) {
        println("Computed Statistics for $player:")
        for (stat in stats) {
            println("  ${stat.name}: ${stat.statValueString()}")
        }
    }

}

private fun Stat.statValueString(): String {
    return when (statTypeCase) {
        Stat.StatTypeCase.COUNT -> count.toString()
        Stat.StatTypeCase.SUM -> sum.toString()
        Stat.StatTypeCase.STATTYPE_NOT_SET -> "?"
        else -> "??"
    }
}
