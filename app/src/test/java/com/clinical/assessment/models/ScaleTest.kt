package com.clinical.assessment.models

import org.junit.Test
import org.junit.Assert.*

class ScaleTest {

    @Test
    fun testScaleCreation() {
        val scale = Scale(
            id = "test_scale",
            name = "Test Scale",
            description = "A test scale",
            items = listOf("Q1", "Q2"),
            options = listOf("0", "1"),
            labels = mapOf("0" to "No", "1" to "Yes"),
            scoring = ScoringType.SUM,
            weights = null,
            thresholds = null,
            threshold = 5.0,
            domains = null
        )

        assertEquals("test_scale", scale.id)
        assertEquals("Test Scale", scale.name)
        assertEquals(ScoringType.SUM, scale.scoring)
        assertEquals(2, scale.items.size)
    }

    @Test
    fun testScoringTypeEnum() {
        val types = ScoringType.values()
        assertTrue(types.contains(ScoringType.SUM))
        assertTrue(types.contains(ScoringType.WEIGHTED_SUM))
        assertTrue(types.contains(ScoringType.COUNT_YES))
        assertTrue(types.contains(ScoringType.MEAN_PER_DOMAIN_WEIGHTED))
    }
}
