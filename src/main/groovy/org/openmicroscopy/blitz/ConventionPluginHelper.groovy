package org.openmicroscopy.blitz

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin

class ConventionPluginHelper {

    static Configuration getRuntimeClasspathConfiguration(Project project) {
        project.configurations.findByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
    }

    static Configuration getImportDataFilesConfiguration(Project project) {
        project.configurations.findByName(ImportHelper.CONFIGURATION_NAME)
    }

}
