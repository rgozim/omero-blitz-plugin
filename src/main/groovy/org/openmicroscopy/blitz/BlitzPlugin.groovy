/*
 * -----------------------------------------------------------------------------
 *  Copyright (C) 2019 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * ------------------------------------------------------------------------------
 */
package org.openmicroscopy.blitz

import groovy.transform.CompileStatic
import ome.dsl.SemanticType
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.openmicroscopy.api.ApiPlugin
import org.openmicroscopy.api.extensions.ApiExtension
import org.openmicroscopy.api.tasks.SplitTask
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.dsl.DslPlugin
import org.openmicroscopy.dsl.extensions.BaseFileConfig
import org.openmicroscopy.dsl.extensions.DslExtension
import org.openmicroscopy.dsl.extensions.MultiFileConfig
import org.openmicroscopy.dsl.tasks.FilesGeneratorTask
import org.openmicroscopy.dsl.tasks.GeneratorBaseTask

import javax.inject.Inject
import java.util.concurrent.Callable

import static org.openmicroscopy.dsl.FileTypes.PATTERN_DB_TYPE
import static org.openmicroscopy.dsl.FileTypes.PATTERN_OME_XML

@CompileStatic
class BlitzPlugin implements Plugin<Project> {

    private static final Logger Log = Logging.getLogger(BlitzPlugin)

    static final String TASK_IMPORT_MODEL_RESOURCES = 'importModelResources'

    Map<String, BaseFileConfig> fileGeneratorConfigMap = [:]

    final ObjectFactory objectFactory

    final ProviderFactory providerFactory

    @Inject
    BlitzPlugin(ObjectFactory objectFactory, ProviderFactory providerFactory) {
        this.objectFactory = objectFactory
        this.providerFactory = providerFactory
    }

    @Override
    void apply(Project project) {
        final BlitzExtension blitz = project.extensions.create("blitz", BlitzExtension, project)

        final TaskProvider<Sync> importTask = registerImportTask(project)

        project.plugins.withType(JavaPlugin) {
            // Configure task to import omero data
            importTask.configure(new Action<Sync>() {
                @Override
                void execute(Sync t) {
                    Configuration config = ImportHelper.getDataFilesConfig(project)
                    def artifact = config.resolvedConfiguration
                            .resolvedArtifacts
                            .find { it.name.contains("omero-model") }

                    t.dependsOn(config)
                    t.with(createImportModelResSpec(project, artifact.file))
                }
            })
        }

        project.plugins.withType(DslPlugin) {
            // Get the [ task.name | extension ] map
            fileGeneratorConfigMap =
                    project.properties.get("fileGeneratorConfigMap") as Map<String, BaseFileConfig>

            DslExtension dsl = project.extensions.getByType(DslExtension)

            // Configure extensions of ome.dsl plugin
            dsl.outputDir.set(blitz.outputDir)
            dsl.omeXmlFiles.from(importTask)
            dsl.databaseTypes.from(importTask)

            // Add generateCombinedFilesTask
            // registerGenerateCombinedTask(project, ome.dsl)
            dsl.multiFile.add(createGenerateCombinedFilesConfig(project, blitz).get())

            // Set each generator task to depend on
            project.tasks.withType(GeneratorBaseTask).configureEach(new Action<GeneratorBaseTask>() {
                @Override
                void execute(GeneratorBaseTask gbt) {
                    gbt.dependsOn importTask
                }
            })
        }

        project.plugins.withType(ApiPlugin) {
            TaskProvider<FilesGeneratorTask> generateCombinedTask =
                    getGenerateCombinedTask(project)

            ApiExtension api = project.extensions.getByType(ApiExtension)
            api.outputDir.set(blitz.outputDir)
            api.combinedFiles.from(generateCombinedTask)

            project.tasks.withType(SplitTask).configureEach(new Action<SplitTask>() {
                @Override
                void execute(SplitTask st) {
                    st.dependsOn generateCombinedTask
                }
            })
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


    Provider<MultiFileConfig> createGenerateCombinedFilesConfig(Project project, BlitzExtension blitz) {
        providerFactory.provider(new Callable<MultiFileConfig>() {
            @Override
            MultiFileConfig call() throws Exception {
                def extension = new MultiFileConfig("combined", project)
                extension.template = "combined.vm"
                extension.outputDir = "${project.buildDir}/${blitz.database.get()}/combined"
                extension.formatOutput = { SemanticType st -> "${st.getShortname()}I.combined" }
                return extension
            }
        })
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

//    TaskProvider<FilesGeneratorTask> registerGenerateCombinedTask(Project project, DslExtension ome.dsl) {
//        String taskName = "generateCombined" + ome.dsl.database.get().capitalize()
//        project.tasks.register(taskName, FilesGeneratorTask, new Action<FilesGeneratorTask>() {
//            @Override
//            void execute(FilesGeneratorTask t) {
//                t.with {
//                    dependsOn
//                    velocityConfig.set(ome.dsl.velocity.data)
//                    outputDir.set(project.layout.buildDirectory.dir("combined"))
//                    template.set(findTemplateProvider(ome.dsl.templates, new File("combined.vm")))
//                    databaseType.set(findDatabaseTypeProvider(ome.dsl.databaseTypes, ome.dsl.database))
//                    mappingFiles.from(ome.dsl.omeXmlFiles)
//                }
//            }
//        })
//    }
//
//    Provider<RegularFile> findDatabaseTypeProvider(FileCollection collection, Property<String> type) {
//        providerFactory.provider(new Callable<RegularFile>() {
//            @Override
//            RegularFile call() throws Exception {
//                RegularFileProperty result = objectFactory.fileProperty()
//                result.set(DslBase.findDatabaseType(collection, type.get()))
//                result.get()
//            }
//        })
//    }
//
//    Provider<RegularFile> findTemplateProvider(FileCollection collection, File file) {
//        providerFactory.provider(new Callable<RegularFile>() {
//            @Override
//            RegularFile call() throws Exception {
//                RegularFileProperty result = objectFactory.fileProperty()
//                result.set(DslBase.findTemplate(collection, file))
//                result.get()
//            }
//        })
//    }

}
