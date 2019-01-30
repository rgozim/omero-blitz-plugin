package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.openmicroscopy.blitz.extensions.ApiExtension
import org.openmicroscopy.blitz.tasks.SplitTask

import java.nio.file.Paths

@CompileStatic
class ApiPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Apply our base plugin
        project.plugins.apply(ApiPluginBase)

        // Get the extension
        ApiExtension api = project.extensions.getByType(ApiExtension)

        // Configure convention
        api.combinedFiles = project.fileTree(dir: "$project.buildDir/combined", include: "**/*.combined")

        // Set default output dir
        api.outputDir = "src/generated"

        // React to java plugin
        configureForJava(project, api)
    }

    static void configureForJava(Project project, ApiExtension api) {
        project.plugins.withType(JavaPlugin) { JavaPlugin java ->
            // Configure split tasks with java language to output to java dir
            project.tasks.withType(SplitTask).configureEach(new Action<SplitTask>() {
                @Override
                void execute(SplitTask splitTask) {
                    if (splitTask.language == Language.JAVA && !splitTask.outputDir.isAbsolute()) {
                        splitTask.outputDir = Paths.get("$api.outputDir", "java", "$splitTask.outputDir")
                    }
                }
            })
        }
    }


}
