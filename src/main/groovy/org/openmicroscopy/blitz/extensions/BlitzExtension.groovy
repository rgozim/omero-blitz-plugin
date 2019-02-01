package org.openmicroscopy.blitz.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.openmicroscopy.dsl.extensions.FileGeneratorExtension
import org.openmicroscopy.dsl.extensions.FilesGeneratorExtension
import org.openmicroscopy.dsl.extensions.specs.DslExtension

class BlitzExtension implements DslExtension {

    final Project project

    final NamedDomainObjectContainer<FilesGeneratorExtension> files

    final NamedDomainObjectContainer<FileGeneratorExtension> file

    FileCollection omeXmlFiles

    FileCollection databaseTypes

    FileCollection templates

    String databaseType

    File outputDir

    File combinedOutputDir

    File template

    BlitzExtension(Project project,
                   NamedDomainObjectContainer<FilesGeneratorExtension> files,
                   NamedDomainObjectContainer<FileGeneratorExtension> file) {
        this.project = project
        this.files = files
        this.file = file
        this.omeXmlFiles = project.files()
        this.databaseTypes = project.files()
        this.templates = project.files()
    }

    void files(Action<? super NamedDomainObjectContainer<FilesGeneratorExtension>> action) {
        action.execute(files)
    }

    void file(Action<? super NamedDomainObjectContainer<FileGeneratorExtension>> action) {
        action.execute(file)
    }

    void outputDir(Object dir) {
        setOutputDir(dir)
    }

    void setOutputDir(Object dir) {
        this.outputDir = project.file(dir)
    }

    void combinedOutputDir(Object dir) {
        setCombinedOutputDir(dir)
    }

    void setCombinedOutputDir(Object dir) {
        this.combinedOutputDir = project.file(dir)
    }

    void template(String template) {
        setTemplate(template)
    }

    void template(File file) {
        setTemplate(file)
    }

    void setTemplate(String file) {
        setTemplate(new File(file))
    }

    void setTemplate(File file) {
        this.template = file
    }

}