package org.openmicroscopy.blitz

import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.dsl.DslPluginBase
import org.openmicroscopy.dsl.extensions.FileGeneratorExtension
import org.openmicroscopy.dsl.extensions.FilesGeneratorExtension
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.factories.FileGeneratorExtFactory
import org.openmicroscopy.dsl.factories.FilesGeneratorExtFactory
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

        // Configure blitz
        configure(project, blitz)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    BlitzExtension createBaseExtension(Project project) {
        def code = project.container(FilesGeneratorExtension, new FilesGeneratorExtFactory(project))
        def resource = project.container(FileGeneratorExtension, new FileGeneratorExtFactory(project))

        // Create the dsl extension
        return project.extensions.create('blitz', BlitzExtension, project, code, resource)
    }

    static void configure(Project project, BlitzExtension blitz) {
        registerCombinedTask(project, blitz)
    }

    static TaskProvider<FilesGeneratorTask> registerCombinedTask(Project project, BlitzExtension blitz) {
        return project.tasks.register("generateCombinedFiles", FilesGeneratorTask, new Action<FilesGeneratorTask>() {
            @Override
            void execute(FilesGeneratorTask t) {
                t.omeXmlFiles = blitz.omeXmlFiles
                t.databaseTypes = blitz.databaseTypes
                t.databaseType = blitz.databaseType
                t.outputDir = blitz.combinedOutputDir
                t.template = DslPluginBase.getFileInCollection(blitz.templates, blitz.template)
                t.formatOutput = { SemanticType st -> "${st.getShortname()}I.combinedFiles" }
                t.velocityProperties = new VelocityExtension(project).data.get()
                t.description = "Processes combinedFiles.vm and generates .combinedFiles files"
                t.group = GROUP
            }
        })
    }

}
