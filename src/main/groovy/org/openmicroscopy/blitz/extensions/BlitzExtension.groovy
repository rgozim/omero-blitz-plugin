package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project

class BlitzExtension {

    static final def COMBINED_FILES_DIR = "combined"

    static final def OME_XML_FILES_DIR = "extracted"

    final Project project

    String databaseType

    File combinedDir

    File omeXmlDir

    File outputDir

    File template

    String modelVersion

    BlitzExtension(Project project) {
        this.project = project
        this.combinedDir = project.file("${project.buildDir}/${COMBINED_FILES_DIR}")
        this.omeXmlDir = project.file("${project.buildDir}/$OME_XML_FILES_DIR")
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

    void modelVersion(String version) {
        modelVersion = version
    }

}