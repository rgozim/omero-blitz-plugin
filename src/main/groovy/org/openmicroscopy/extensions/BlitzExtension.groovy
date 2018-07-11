package org.openmicroscopy.extensions

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class BlitzExtension {
    static final def OME_XML_FILES_DIR = "extracted"
    static final def COMBINED_FILES_DIR = "combined"

    FileCollection omeXmlFiles

    File combinedDir

    void setOmeXmlFiles(FileCollection files) {
        omeXmlFiles(files)
    }

    void omeXmlFiles(FileCollection files) {
        if (this.omeXmlFiles) {
            this.omeXmlFiles = this.omeXmlFiles + files
        } else {
            this.omeXmlFiles = files
        }
    }

    void setCombinedDir(String dir) {
        combinedDir(dir)
    }

    void combinedDir(String dir) {
        this.combinedDir = new File(dir)
    }

    BlitzExtension(Project project) {
        // Set defaults
        omeXmlFiles = project.fileTree(
                dir: "${project.buildDir}/${OME_XML_FILES_DIR}",
                include: "**/*.ome.xml"
        )
        combinedDir = project.file("${project.buildDir}/${COMBINED_FILES_DIR}")
    }
}