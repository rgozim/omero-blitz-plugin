package org.openmicroscopy.blitz

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.blitz.extensions.SplitExtension
import org.openmicroscopy.blitz.tasks.SplitTask

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
        configureSplitTasks(project)
    }

    def setupBlitzExtension(Project project, ExtensionContainer baseExt) {
        // Add the 'blitz' extension object
        blitzExt = baseExt.create('blitz', BlitzExtension, project)

        // Add container for blitz
        blitzExt.extensions.add('api',
                project.container(SplitExtension, { new SplitExtension(it, project) }))
    }

    def configureSplitTasks(Project project) {
        project.afterEvaluate {
            blitzExt.api.all { SplitExtension split ->
                String taskName = "split${split.name.capitalize()}"
                def task = project.tasks.create(taskName, SplitTask)
                task.dependsOn project.tasks.getByName("generateCombinedFiles")
                task.group = GROUP
                task.description = "Splits ${split.language} from .combined files"
                task.combined = project.fileTree(dir: blitzExt.combinedDir, include: '**/*.combined')
                task.language = split.language
                task.outputDir = split.outputDir
                task.replaceWith = split.outputName
            }
        }
    }


}
