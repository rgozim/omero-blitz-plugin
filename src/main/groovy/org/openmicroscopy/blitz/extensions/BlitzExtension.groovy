package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection

class BlitzExtension {

    static final def COMBINED_FILES_DIR = "combined"

    static final def OME_XML_FILES_DIR = "extracted"

    final Project project

    final ConfigurableFileCollection omeXmlFiles

    String databaseType

    File combinedDir

    File omeXmlDir

    File outputDir

    File template

    BlitzExtension(Project project) {
        this.project = project
        this.omeXmlFiles = project.files()
        this.combinedDir = project.file("${project.buildDir}/${COMBINED_FILES_DIR}")
        this.omeXmlDir = project.file("${project.buildDir}/$OME_XML_FILES_DIR")
    }

    void omeXmlFiles(FileCollection files) {
        setOmeXmlFiles(files)
    }

    void omeXmlFiles(Object... files) {
        setOmeXmlFiles(files)
    }

    void setOmeXmlFiles(FileCollection files) {
        omeXmlFiles.setFrom(files)
    }

    void setOmeXmlFiles(Object... files) {
        setOmeXmlFiles(project.files(files))
    }

    void databaseType(String type) {
        databaseType = type
    }

    void setCombinedDir(Object dir) {
        combinedDir = project.file(dir)
    }

    void setOmeXmlDir(Object dir) {
        outputDir = project.file(dir)
    }

    void setOutputDir(Object dir) {
        outputDir = project.file(dir)
    }

    void combinedDir(Object dir) {
        setCombinedDir(dir)
    }

    void omeXmlDir(Object dir) {
        setOmeXmlDir(dir)
    }

    void outputDir(Object dir) {
        setOutputDir(dir)
    }

    void setTemplate(Object p) {
        template = project.file(p)
    }

    void template(Object p) {
        setTemplate(p)
    }

}