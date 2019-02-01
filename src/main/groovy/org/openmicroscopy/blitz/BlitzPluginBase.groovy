package org.openmicroscopy.blitz

import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.dsl.DslPluginBase
import org.openmicroscopy.dsl.extensions.MultiFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.SingleFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.factories.MultiFileGeneratorFactory
import org.openmicroscopy.dsl.factories.SingleFileGeneratorFactory
import org.openmicroscopy.dsl.tasks.FilesGeneratorTask

class BlitzPluginBase implements Plugin<Project> {

    public static final String GROUP = "omero-blitz"

    @Override
    void apply(Project project) {
        // BlitzExtension
        BlitzExtension blitz = createBaseExtension(project)

        // Create an inner dsl like syntax for blitz {}. Blitz is an extension
        // of dsl {}
        DslPluginBase.configure(project, blitz)

        // We need this extension
        VelocityExtension velocity = project.extensions.getByType(VelocityExtension)

        // Configure blitz
        configure(project, blitz, velocity)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    BlitzExtension createBaseExtension(Project project) {
        def code = project.container(MultiFileGeneratorExtension, new MultiFileGeneratorFactory(project))
        def resource = project.container(SingleFileGeneratorExtension, new SingleFileGeneratorFactory(project))

        // Create the dsl extension
        return project.extensions.create('blitz', BlitzExtension, project, code, resource)
    }

    static void configure(Project project, BlitzExtension blitz, VelocityExtension velocity) {
        registerCombinedTask(project, blitz, velocity)
    }

    static TaskProvider<FilesGeneratorTask> registerCombinedTask(Project project, BlitzExtension blitz,
                                                                 VelocityExtension velocity) {
        return project.tasks.register("generateCombinedFiles", FilesGeneratorTask, new Action<FilesGeneratorTask>() {
            @Override
            void execute(FilesGeneratorTask t) {
                t.group = DslPluginBase.GROUP
                t.description = "Generates .combined files"
                t.velocityProperties = velocity.data.get()
                t.omeXmlFiles = blitz.omeXmlFiles
                t.outputDir = blitz.combined.outputDir
                t.template = DslPluginBase.findFileInCollection(blitz.templates, blitz.combined.template)
                t.databaseType = DslPluginBase.findDatabaseType(blitz.databaseTypes, blitz.database)
                t.formatOutput = { SemanticType st -> "${st.getShortname()}I.combinedFiles" }
            }
        })
    }

}
