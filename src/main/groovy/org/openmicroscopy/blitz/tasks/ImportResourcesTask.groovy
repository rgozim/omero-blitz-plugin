package org.openmicroscopy.blitz.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@CompileStatic
class ImportResourcesTask extends DefaultTask {

    private static final String CONFIGURATION_NAME = 'omeroModelFiles'

    private static final Logger Log = Logging.getLogger(ImportResourcesTask)

    @OutputDirectory
    File extractDir

    @Input
    String includePattern

    @TaskAction
    void apply() {
        ResolvedArtifact artifact = getOmeroModelArtifact()
        if (!artifact) {
            throw new GradleException('Can\'t find omero-model artifact')
        }

        PatternSet patternSet = new PatternSet()
        patternSet.include(includePattern)

        FileTree fileTree = project.zipTree(artifact.file).matching(patternSet)

        fileTree.files.each { File src ->
            Path file = src.toPath()
            Path to = extractDir.toPath()

            // Copy each file to output location
            Files.copy(src.toPath(), to.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    void extractDir(Object dir) {
        setExtractDir(dir)
    }

    void setExtractDir(Object dir) {
        this.extractDir = project.file(dir)
    }

    List<File> getResults() {
        return Arrays.asList(extractDir.listFiles())
    }

    private ResolvedArtifact getOmeroModelArtifact() {
        ResolvedArtifact artifact = project.plugins.hasPlugin(JavaPlugin) ?
                getOmeroModelFromCompileConfig() : null

        if (artifact) {
            return artifact
        } else {
            return getOmeroModelWithCustomConfig()
        }
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
