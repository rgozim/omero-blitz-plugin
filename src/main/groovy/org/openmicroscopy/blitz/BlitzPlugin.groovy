package org.openmicroscopy.blitz


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.tasks.DslMultiFileTask
import org.openmicroscopy.dsl.utils.OmeXmlLoader
import org.openmicroscopy.dsl.utils.ResourceLoader

class BlitzPlugin implements Plugin<Project> {

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        // Apply base plugin
        def basePlugin = project.plugins.apply(BlitzBasePlugin.class)

        // Get Extension
        blitzExt = basePlugin.blitzExt

        // Configure generateCombinedFiles task
        configureCombineTask(project)
    }

    /**
     * Creates task to process combined.vm template and spit out .combined
     * files for generating sources.
     * @param project
     * @return
     */
    def configureCombineTask(Project project) {
        project.afterEvaluate {
            def ve = new VelocityExtension(project)
            // Config for velocity
            ve.loggerClassName = project.getLogger().getClass().getName()

            def genCombineFilesTask = project.tasks.create("generateCombinedFiles", DslMultiFileTask) {
                group = BlitzBasePlugin.GROUP
                description = "Processes combined.vm and generates .combined files"
                profile = "psql"
                template = ResourceLoader.loadFile(project, "templates/combined.vm")
                velocityProperties = ve.data.get()
                outputPath = blitzExt.combinedDir
                formatOutput = { st -> "${st.getShortname()}I.combined" }
                omeXmlFiles = OmeXmlLoader.loadOmeXmlFiles(project)
            }

            // Find all splitXXX tasks
            def splitTaskNames = project.tasks.getNames().findAll {
                it.matches("split(.*)")
            }

            // And make them depend on generateCombinedFiles task
            splitTaskNames.each {
                def task = project.tasks.getByName(it)
                task.dependsOn genCombineFilesTask
            }
        }
    }
}

