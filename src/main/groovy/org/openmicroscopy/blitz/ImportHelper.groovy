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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ResolvedArtifact

import static org.openmicroscopy.blitz.ConventionPluginHelper.getRuntimeClasspathConfiguration

@CompileStatic
class ImportHelper {

    static final String CONFIGURATION_NAME = "dataFiles"

    static ResolvedArtifact getOmeroModelArtifact(Project project) {
        ResolvedArtifact artifact = getOmeroModelFromCompileConfig(project)
        return artifact ?: getOmeroModelWithCustomConfig(project)
    }

    static ResolvedArtifact getOmeroModelFromCompileConfig(Project project) {
        Configuration runtimeClasspath = getRuntimeClasspathConfiguration(project)
        if (!runtimeClasspath) {
            return null
        }
        runtimeClasspath.resolvedConfiguration.resolvedArtifacts.find { it.name.contains("omero-model") }
    }

    static ResolvedArtifact getOmeroModelWithCustomConfig(Project project) {
        Configuration config = project.configurations.findByName(CONFIGURATION_NAME)
        if (!config) {
            config = createDataFilesConfig(project)
        }

        config.resolvedConfiguration.resolvedArtifacts.find { it.name.contains("omero-model") }
    }

    static Configuration getDataFilesConfig(Project project) {
        Configuration config = project.configurations.findByName(CONFIGURATION_NAME)
        if (!config) {
            config = createDataFilesConfig(project)
        }
        return config
    }

    static Configuration createDataFilesConfig(Project project) {
        project.buildscript.repositories.addAll(
                project.repositories.mavenLocal(),
                project.repositories.mavenCentral(),
                project.repositories.jcenter()
        )

        final Configuration config = project.getConfigurations().create(ImportHelper.CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(false)
                .setDescription("The data artifacts to be processed for this plugin.")

        config.defaultDependencies(new Action<DependencySet>() {
            void execute(DependencySet dependencies) {
                dependencies.add(project.getDependencies().create("org.openmicroscopy:omero-model:5.5.0-m4"))
            }
        })

        return config
    }
}
