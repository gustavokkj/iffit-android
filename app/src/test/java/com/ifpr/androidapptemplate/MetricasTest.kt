package com.ifpr.androidapptemplate

import com.ifpr.androidapptemplate.baseclasses.Metricas
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricasTest {
    @Test fun paceUsesMinutesAndSecondsRatherThanDecimalMinutes() {
        assertEquals("5:30 min/km", Metricas.ritmo(330, 1000.0))
    }
    @Test fun roundedSecondsCarryIntoNextMinute() {
        assertEquals("6:00 min/km", Metricas.ritmo(360, 1000.1))
    }
    @Test fun stationaryActivityDoesNotProduceInfinitePace() {
        assertEquals("—", Metricas.ritmo(100, 0.0))
        assertEquals("—", Metricas.ritmo(0, 100.0))
        assertEquals("—", Metricas.ritmo(10, Double.NaN))
    }
    @Test fun durationSupportsHours() {
        assertEquals("01:01:01", Metricas.tempo(3661))
        assertEquals("00:00:00", Metricas.tempo(-1))
    }
}
