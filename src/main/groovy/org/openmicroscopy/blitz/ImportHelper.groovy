package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ResolvedArtifact

import static org.openmicroscopy.blitz.ConventionPluginHelper.getRuntimeClasspathConfiguration

@CompileStatic
class ImportHelper {

    static final String CONFIGURATION_NAME = "dataFiles"

    static ResolvedArtifact getOmeroModelArtifact(Project project) {
        ResolvedArtifact artifact = getOmeroModelFromCompileConfig(project)
        return artifact ?: getOmeroModelWithCustomConfig(project)
    }

    static ResolvedArtifact getOmeroModelFromCompileConfig(Project project) {
        Configuration runtimeClasspath = getRuntimeClasspathConfiguration(project)
        if (!runtimeClasspath) {
            return null
        }
        runtimeClasspath.resolvedConfiguration.resolvedArtifacts.find { it.name.contains("omero-model") }
    }

    static ResolvedArtifact getOmeroModelWithCustomConfig(Project project) {
        Configuration config = project.configurations.findByName(CONFIGURATION_NAME)
        if (!config) {
            config = createDataFilesConfig(project)
        }

        config.resolvedConfiguration.resolvedArtifacts.find { it.name.contains("omero-model") }
    }

    static Configuration createDataFilesConfig(Project project) {
        project.buildscript.repositories.addAll(
                project.repositories.mavenLocal(),
                project.repositories.mavenCentral(),
                project.repositories.jcenter()
        )

        final Configuration config = project.getConfigurations().create(ImportHelper.CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(false)
                .setDescription("The data artifacts to be processed for this plugin.")

        config.defaultDependencies(new Action<DependencySet>() {
            void execute(DependencySet dependencies) {
                dependencies.add(project.getDependencies().create("org.openmicroscopy:omero-model:5.5.0-SNAPSHOT"))
            }
        })

        return config
    }
}
