package org.openmicroscopy.blitz.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet
import org.openmicroscopy.blitz.ImportHelper

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@CompileStatic
class ImportResourcesTask extends DefaultTask {

    private static final Logger Log = Logging.getLogger(ImportResourcesTask)

    @OutputDirectory
    File extractDir

    @Input
    String pattern

    PatternSet patternSet = new PatternSet()

    ImportHelper importHelper = new ImportHelper(project)

    @TaskAction
    void apply() {
        ResolvedArtifact artifact = importHelper.getOmeroModelArtifact()
        if (!artifact) {
            throw new GradleException('Can\'t find omero-model artifact')
        }

        // Set our pattern set
        patternSet.include(pattern)

        // obtain file tree for jar file
        FileTree fileTree = project.zipTree(artifact.file).matching(patternSet)

        // Copy each file matching pattern to our extract directory
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

}
