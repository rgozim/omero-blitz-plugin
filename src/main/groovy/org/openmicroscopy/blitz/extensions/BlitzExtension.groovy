package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project

class BlitzExtension {

    static final def COMBINED_FILES_DIR = "combined"

    static final def OME_XML_FILES_DIR = "extracted"

    final Project project

    File combinedDir

    File omeXmlDir

    File outputDir

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

    BlitzExtension(Project project) {
        this.project = project
        this.combinedDir = project.file("${project.buildDir}/${COMBINED_FILES_DIR}")
        this.omeXmlDir = project.file("${project.buildDir}/$OME_XML_FILES_DIR")
    }
}