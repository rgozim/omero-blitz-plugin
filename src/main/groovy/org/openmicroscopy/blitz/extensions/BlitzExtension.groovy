package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project

class BlitzExtension {

    static final def COMBINED_FILES_DIR = "combined"

    final Project project

    File combinedPath

    File outputPath

    void setCombinedPath(String dir) {
        this.combinedPath = new File(dir)
    }
    
    void setOutputPath(String dir) {
        setOutputPath(new File(dir))
    }

    void setOutputPath(File path) {
        if (!path.isAbsolute()) {
            outputPath = project.file(path)
        } else {
            outputPath = path
        }
    }

    void combinedPath(String dir) {
        setCombinedPath(dir)
    }

    void outputPath(String dir) {
        setOutputPath(dir)
    }

    void outputPath(File dir) {
        setOutputPath(dir)
    }

    BlitzExtension(Project project) {
        this.project = project
        this.combinedPath = project.file("${project.buildDir}/${COMBINED_FILES_DIR}")
    }
}