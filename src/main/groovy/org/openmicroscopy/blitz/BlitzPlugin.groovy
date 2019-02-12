package org.openmicroscopy.blitz

import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.openmicroscopy.api.ApiPlugin
import org.openmicroscopy.api.extensions.ApiExtension
import org.openmicroscopy.dsl.DslPlugin
import org.openmicroscopy.dsl.extensions.BaseFileConfig
import org.openmicroscopy.dsl.extensions.DslExtension
import org.openmicroscopy.dsl.extensions.MultiFileConfig
import org.openmicroscopy.dsl.tasks.GeneratorBaseTask

import static org.openmicroscopy.dsl.DslPluginBase.findDatabaseType
import static org.openmicroscopy.dsl.FileTypes.PATTERN_DB_TYPE
import static org.openmicroscopy.dsl.FileTypes.PATTERN_OME_XML

class BlitzPlugin implements Plugin<Project> {

    private static final Logger Log = Logging.getLogger(BlitzPlugin)

    static final String TASK_EVALUATE_DSL_INPUTS = "evaluateDslInputs"

    static final String TASK_IMPORT_MODEL_RESOURCES = 'importModelResources'

    Map<String, BaseFileConfig> fileGeneratorConfigMap = [:]

    @Override
    void apply(Project project) {
        ResolvedArtifact artifact = ImportHelper.getOmeroModelWithCustomConfig(project)
        if (!artifact) {
            throw new GradleException("omero-model artifact not found")
        }

        // Register import model task. Configure when JavaPlugin is found
        registerImportTask(project, artifact.file)

        // Apply Dslplugin
        project.plugins.apply(DslPlugin)

        // Get the [ task.name | extension ] map
        fileGeneratorConfigMap =
                project.properties.get("fileGeneratorConfigMap") as Map<String, BaseFileConfig>

        DslExtension dsl = project.extensions.getByType(DslExtension)

        // Add generateCombinedFilesTask
        dsl.multiFile.add(createGenerateCombinedFilesConfig(project, dsl))

        createEvaluateDslInputsTask(project, dsl)
        configureForApiPlugin(project, dsl)
        configureForJavaPlugin(project)

        project.tasks.withType(GeneratorBaseTask).configureEach { task ->
            task.dependsOn project.tasks.named(TASK_EVALUATE_DSL_INPUTS)
        }
    }

    TaskProvider<Sync> registerImportTask(Project project, Object from) {
        project.tasks.register(TASK_IMPORT_MODEL_RESOURCES, Sync, new Action<Sync>() {
            @Override
            void execute(Sync sync) {
                sync.into("$project.buildDir/import")
                sync.with(createImportModelResSpec(project, from))
            }
        })
    }

    TaskProvider<Task> createEvaluateDslInputsTask(Project project, DslExtension dsl) {
        final TaskProvider importTask =
                project.tasks.named(TASK_IMPORT_MODEL_RESOURCES)

        project.tasks.register(TASK_EVALUATE_DSL_INPUTS, new Action<Task>() {
            @Override
            void execute(Task task) {
                task.dependsOn importTask
                task.doLast {
                    FileCollection importedFiles = project.files(importTask)

                    fileGeneratorConfigMap.entrySet().each { Map.Entry<String, BaseFileConfig> entry ->
                        project.tasks.named(entry.key, GeneratorBaseTask).configure(new Action<GeneratorBaseTask>() {
                            @Override
                            void execute(GeneratorBaseTask t) {
                                t.mappingFiles.from(importedFiles)
                                t.databaseType.set(findDatabaseType(importedFiles, dsl.database.get()))
                            }
                        })
                    }
                }
            }
        })
    }

    MultiFileConfig createGenerateCombinedFilesConfig(Project project, DslExtension dsl) {
        def extension = new MultiFileConfig("combined", project)
        extension.with {
            template = "combined.vm"
            outputDir = project.file("${project.buildDir}/${dsl.database.get()}/combined")
            formatOutput = { SemanticType st -> "${st.getShortname()}I.combined" }
        }
        extension
    }

    void configureForJavaPlugin(Project project) {
        project.plugins.withType(JavaPlugin) { JavaPlugin java ->
            // Register task to import omero data
            project.tasks.named(TASK_IMPORT_MODEL_RESOURCES).configure { Sync t ->
                ResolvedArtifact artifact = ImportHelper.getOmeroModelFromCompileConfig(project)
                if (artifact) {
                    t.with(createImportModelResSpec(project, artifact.file))
                }
            }
        }
    }

    void configureForApiPlugin(Project project, DslExtension dsl) {
        project.plugins.withType(ApiPlugin) { ApiPlugin plugin ->
            // Set output dir of API to equal DSL
            ApiExtension api = project.extensions.getByType(ApiExtension) as ApiExtension
            api.outputDir = dsl.outputDir
            api.combinedFiles.setFrom(project.fileTree(
                    dir: "${project.buildDir}/${dsl.database.get()}/combined",
                    include: "**/*.combined"
            ))

            /*project.tasks.register("evaluateApiInputs") { Task task ->
                def combinedFilesExt = fileGeneratorConfigMap.find {
                    it.key.toLowerCase().contains("combined")
                }
                def generateCombined = project.tasks.named(combinedFilesExt.key)

                task.dependsOn
                task.doLast {
                    project.tasks.withType(SplitTask).configureEach(new Action<SplitTask>() {
                        @Override
                        void execute(SplitTask t) {
                            t.source = generateCombined
                        }
                    })
                }
            }*/
        }
    }

    CopySpec createImportModelResSpec(Project project, Object from) {
        project.copySpec(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.with {
                    includeEmptyDirs = false
                    into("mappings", new Action<CopySpec>() {
                        @Override
                        void execute(CopySpec spec) {
                            spec.from(project.zipTree(from))
                            spec.include(PATTERN_OME_XML)
                            spec.eachFile { FileCopyDetails copyDetails ->
                                copyDetails.path = "mappings/$copyDetails.name"
                            }
                        }
                    })
                    into("databaseTypes", new Action<CopySpec>() {
                        @Override
                        void execute(CopySpec spec) {
                            spec.from(project.zipTree(from))
                            spec.include(PATTERN_DB_TYPE)
                            spec.eachFile { FileCopyDetails copyDetails ->
                                copyDetails.path = "databaseTypes/$copyDetails.name"
                            }
                        }
                    })
                }
            }
        })
    }

}
