package com.openminis.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiProviderTemplateTest {
    @Test fun everyPresetHasUniqueNameAndHttpsEndpoint() {
        val templates = ApiProviderTemplate.all
        assertTrue(templates.isNotEmpty())
        assertEquals(templates.size, templates.map { it.name }.distinct().size)
        assertTrue(templates.all { it.baseURL.startsWith("https://") })
        assertTrue(templates.all { !it.baseURL.endsWith("/") })
        assertTrue(templates.all { !it.appendV1 })
    }

    @Test fun endpointsWithVersionedPathsKeepTheirFullPath() {
        val groq = ApiProviderTemplate.all.first { it.name == "GroqCloud" }
        val nvidia = ApiProviderTemplate.all.first { it.name == "NVIDIA NIM" }
        assertEquals("https://api.groq.com/openai/v1", groq.baseURL)
        assertEquals("https://integrate.api.nvidia.com/v1", nvidia.baseURL)
        assertFalse(groq.appendV1)
        assertFalse(nvidia.appendV1)
    }
}
