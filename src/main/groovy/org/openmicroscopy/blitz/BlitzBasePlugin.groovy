package org.openmicroscopy.blitz

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.Delete
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.blitz.extensions.SplitExtension
import org.openmicroscopy.blitz.tasks.SplitTask
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.tasks.DslMultiFileTask
import org.openmicroscopy.dsl.utils.OmeXmlLoader
import org.openmicroscopy.dsl.utils.ResourceLoader

class BlitzBasePlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final def GROUP = "omero-blitz"

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        init(project, project.extensions)
    }

    def init(Project project, ExtensionContainer baseExt) {
        setupBlitzExtension(project, baseExt)
        configureCombineTask(project)
        configureSplitTasks(project)
    }

    def setupBlitzExtension(Project project, ExtensionContainer baseExt) {
        // Add the 'blitz' extension object
        blitzExt = baseExt.create('blitz', BlitzExtension, project)

        // Add container for blitz
        blitzExt.extensions.add('api', project.container(SplitExtension))
    }

    /**
     * Creates task to process combined.vm template and spit out .combined
     * files for generating sources.
     * @param project
     * @return
     */
    def configureCombineTask(Project project) {
        project.afterEvaluate {
            // Config for velocity
            def ve = new VelocityExtension(project)
            ve.loggerClassName = project.getLogger().getClass().getName()

            def task = project.tasks.create("generateCombinedFiles", DslMultiFileTask) {
                group = GROUP
                description = "Processes combined.vm and generates .combined files"
                profile = "psql"
                template = ResourceLoader.loadFile(project, "templates/combined.vm")
                velocityProperties = ve.data.get()
                outputPath = blitzExt.combinedPath
                formatOutput = { st -> "${st.getShortname()}I.combined" }
                omeXmlFiles = OmeXmlLoader.loadOmeXmlFiles(project)
            }

            // Add cleanup tasks
            addCleanTask(project, task.name, task.outputPath)
        }
    }

    def configureSplitTasks(Project project) {
        project.afterEvaluate {
            blitzExt.api.all { SplitExtension split ->
                String taskName = "split${split.name.capitalize()}"
                def task = project.tasks.create(taskName, SplitTask) {
                    group = GROUP
                    description = "Splits ${split.language} from .combined files"
                    combined = project.fileTree(
                            dir: getDir(split.combinedPath, blitzExt.combinedPath),
                            include: '**/*.combined'
                    )
                    outputDir = getDir(split.outputPath, blitzExt.outputPath)
                    language = split.language
                    replaceWith = split.outputName
                }

                // All split tasks require the .combined files to work.
                task.dependsOn getGenerateCombinedFilesTask(project)

                // Add cleanup tasks
                addCleanTask(project, task.name, task.outputDir)
            }
        }
    }

    def addCleanTask(Project project, String taskName, File toDelete) {
        String cleanTaskName = "clean${taskName.capitalize()}"
        project.tasks.create(cleanTaskName, Delete) {
            group GROUP
            delete toDelete
            shouldRunAfter project.tasks.getByName('clean')
        }
    }

    def getGenerateCombinedFilesTask(Project project) {
        return project.tasks.findByName("generateCombinedFiles")
    }

    static def getDir(File expected, File fallback) {
        if (expected) {
            if (expected.isAbsolute()) {
                return expected
            } else if (fallback) {
                return new File(fallback, expected.path)
            }
        }
        return fallback
    }
}
