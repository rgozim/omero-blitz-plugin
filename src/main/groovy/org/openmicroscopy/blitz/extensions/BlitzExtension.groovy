package org.openmicroscopy.blitz.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.openmicroscopy.dsl.extensions.MultiFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.SingleFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.specs.DslSpec

class BlitzExtension implements DslSpec {

    private final Project project

    final NamedDomainObjectContainer<MultiFileGeneratorExtension> multiFile

    final NamedDomainObjectContainer<SingleFileGeneratorExtension> singleFile

    final CombinedConfig combined = new CombinedConfig(project)

    final ConfigurableFileCollection omeXmlFiles

    final ConfigurableFileCollection databaseTypes

    final ConfigurableFileCollection templates

    final DirectoryProperty outputDir

    final Property<String> database

    BlitzExtension(Project project,
                   NamedDomainObjectContainer<MultiFileGeneratorExtension> multiFile,
                   NamedDomainObjectContainer<SingleFileGeneratorExtension> singleFile) {
        this.project = project
        this.multiFile = multiFile
        this.singleFile = singleFile
        this.omeXmlFiles = project.files()
        this.databaseTypes = project.files()
        this.templates = project.files()
        this.outputDir = project.objects.directoryProperty()
        this.database = project.objects.property(String)

        // Set some conventions
        this.outputDir.convention(project.layout.projectDirectory.dir("src/generated"))
        this.database.convention("psql")
        this.combined.outputDir.convention(new File("$project.buildDir/combined"))
        this.combined.template.convention("combined.vm")
    }

    void multiFile(Action<? super NamedDomainObjectContainer<MultiFileGeneratorExtension>> action) {
        action.execute(multiFile)
    }

    void singleFile(Action<? super NamedDomainObjectContainer<SingleFileGeneratorExtension>> action) {
        action.execute(singleFile)
    }

    void customData(Action<? super CombinedConfig> action) {
        action.execute(combined)
    }

    void omeXmlFiles(Object... paths) {
        this.omeXmlFiles.from(paths)
    }

    void setOmeXmlFiles(Object... paths) {
        this.omeXmlFiles.setFrom(paths)
    }

    void setOmeXmlFiles(Iterable<?> paths) {
        this.omeXmlFiles.setFrom(paths)
    }

    void databaseTypes(Object... paths) {
        this.databaseTypes.from(paths)
    }

    void setDatabaseTypes(Object... paths) {
        this.databaseTypes.setFrom(paths)
    }

    void setDatabaseTypes(Iterable<?> paths) {
        this.databaseTypes.setFrom(paths)
    }

    void templates(Object... paths) {
        this.templates.from(paths)
    }

    void setTemplates(Object... paths) {
        this.templates.setFrom(paths)
    }

    void setTemplates(Iterable<?> paths) {
        this.templates.setFrom(paths)
    }

    void setOutputDir(Provider<? extends Directory> dir) {
        this.outputDir.set(dir)
    }

    void setOutputDir(Directory dir) {
        this.outputDir.set(dir)
    }

    void setOutputDir(File dir) {
        this.outputDir.set(dir)
    }

    void database(String db) {
        setDatabase(db)
    }

    void setDatabase(String db) {
        this.database.set(db)
    }

}