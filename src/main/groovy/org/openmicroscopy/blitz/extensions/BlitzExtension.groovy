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
package org.openmicroscopy.blitz.extensions

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

class BlitzExtension {

    private final Project project

    final Property<String> database

    final DirectoryProperty outputDir

    BlitzExtension(Project project) {
        this.project = project
        this.database = project.objects.property(String)
        this.outputDir = project.objects.directoryProperty()

        this.database.convention("psql")
        this.outputDir.convention(project.layout.projectDirectory.dir("src/psql"))
    }

}
