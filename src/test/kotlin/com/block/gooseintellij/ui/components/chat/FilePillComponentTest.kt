package com.block.gooseintellij.ui.components.chat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.junit.jupiter.api.Assertions.*

class FilePillComponentTest {
    private lateinit var project: Project
    private lateinit var virtualFile: VirtualFile
    private lateinit var onRemove: (String) -> Unit
    
    @BeforeEach
    fun setup() {
        project = mock(Project::class.java)
        virtualFile = mock(VirtualFile::class.java)
        onRemove = mock { }
        
        // Set up virtual file mock
        `when`(virtualFile.name).thenReturn("test.kt")
        `when`(virtualFile.path).thenReturn("/test/path/test.kt")
    }
    
    @Test
    fun `test hasFilePathInPills returns true for existing path`() {
        // Create a pill component
        val pill = FilePillComponent(project, virtualFile, true) { }
        val pills = mapOf(pill to "/test/path/test.kt")
        
        // Test the static method
        assertTrue(FilePillComponent.hasFilePathInPills("/test/path/test.kt", pills))
    }
    
    @Test
    fun `test hasFilePathInPills returns false for non-existing path`() {
        // Create a pill component
        val pill = FilePillComponent(project, virtualFile, true) { }
        val pills = mapOf(pill to "/test/path/test.kt")
        
        // Test with a different path
        assertFalse(FilePillComponent.hasFilePathInPills("/different/path/file.kt", pills))
    }
    
    @Test
    fun `test getUniquePills removes duplicates`() {
        // Create two pills with the same path
        val pill1 = FilePillComponent(project, virtualFile, true) { }
        val pill2 = FilePillComponent(project, virtualFile, true) { }
        
        val pills = mapOf(
            pill1 to "/test/path/test.kt",
            pill2 to "/test/path/test.kt"
        )
        
        // Get unique pills
        val uniquePills = FilePillComponent.getUniquePills(pills)
        
        // Should only contain one entry
        assertEquals(1, uniquePills.size)
        assertEquals("/test/path/test.kt", uniquePills.values.first())
    }
    
    @Test
    fun `test getUniquePills keeps different paths`() {
        // Create mock for second file
        val virtualFile2 = mock(VirtualFile::class.java)
        `when`(virtualFile2.name).thenReturn("other.kt")
        `when`(virtualFile2.path).thenReturn("/test/path/other.kt")
        
        // Create two pills with different paths
        val pill1 = FilePillComponent(project, virtualFile, true) { }
        val pill2 = FilePillComponent(project, virtualFile2, true) { }
        
        val pills = mapOf(
            pill1 to "/test/path/test.kt",
            pill2 to "/test/path/other.kt"
        )
        
        // Get unique pills
        val uniquePills = FilePillComponent.getUniquePills(pills)
        
        // Should contain both entries
        assertEquals(2, uniquePills.size)
        assertTrue(uniquePills.values.containsAll(listOf("/test/path/test.kt", "/test/path/other.kt")))
    }
}