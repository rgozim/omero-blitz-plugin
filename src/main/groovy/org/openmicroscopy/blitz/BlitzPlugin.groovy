package org.openmicroscopy.blitz

import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.openmicroscopy.api.ApiPlugin
import org.openmicroscopy.api.extensions.ApiExtension
import org.openmicroscopy.api.tasks.SplitTask
import org.openmicroscopy.dsl.DslPlugin
import org.openmicroscopy.dsl.extensions.BaseFileConfig
import org.openmicroscopy.dsl.extensions.DslExtension
import org.openmicroscopy.dsl.extensions.MultiFileConfig
import org.openmicroscopy.dsl.tasks.FilesGeneratorTask
import org.openmicroscopy.dsl.tasks.GeneratorBaseTask

import javax.inject.Inject

import static org.openmicroscopy.dsl.FileTypes.PATTERN_DB_TYPE
import static org.openmicroscopy.dsl.FileTypes.PATTERN_OME_XML

class BlitzPlugin implements Plugin<Project> {

    private static final Logger Log = Logging.getLogger(BlitzPlugin)

    static final String TASK_IMPORT_MODEL_RESOURCES = 'importModelResources'

    Map<String, BaseFileConfig> fileGeneratorConfigMap = [:]

    final DirectoryProperty sharedOutputDir

    final ObjectFactory objectFactory

    @Inject
    BlitzPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        this.sharedOutputDir = objectFactory.directoryProperty()
    }

    @Override
    void apply(Project project) {
        TaskProvider<Sync> importTask = registerImportTask(project)

        project.plugins.withType(JavaPlugin) { JavaPlugin java ->
            // Register task to import omero data
            importTask.configure { Sync t ->
                def artifact = ImportHelper.getOmeroModelFromCompileConfig(project)
                t.with(createImportModelResSpec(project, artifact.file))
            }
        }

        project.plugins.withType(DslPlugin) {
            // Get the [ task.name | extension ] map
            fileGeneratorConfigMap =
                    project.properties.get("fileGeneratorConfigMap") as Map<String, BaseFileConfig>

            DslExtension dsl = project.extensions.getByType(DslExtension)

            // Set the shared output directory
            sharedOutputDir.set(dsl.outputDir)

            // Configure extensions of dsl plugin
            dsl.omeXmlFiles.from importTask
            dsl.databaseTypes.from importTask

            // Add generateCombinedFilesTask
            dsl.multiFile.add(createGenerateCombinedFilesConfig(project, dsl))

            // Set each generator task to depend on
            project.tasks.withType(GeneratorBaseTask).configureEach { task ->
                task.dependsOn importTask
            }
        }

        project.plugins.withType(ApiPlugin) {
            TaskProvider<FilesGeneratorTask> generateCombinedTask =
                    getGenerateCombinedTask(project)

            ApiExtension api = project.extensions.getByType(ApiExtension)
            api.outputDir.set(sharedOutputDir)
            api.combinedFiles.from(generateCombinedTask)

            project.tasks.withType(SplitTask).configureEach { task ->
                task.dependsOn generateCombinedTask
            }
        }
    }

    TaskProvider<Sync> registerImportTask(Project project) {
        project.tasks.register(TASK_IMPORT_MODEL_RESOURCES, Sync, new Action<Sync>() {
            @Override
            void execute(Sync s) {
                s.into("$project.buildDir/import")
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

    TaskProvider<FilesGeneratorTask> getGenerateCombinedTask(Project project) {
        def combinedFilesExt = fileGeneratorConfigMap.find {
            it.key.toLowerCase().contains("combined")
        }
        project.tasks.named(combinedFilesExt.key, FilesGeneratorTask)
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
