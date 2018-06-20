package org.openmicroscopy

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class BlitzExtension {
    static final def OME_XML_FILES_DIR = "extracted"
    static final def COMBINED_FILES_DIR = "combined"

    FileCollection omeXmlFiles
    File omeXmlDir
    File combinedDir

    void setOmeXmlFiles(FileCollection files) {
        if (this.omeXmlFiles) {
            this.omeXmlFiles = this.omeXmlFiles + files
        } else {
            this.omeXmlFiles = files
        }
    }

    void setCombinedDir(String dir) {
        this.combinedDir = new File(dir)
    }

    BlitzExtension(Project project) {
        // Set defaults
        omeXmlDir = project.file("${project.buildDir}/${OME_XML_FILES_DIR}")
        combinedDir = project.file("${project.buildDir}/${COMBINED_FILES_DIR}")
    }
}