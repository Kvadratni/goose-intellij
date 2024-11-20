package com.block.gooseintellij.utils

import com.intellij.openapi.project.Project
import java.io.File

object GooseUtils {
    /**
     * Gets the project path, handling special cases like .ijwb extension
     */
    fun getProjectPath(project: Project): String {
        val basePath = project.basePath!!
        if (basePath.endsWith(".ijwb")) {
            return File(basePath).parent
        }
        return basePath
    }
}