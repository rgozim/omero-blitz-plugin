package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project
import org.openmicroscopy.dsl.extensions.DslExtension

class BlitzExtension extends DslExtension {

    File combinedDir

    File template

    String modelVersion

    BlitzExtension(Project project) {
        super(project)
    }

    void combinedDir(Object dir) {
        setCombinedDir(dir)
    }

    void setCombinedDir(Object dir) {
        combinedDir = project.file(dir)
    }

    void template(Object p) {
        setTemplate(p)
    }

    void setTemplate(Object p) {
        template = project.file(p)
    }

    void modelVersion(String version) {
        modelVersion = version
    }

}