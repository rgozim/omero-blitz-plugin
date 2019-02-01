package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project

class CombinedConfig {

    private final Project project

    File outputDir

    File template

    CombinedConfig(Project project) {
        this.project = project
    }

    void outputDir(Object dir) {
        setOutputDir(dir)
    }

    void setOutputDir(Object dir) {
        this.combinedOutputDir = project.file(dir)
    }

    void template(String file) {
        setTemplate(file)
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
