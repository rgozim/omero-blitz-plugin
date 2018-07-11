package org.openmicroscopy.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task that allows you to get {@code .ome.xml} files from omero-model artifact
 *
 * Example usage:
 * <pre>
 * {@code
 * task importMappings(type: org.openmicroscopy.tasks.ImportMappingsTask)
 *}
 * </pre>
 */
class ImportMappingsTask extends DefaultTask {

    @Internal
    static final String CONFIGURATION_NAME = 'omeXmlFiles'

    @Internal
    static final String OMERO_MODEL_VERSION = '1.0-SNAPSHOT'

    @OutputDirectory
    File extractDir

    void setExtractDir(Object dir) {
        this.extractDir = project.file(dir)
    }

    void extractDir(Object dir) {
        setExtractDir(dir)
    }

    @TaskAction
    void apply() {
        def omeroModelArtifact = getOmeroModelArtifact()
        if (!omeroModelArtifact) {
            throw new GradleException('Can\'t find omero-model artifact')
        }

        println "Extracting to: " + extractDir

        project.copy {
            from project.zipTree(omeroModelArtifact.file)
            into extractDir
            include "mappings/*.ome.xml"
        }

        println "project.copy"
    }

    def getOmeroModelArtifact() {
        def artifact = project.plugins.hasPlugin(JavaPlugin) ?
                getOmeroModelFromCompileConfig() : null

        if (artifact) {
            return artifact
        } else {
            return getOmeroModelWithCustomConfig()
        }
    }

    def getOmeroModelFromCompileConfig() {
        return project.configurations.compile
                .resolvedConfiguration
                .resolvedArtifacts
                .find { item ->
            item.name.contains("omero-model")
        }
    }

    def getOmeroModelWithCustomConfig() {
        def config = project.configurations.findByName(CONFIGURATION_NAME)
        if (!config) {
            config = project.configurations.create(CONFIGURATION_NAME)
                    .setVisible(false)
                    .setDescription("The data artifacts to be processed for this plugin.");
        }

        if (config.dependencies.empty) {
            config.defaultDependencies { DependencySet dependencies ->
                dependencies.add project.dependencies.create("org.openmicroscopy:omero-model:${OMERO_MODEL_VERSION}")
            }
        }

        return config.resolvedConfiguration
                .resolvedArtifacts
                .find { item ->
            item.name.contains("omero-model")
        }
    }
}
