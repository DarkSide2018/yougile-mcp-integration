package com.yougile.alertbot.harness

import com.yougile.alertbot.rag.loader.TemplateLoader
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TemplateLoaderAndRagHarnessTest {

    private val templateLoader = TemplateLoader()

    @Test
    @DisplayName("Verify that all knowledge base markdown templates load with frontmatter parsed")
    fun testTemplateLoading() {
        val templates = templateLoader.loadTemplates()
        assertFalse(templates.isEmpty(), "Templates list should not be empty")

        val types = templates.map { it.type }
        assertTrue(types.contains("cpu"), "Templates should include cpu template")
        assertTrue(types.contains("memory"), "Templates should include memory template")
        assertTrue(types.contains("disk"), "Templates should include disk template")
        assertTrue(types.contains("service"), "Templates should include service template")
        assertTrue(types.contains("default"), "Templates should include default template")

        for (t in templates) {
            assertNotNull(t.id)
            assertFalse(t.content.isBlank(), "Template ${t.type} content should not be blank")
            assertFalse(t.tags.isEmpty(), "Template ${t.type} tags should not be empty")
        }
    }
}
