package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.dsl.DslPluginBase
import org.openmicroscopy.dsl.extensions.MultiFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.SingleFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.factories.MultiFileGeneratorFactory
import org.openmicroscopy.dsl.factories.SingleFileGeneratorFactory

@CompileStatic
class BlitzPluginBase implements Plugin<Project> {

    static final String GROUP = "omero-blitz"

    static final String EXTENSION_NAME_BLITZ = "blitz"

    @Override
    void apply(Project project) {
        // BlitzExtension
        BlitzExtension blitz = createBaseExtension(project)
        VelocityExtension velocity = DslPluginBase.createVelocityExtension(project, blitz)

        // Create an inner dsl like syntax for blitz {}. Blitz is an extension
        // of dsl {}
        // DslPluginBase.configure(project, blitz, velocity)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    BlitzExtension createBaseExtension(Project project) {
        def code = project.container(MultiFileGeneratorExtension, new MultiFileGeneratorFactory(project))
        def resource = project.container(SingleFileGeneratorExtension, new SingleFileGeneratorFactory(project))

        // Create the dsl extension
        return project.extensions.create(EXTENSION_NAME_BLITZ, BlitzExtension, project, code, resource)
    }

    static BlitzExtension getBlitzExtension(ExtensionContainer extensions) {
        return extensions.getByType(BlitzExtension)
    }

    static VelocityExtension getVelocityExtension(Project project) {
        return getVelocityExtension(getBlitzExtension(project.extensions))
    }

    static VelocityExtension getVelocityExtension(BlitzExtension blitz) {
        return ((ExtensionAware) blitz).extensions.getByType(VelocityExtension)
    }

}
