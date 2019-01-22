package org.openmicroscopy.blitz.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task that allows you to get {@code .ome.xml} files from omero-model artifact
 *
 * Example usage:
 * <pre>
 * {@code
 * task importMappings(type: org.openmicroscopy.blitz.tasks.ImportMappingsTask)
 *}
 * </pre>
 */
class ImportMappingsTask extends DefaultTask {

    private final def Log = Logging.getLogger(ImportMappingsTask)

    private final String CONFIGURATION_NAME = 'omeXmlFiles'

    private final String OMERO_MODEL_VERSION = project.properties['omeroModelVersion']

    @OutputDirectory
    File extractDir

    @TaskAction
    void apply() {
        def omeroModelArtifact = getOmeroModelArtifact()
        if (!omeroModelArtifact) {
            throw new GradleException('Can\'t find omero-model artifact')
        }

        Log.info("Extracting ome.xml files to: " + extractDir)
        project.copy {
            from project.zipTree(omeroModelArtifact.file)
            into extractDir
            include "mappings/*.ome.xml"
        }
    }

    void setExtractDir(Object dir) {
        this.extractDir = project.file(dir)
    }

    void extractDir(Object dir) {
        setExtractDir(dir)
    }

    private def getOmeroModelArtifact() {
        def artifact = getOmeroModelFromCompileConfig()
        if (artifact) {
            Log.info("omero-model found as a dependency")
            return artifact
        } else {
            Log.info("Adding omero-model as a dependency to obtain ome.xml files")
            return getOmeroModelWithCustomConfig()
        }
    }

    private def getOmeroModelFromCompileConfig() {
        return project.configurations.findAll { config ->
            config.resolvedConfiguration
                    .resolvedArtifacts
                    .find { item -> item.name.contains("omero-model") }
        }
    }

    private def getOmeroModelWithCustomConfig() {
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
