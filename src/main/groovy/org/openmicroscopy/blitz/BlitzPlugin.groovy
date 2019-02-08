package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.dsl.DslPlugin
import org.openmicroscopy.dsl.extensions.MultiFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.OperationExtension
import org.openmicroscopy.dsl.extensions.SingleFileGeneratorExtension
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.factories.MultiFileGeneratorFactory
import org.openmicroscopy.dsl.factories.SingleFileGeneratorFactory
import org.openmicroscopy.dsl.tasks.FileGeneratorTask
import org.openmicroscopy.dsl.tasks.FilesGeneratorTask
import org.openmicroscopy.dsl.tasks.GeneratorBaseTask

import static org.openmicroscopy.dsl.DslPluginBase.findDatabaseType
import static org.openmicroscopy.dsl.DslPluginBase.findTemplate
import static org.openmicroscopy.dsl.DslPluginBase.getOutputDirProvider
import static org.openmicroscopy.dsl.DslPluginBase.getOutputFileProvider
import static org.openmicroscopy.dsl.FileTypes.PATTERN_DB_TYPE
import static org.openmicroscopy.dsl.FileTypes.PATTERN_OME_XML
import static org.openmicroscopy.dsl.FileTypes.PATTERN_TEMPLATE

@CompileStatic
class BlitzPlugin implements Plugin<Project> {

    private static final Logger Log = Logging.getLogger(BlitzPlugin)

    static final String GROUP = "omero-blitz"

    static final String EXTENSION_NAME_BLITZ = "blitz"

    static final String EXTENSION_NAME_VELOCITY = "velocity"

    static final String TASK_EVALUATE_DSL_INPUTS = "evaluateDslInputs"

    static final String TASK_IMPORT_MODEL_RESOURCES = 'importModelResources'

    static final String TASK_PREFIX_GENERATE = "generate"

    final Map<String, OperationExtension> fileGeneratorTasksMap = [:]

    @Override
    void apply(Project project) {
        if (project.plugins.withType(DslPlugin)) {
            throw new GradleException("Blitz overrides dsl plugin.")
        }

        Configuration config = ImportHelper.createDataFilesConfig(project)

        ResolvedArtifact artifact = config.resolvedConfiguration.resolvedArtifacts.find {
            it.name.contains("omero-model")
        }
        if (!artifact) {
            throw new GradleException("omero-model artifact not found")
        }

        registerImportTask(project, artifact.file)
        createBaseExtension(project)
    }

    void createBaseExtension(Project project) {
        def multiFileGenContainer = project.container(MultiFileGeneratorExtension,
                new MultiFileGeneratorFactory(project))
        def singleFileGenContainer = project.container(SingleFileGeneratorExtension,
                new SingleFileGeneratorFactory(project))

        // Create the dsl extension
        def blitz = project.extensions.create(EXTENSION_NAME_BLITZ, BlitzExtension,
                project, multiFileGenContainer, singleFileGenContainer)

        def velocity = ((ExtensionAware) blitz).extensions
                .create(EXTENSION_NAME_VELOCITY, VelocityExtension, project)

        // Set default template files dir
        blitz.templates =
                project.fileTree(dir: "src/main/resources/templates", include: PATTERN_TEMPLATE)

        // Set default for velocity config
        velocity.checkEmptyObjects = false

        multiFileGenContainer.whenObjectAdded { MultiFileGeneratorExtension ext ->
            addMultiFileGenTask(project, blitz, ext)
        }

        singleFileGenContainer.whenObjectAdded { SingleFileGeneratorExtension ext ->
            addSingleFileGenTask(project, blitz, ext)
        }

        // Manually add a task via dsl
        registerCombinedTask(project, blitz)

        // Add
        createEvaluateDslInputsTask(project, blitz)

        // React to java plugin, set source dirs and resource dirs
        configureForJavaPlugin(project, blitz)
    }

    TaskProvider<Sync> registerImportTask(Project project, Object from) {
        project.tasks.register(TASK_IMPORT_MODEL_RESOURCES, Sync, new Action<Sync>() {
            @Override
            void execute(Sync sync) {
                sync.with {
                    includeEmptyDirs = false
                    into("$project.buildDir")
                    into("mappings", new Action<CopySpec>() {
                        @Override
                        void execute(CopySpec copySpec) {
                            copySpec.from(project.zipTree(from))
                            copySpec.include(PATTERN_OME_XML)
                            copySpec.eachFile { FileCopyDetails copyDetails ->
                                copyDetails.path = "mappings/$copyDetails.name"
                            }
                        }
                    })
                    into("databaseTypes", new Action<CopySpec>() {
                        @Override
                        void execute(CopySpec copySpec) {
                            copySpec.from(project.zipTree(from))
                            copySpec.include(PATTERN_DB_TYPE)
                            copySpec.eachFile { FileCopyDetails copyDetails ->
                                copyDetails.path = "databaseTypes/$copyDetails.name"
                            }
                        }
                    })
                }
            }
        })
    }

    void addMultiFileGenTask(Project project, BlitzExtension blitz, MultiFileGeneratorExtension ext) {
        def taskName = TASK_PREFIX_GENERATE + ext.name.capitalize() + blitz.database.get().capitalize()
        project.tasks.register(taskName, FilesGeneratorTask, new Action<FilesGeneratorTask>() {
            @Override
            void execute(FilesGeneratorTask t) {
                t.group = GROUP
                t.formatOutput = ext.formatOutput
                t.outputDir = getOutputDirProvider(blitz.outputDir, ext.outputDir)
                t.dependsOn project.tasks.named(TASK_EVALUATE_DSL_INPUTS)
            }
        })
        fileGeneratorTasksMap.put(taskName, ext)
    }

    void addSingleFileGenTask(Project project, BlitzExtension blitz, SingleFileGeneratorExtension ext) {
        def taskName = TASK_PREFIX_GENERATE + ext.name.capitalize() + blitz.database.get().capitalize()
        project.tasks.register(taskName, FileGeneratorTask, new Action<FileGeneratorTask>() {
            @Override
            void execute(FileGeneratorTask t) {
                t.group = GROUP
                t.outputFile = getOutputFileProvider(blitz.outputDir, ext.outputFile)
                t.dependsOn project.tasks.named(TASK_EVALUATE_DSL_INPUTS)
            }
        })
        fileGeneratorTasksMap.put(taskName, ext)
    }

    void registerCombinedTask(Project project, BlitzExtension blitz) {
        def extension = new MultiFileGeneratorExtension("combinedFiles", project)
        extension.with { ext ->
            ext.template = project.file("src/main/resources/templates/combined.vm")
            ext.outputDir = project.file("$project.buildDir/combined")
            ext.formatOutput = { SemanticType st -> "${st.getShortname()}I.combinedFiles" }
        }
        blitz.multiFile.add(extension)
    }

    TaskProvider<Task> createEvaluateDslInputsTask(Project project, BlitzExtension blitz) {
        final TaskProvider importTask =
                project.tasks.named(TASK_IMPORT_MODEL_RESOURCES)

        project.tasks.register(TASK_EVALUATE_DSL_INPUTS, new Action<Task>() {
            @Override
            void execute(Task task) {
                task.dependsOn importTask
                task.doLast {
                    fileGeneratorTasksMap.entrySet().each { Map.Entry<String, OperationExtension> entry ->
                        project.tasks.named(entry.key).configure { GeneratorBaseTask t ->
                            t.omeXmlFiles = importTask
                            t.template = findTemplate(project, blitz.templates, entry.value.template.get())
                            t.databaseType = findDatabaseType(project, project.files(importTask), blitz.database.get())
                        }
                    }
                }
            }
        })
    }

    void configureForJavaPlugin(Project project, BlitzExtension blitz) {
        // Configure default outputDir
        project.plugins.withType(JavaPlugin) { JavaPlugin java ->
            JavaPluginConvention javaConvention =
                    project.convention.getPlugin(JavaPluginConvention)

            SourceSet main =
                    javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

            main.java.srcDirs blitz.outputDir.dir("java")
            main.resources.srcDirs blitz.outputDir.dir("resources")

            // Configure compileJava to depend on dsl tasks
            project.tasks.named("compileJava").configure { JavaCompile jc ->
                jc.dependsOn project.tasks.withType(GeneratorBaseTask)
            }
        }
    }

}
