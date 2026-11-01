package com.openminis.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRouterPlannerTest {
    private fun result(id: String, state: SmartRouterProbe.State) =
        SmartRouterProbe.Result(id, id, state, "")

    @Test fun prioritizesHealthyModelsWithoutDroppingFailedOrSkippedEntries() {
        val original = listOf("nvidia-a", "groq-b", "gemini-c", "nvidia-d")
        val results = listOf(
            result("nvidia-a", SmartRouterProbe.State.UNAVAILABLE),
            result("groq-b", SmartRouterProbe.State.AVAILABLE),
            result("gemini-c", SmartRouterProbe.State.SKIPPED),
            result("nvidia-d", SmartRouterProbe.State.AVAILABLE),
        )
        assertEquals(listOf("groq-b", "nvidia-d", "nvidia-a", "gemini-c"),
            SmartRouterPlanner.prioritize(original, results))
    }

    @Test fun noSuccessKeepsExistingOrder() {
        val ids = listOf("a", "b")
        assertEquals(ids, SmartRouterPlanner.prioritize(ids,
            listOf(result("a", SmartRouterProbe.State.UNAVAILABLE))))
    }

    @Test fun unknownAndDuplicatedResultsCannotInventGroupMembers() {
        val ids = listOf("a", "b", "c")
        val results = listOf(
            result("ghost", SmartRouterProbe.State.AVAILABLE),
            result("b", SmartRouterProbe.State.AVAILABLE),
            result("b", SmartRouterProbe.State.AVAILABLE),
        )
        assertEquals(listOf("b", "a", "c"), SmartRouterPlanner.prioritize(ids, results))
    }

    @Test fun probeLimitsAreStrictAndPositive() {
        assertTrue(SmartRouterProbe.MAX_PROBES in 1..12)
        assertTrue(SmartRouterProbe.PROBE_TIMEOUT_MS in 1000L..30000L)
    }
}
