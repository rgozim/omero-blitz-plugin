package org.openmicroscopy.blitz.tasks

import ome.dsl.SemanticType
import ome.dsl.velocity.MultiFileGenerator
import org.apache.velocity.app.VelocityEngine
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class CombinedFileTask extends DefaultTask {

    private static final def Log = Logging.getLogger(CombinedFileTask)

    @InputFiles
    FileCollection omeXmlFiles = project.files()

    @InputFiles
    FileCollection databaseTypes = project.files()

    @Input
    String databaseType

    @InputFile
    File template

    @OutputDirectory
    File outputDir

    @Nested
    MultiFileGenerator.FileNameFormatter formatOutput

    @Input
    @Optional
    Properties velocityProperties = new Properties()

    @TaskAction
    void apply() {
        VelocityEngine ve = new VelocityEngine(velocityProperties)

        // Build our file generator
        def builder = new MultiFileGenerator.Builder()
                .setOutputDir(outputDir)
                .setFileNameFormatter(formatOutput)

        builder.velocityEngine = ve
        builder.profile = databaseType
        builder.template = template
        builder.databaseTypes = getDatabaseTypeProperties()
        builder.omeXmlFiles = getOmeXmlFiles()
        builder.build().call()
    }

    void omeXmlFiles(Object... paths) {
        omeXmlFiles = omeXmlFiles + project.layout.files(paths)
    }

    void setOmeXmlFiles(Object... paths) {
        omeXmlFiles = project.layout.files(paths)
    }

    void formatOutput(Closure formatter) {
        setFormatOutput(formatter)
    }

    void setFormatOutput(Closure formatter) {
        formatOutput = new MultiFileGenerator.FileNameFormatter() {
            @Override
            String format(SemanticType t) {
                return formatter(t)
            }
        }
    }

    void databaseTypes(Object... paths) {
        this.databaseTypes = databaseTypes + project.layout.files(paths)
    }

    void setDatabaseTypes(Object... paths) {
        this.databaseTypes = project.layout.files(paths)
    }

    Properties getDatabaseTypeProperties() {
        Properties databaseTypeProps = new Properties()
        File databaseTypeFile = getDatabaseTypes()
        if (!databaseTypeFile) {
            throw new GradleException("Can't find ${databaseType}-types.properties")
        }
        databaseTypeFile.withInputStream { databaseTypeProps.load(it) }
        return databaseTypeProps
    }

    File getDatabaseTypes() {
        return getFilesInCollection(databaseTypes, "-types.properties").find {
            it.name == "$databaseType-types.properties"
        }
    }

    List<File> getOmeXmlFiles() {
        return getFilesInCollection(omeXmlFiles, ".ome.xml")
    }

    List<File> getFilesInCollection(FileCollection collection, String extension) {
        def directories = collection.findAll {
            it.isDirectory()
        }

        def files = collection.findAll {
            it.isFile() && it.name.endsWith("$extension")
        }

        files = files + directories.collectMany {
            project.fileTree(dir: it, include: "**/*$extension").files
        }

        return files
    }

}
