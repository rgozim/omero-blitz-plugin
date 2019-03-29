/*
 * -----------------------------------------------------------------------------
 *  Copyright (C) 2019 University of Dundee & Open Microscopy Environment.
 *  All rights reserved.
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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.openmicroscopy.api.ApiPlugin
import org.openmicroscopy.api.extensions.ApiExtension
import org.openmicroscopy.api.tasks.SplitTask
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.blitz.tasks.ImportResourcesTask
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

    public static final String TASK_IMPORT_MAPPINGS = "importMappings"

    public static final String TASK_IMPORT_DATABASE_TYPES = "importDatabaseTypes"

    Map<String, BaseFileConfig> fileGeneratorConfigMap = [:]

    final ObjectFactory objectFactory

    final ProviderFactory providerFactory

    private Project project

    @Inject
    BlitzPlugin(ObjectFactory objectFactory, ProviderFactory providerFactory) {
        this.objectFactory = objectFactory
        this.providerFactory = providerFactory
    }

    @Override
    void apply(Project project) {
        this.project = project

        BlitzExtension blitz =
                project.extensions.create("blitz", BlitzExtension, project)

        TaskProvider<ImportResourcesTask> importMappings = registerImportMappings()

        TaskProvider<ImportResourcesTask> importDbTypesTask = registerImportDbTypes()

        project.plugins.withType(DslPlugin) {
            // Get the [ task.name | extension ] map
            fileGeneratorConfigMap =
                    project.properties.get("fileGeneratorConfigMap") as Map<String, BaseFileConfig>

            DslExtension dsl = project.extensions.getByType(DslExtension)

            // Configure extensions of ome.dsl plugin
            dsl.outputDir.set(blitz.outputDir)
            dsl.omeXmlFiles.from(importMappings)
            dsl.databaseTypes.from(importDbTypesTask)

            // Add generateCombinedFilesTask
            dsl.multiFile.addLater(createGenerateCombinedFilesConfig(blitz))

            // Set each generator task to depend on
            project.tasks.withType(GeneratorBaseTask).configureEach(new Action<GeneratorBaseTask>() {
                @Override
                void execute(GeneratorBaseTask gbt) {
                    gbt.dependsOn importMappings, importDbTypesTask
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

    Provider<MultiFileConfig> createGenerateCombinedFilesConfig(BlitzExtension blitz) {
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

    TaskProvider<ImportResourcesTask> registerImportMappings() {
        project.tasks.register(TASK_IMPORT_MAPPINGS, ImportResourcesTask, new Action<ImportResourcesTask>() {
            @Override
            void execute(ImportResourcesTask t) {
                t.with {
                    config = ImportHelper.getConfigurationForOmeroModel(project)
                    extractDir = "$project.buildDir/mappings"
                    pattern = PATTERN_OME_XML
                }
            }
        })
    }

    TaskProvider<ImportResourcesTask> registerImportDbTypes() {
        project.tasks.register(TASK_IMPORT_DATABASE_TYPES, ImportResourcesTask, new Action<ImportResourcesTask>() {
            @Override
            void execute(ImportResourcesTask t) {
                t.with {
                    config = ImportHelper.getConfigurationForOmeroModel(project)
                    extractDir = "$project.buildDir/databaseTypes"
                    pattern = PATTERN_DB_TYPE
                }
            }
        })
    }

}
