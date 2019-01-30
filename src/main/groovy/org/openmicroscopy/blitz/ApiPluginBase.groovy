package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionContainer
import org.openmicroscopy.blitz.extensions.ApiExtension
import org.openmicroscopy.blitz.extensions.SplitExtension
import org.openmicroscopy.blitz.tasks.SplitTask

@CompileStatic
class ApiPluginBase implements Plugin<Project> {

    private static final def Log = Logging.getLogger(ApiPluginBase)

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final String GROUP = "omero-api"

    @Override
    void apply(Project project) {
        ApiExtension api = createBaseExtension(project, project.extensions)
        configureSplitTasks(project, api)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    ApiExtension createBaseExtension(Project project, ExtensionContainer extensions) {
        def language = project.container(SplitExtension)

        return extensions.create("api", ApiExtension, project, language)
    }

    static void configureSplitTasks(Project project, ApiExtension api) {
        api.language.all { SplitExtension split ->
            String taskName = "split${split.name.capitalize()}"
            project.tasks.register(taskName, SplitTask, new Action<SplitTask>() {
                @Override
                void execute(SplitTask t) {
                    t.group = GROUP
                    t.description = "Splits ${split.language} from .combinedFiles files"
                    t.outputDir = getOutputDir(api.outputDir, split.outputDir)
                    t.combinedFiles = api.combinedFiles + split.combinedFiles
                    t.language = split.language
                    t.replaceWith = split.outputName
                }
            })
        }
    }

    static File getOutputDir(File dslFile, File singleFile) {
        if (!singleFile) {
            return dslFile
        }

        if (!dslFile || singleFile.isAbsolute()) {
            return singleFile
        }

        return new File(dslFile, "$singleFile")
    }
}
