package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ResolvedArtifact

@CompileStatic
class ImportHelper {

    private static final String CONFIGURATION_NAME = "omeroModelFiles"

    final Project project

    ImportHelper(Project project) {
        this.project = project
    }

    ResolvedArtifact getOmeroModelArtifact() {
        ResolvedArtifact artifact = getOmeroModelFromCompileConfig()
        return artifact ?: getOmeroModelWithCustomConfig()
    }

    private ResolvedArtifact getOmeroModelFromCompileConfig() {
        Set<Configuration> resolvableConfigs = project.configurations.findAll { Configuration c ->
            c.canBeResolved
        }
        List<ResolvedArtifact> artifacts = resolvableConfigs.collect { Configuration config ->
            config.resolvedConfiguration.resolvedArtifacts.find { item ->
                item.name.contains("omero-model")
            }
        }
        return artifacts[0]
    }

    private ResolvedArtifact getOmeroModelWithCustomConfig() {
        def config = project.configurations.findByName(CONFIGURATION_NAME)
        if (!config) {
            config = project.configurations.create(CONFIGURATION_NAME)
                    .setVisible(false)
                    .setDescription("The data artifacts to be processed for this plugin.");
        }

        if (config.dependencies.empty) {
            config.defaultDependencies { DependencySet dependencies ->
                dependencies.add project.dependencies.create("org.openmicroscopy:omero-model:+")
            }
        }

        return config.resolvedConfiguration
                .resolvedArtifacts
                .find { item ->
            item.name.contains("omero-model")
        }
    }
}
