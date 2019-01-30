package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.openmicroscopy.api.ApiPlugin
import org.openmicroscopy.api.tasks.SplitTask
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.blitz.tasks.ImportResourcesTask
import org.openmicroscopy.dsl.DslPlugin
import org.openmicroscopy.dsl.DslPluginBase
import org.openmicroscopy.dsl.FileTypes
import org.openmicroscopy.dsl.extensions.CodeExtension
import org.openmicroscopy.dsl.extensions.ResourceExtension
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.factories.CodeFactory
import org.openmicroscopy.dsl.factories.ResourceFactory
import org.openmicroscopy.dsl.tasks.DslBaseTask
import org.openmicroscopy.dsl.tasks.DslMultiFileTask

@CompileStatic
class BlitzPlugin implements Plugin<Project> {

    private static final Logger Log = Logging.getLogger(BlitzPlugin)

    public static final String GROUP = "omero-blitz"

    @Override
    void apply(Project project) {
        if (project.plugins.withType(DslPlugin)) {
            throw new GradleException("DSL plugin overrides Blitz conventions")
        }

        // BlitzExtension
        BlitzExtension blitz = createBaseExtension(project)

        // Create an inner dsl like syntax for blitz {}
        DslPluginBase.configure(project, blitz)

        // ApiPluginBase. ((ExtensionAware) blitz).extensions

        configureForApiPlugin(project)

        // Configure default conventions for blitz plugin
        configureConventions(project, blitz)

        // React to java plugin inclusion
        configureForJavaPlugin(project, blitz)
    }

    BlitzExtension createBaseExtension(Project project) {
        def code = project.container(CodeExtension, new CodeFactory(project))
        def resource = project.container(ResourceExtension, new ResourceFactory(project))

        // Create the dsl extension
        return project.extensions.create('blitz', BlitzExtension, project, code, resource)
    }

    private static void configureConventions(Project project, BlitzExtension blitz) {
        // Set default dir for files generated by .combinedFiles files
        blitz.outputDir = "src/generated"

        blitz.combinedOutputDir = "${project.buildDir}/combined"

        blitz.omeXmlFiles = project.fileTree(dir: "${project.buildDir}/mappings",
                include: FileTypes.PATTERN_OME_XML)

        blitz.databaseTypes = project.fileTree(dir: "${project.buildDir}/properties",
                include: FileTypes.PATTERN_DB_TYPE)

        project.tasks.register("importMappings", ImportResourcesTask, new Action<ImportResourcesTask>() {
            @Override
            void execute(ImportResourcesTask t) {
                t.group = GROUP
                t.extractDir = "$project.buildDir/mappings"
                t.pattern = FileTypes.PATTERN_OME_XML
            }
        })

        project.tasks.register("importDatabaseTypes", ImportResourcesTask, new Action<ImportResourcesTask>() {
            @Override
            void execute(ImportResourcesTask t) {
                t.group = GROUP
                t.extractDir = "$project.buildDir/properties"
                t.pattern = FileTypes.PATTERN_DB_TYPE
            }
        })

        project.tasks.register("generateCombinedFiles", DslMultiFileTask, new Action<DslMultiFileTask>() {
            @Override
            void execute(DslMultiFileTask t) {
                t.dependsOn project.tasks.named("importMappings"),
                        project.tasks.named("importDatabaseTypes")
                t.group = GROUP
                t.description = "Processes combinedFiles.vm and generates .combinedFiles files"
                t.template = DslPluginBase.getFileInCollection(blitz.templates, blitz.template)
                t.omeXmlFiles = blitz.omeXmlFiles
                t.databaseTypes = blitz.databaseTypes
                t.outputDir = blitz.combinedOutputDir
                t.databaseType = blitz.databaseType
                t.formatOutput = { SemanticType st -> "${st.getShortname()}I.combinedFiles" }
                t.velocityProperties = new VelocityExtension(project).data.get()
            }
        })

        // Set any DSL tasks to depend on import tasks
        project.tasks.withType(DslBaseTask).configureEach(new Action<DslBaseTask>() {
            @Override
            void execute(DslBaseTask dslTask) {
                dslTask.dependsOn project.tasks.named("importMappings"),
                        project.tasks.named("importDatabaseTypes")
            }
        })

        project.plugins.withType(JavaPlugin) {
            // Configure compileJava to depend on dsl tasks
            project.tasks.named("compileJava").configure { JavaCompile jc ->
                jc.dependsOn project.tasks.withType(DslBaseTask)
            }
        }
    }

    private static void configureForApiPlugin(Project project) {
        project.plugins.withType(ApiPlugin) { ApiPlugin java ->
            // Set split tasks to depend on "generateCombinedFiles"
            project.tasks.withType(SplitTask).configureEach(new Action<SplitTask>() {
                @Override
                void execute(SplitTask t) {
                    t.dependsOn project.tasks.named("generateCombinedFiles")
                }
            })
        }
    }

    private static void configureForJavaPlugin(Project project, BlitzExtension blitz) {
        // Configure default outputDir
        project.plugins.withType(JavaPlugin) { JavaPlugin java ->
            // If we have java, resources are default here
            blitz.templates = project.fileTree(dir: "src/main/resources/templates",
                    include: FileTypes.PATTERN_TEMPLATE)

            JavaPluginConvention javaConvention =
                    project.convention.getPlugin(JavaPluginConvention)

            SourceSet main =
                    javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

            main.java.srcDirs "${blitz.outputDir}/java"
            main.resources.srcDirs "${blitz.outputDir}/resources"
        }
    }

}

