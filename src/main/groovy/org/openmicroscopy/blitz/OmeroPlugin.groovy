package org.openmicroscopy.blitz

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.openmicroscopy.blitz.extensions.OmeroExtension
import org.openmicroscopy.dsl.DslPlugin

class OmeroPlugin implements Plugin<Project> {

    OmeroExtension omeroExt

    @Override
    void apply(Project project) {

    }

    def setupDsl(Project project, ExtensionContainer baseExt) {
        omeroExt = baseExt.create('omero', OmeroExtension)

        // Add the dsl extension
        new DslPlugin().init(project, omeroExt)

        // Add the blitz plugin
        new BlitzPlugin().init(project, omeroExt)
    }
}
