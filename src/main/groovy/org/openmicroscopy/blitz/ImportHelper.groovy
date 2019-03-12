package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.JavaPlugin

import static org.openmicroscopy.blitz.ConventionPluginHelper.getCompileClasspathConfiguration

@CompileStatic
class ImportHelper {

    public static final String CONFIGURATION_NAME = "dataFiles"

    static File findOmeroModel(Configuration config) {
        return config.incoming.artifacts.artifactFiles.files.find {
            it.name.contains("omero-model")
        }
    }

    static Configuration getConfigurationForOmeroModel(Project project) {
        return project.plugins.hasPlugin(JavaPlugin) ?
                getCompileClasspathConfiguration(project) :
                getDataFilesConfig(project)
    }

    static Configuration getDataFilesConfig(Project project) {
        Configuration config = project.configurations.findByName(CONFIGURATION_NAME)
        if (!config) {
            config = createDataFilesConfig(project)
        }
        return config
    }

    static Configuration createDataFilesConfig(Project project) {
        project.buildscript.repositories.addAll(
                project.repositories.mavenLocal(),
                project.repositories.mavenCentral(),
                project.repositories.jcenter()
        )

        Configuration config = project.configurations.create(CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(false)
                .setDescription("The data artifacts to be processed for this plugin.")

        config.defaultDependencies(new Action<DependencySet>() {
            void execute(DependencySet dependencies) {
                dependencies.add(project.dependencies.create("org.openmicroscopy:omero-model:5.5.+"))
            }
        })

        return config
    }
}
