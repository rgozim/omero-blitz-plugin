package org.openmicroscopy.blitz.extensions


import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.openmicroscopy.dsl.extensions.CodeExtension
import org.openmicroscopy.dsl.extensions.DslExtension
import org.openmicroscopy.dsl.extensions.ResourceExtension

class BlitzExtension extends DslExtension {

    File combinedOutputDir

    File template

    BlitzExtension(Project project,
                   NamedDomainObjectContainer<CodeExtension> code,
                   NamedDomainObjectContainer<ResourceExtension> resource) {
        super(project, code, resource)
    }

    void combinedOutputDir(String dir) {
        setCombinedOutputDir(dir)
    }

    void combinedOutputDir(File dir) {
        setCombinedOutputDir(dir)
    }

    void setCombinedOutputDir(String dir) {
        setCombinedOutputDir(new File(dir))
    }

    void setCombinedOutputDir(File dir) {
        this.combinedOutputDir = dir
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