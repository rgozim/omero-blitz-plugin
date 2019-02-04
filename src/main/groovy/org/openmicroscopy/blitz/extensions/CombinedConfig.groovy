package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project
import org.gradle.api.provider.Property

class CombinedConfig {

    private final Project project

    final Property<File> outputDir

    final Property<File> template

    CombinedConfig(Project project) {
        this.project = project
        this.outputDir = project.objects.property(File)
        this.template = project.objects.property(File)
    }

    void outputDir(String file) {
        setOutputDir(file)
    }

    void outputDir(File file) {
        setOutputDir(file)
    }

    void setOutputDir(String file) {
        setOutputDir(new File(file))
    }

    void setOutputDir(File file) {
        this.outputDir.set(file)
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
        this.template.set(file)
    }

}
