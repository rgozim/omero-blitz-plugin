package org.openmicroscopy.blitz

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionContainer
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.blitz.extensions.SplitExtension
import org.openmicroscopy.blitz.tasks.SplitTask

class BlitzPluginBase implements Plugin<Project> {

    private static final def Log = Logging.getLogger(BlitzPluginBase)

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

    void setupBlitzExtension(Project project, ExtensionContainer baseExt) {
        // Add the 'blitz' extension object
        blitzExt = baseExt.create('blitz', BlitzExtension, project)

        // Add container for blitz
        blitzExt.extensions.add('api', project.container(SplitExtension))
    }

    void configureSplitTasks(Project project) {
        blitzExt.api.all { SplitExtension split ->
            String taskName = "split${split.name.capitalize()}"

            project.tasks.register(taskName, SplitTask) { task ->
                task.dependsOn project.tasks.named("generateCombinedFiles")
                task.group = GROUP
                task.description = "Splits ${split.language} from .combined files"
                task.combined = project.fileTree(
                        dir: handleFile(blitzExt.combinedDir, split.combinedDir),
                        include: '**/*.combined'
                )
                task.outputDir = handleFile(blitzExt.outputDir, split.outputDir)
                task.language = split.language
                task.replaceWith = split.outputName
            }
        }
    }

    File handleFile(File dslFile, File singleFile) {
        if (!singleFile) {
            return dslFile
        }

        if (singleFile.isFile() || !dslFile) {
            return singleFile
        }

        if (dslFile.isFile()) {
            return dslFile
        }

        // DSL file is not a file, but a path.
        // Single file is also not a file or absolute so also a path
        return new File(dslFile, "$singleFile")
    }

    File getDir(File expected, File fallback) {
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
