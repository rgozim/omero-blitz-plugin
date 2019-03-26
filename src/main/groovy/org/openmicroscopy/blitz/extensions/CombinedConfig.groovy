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
import org.gradle.api.provider.Property

class CombinedConfig {

    private final Project project

    final Property<File> outputDir

    final Property<File> template

    CombinedConfig(Project project) {
        this.project = project
        this.outputDir = project.objects.property(File)
        this.template = project.objects.property(File)
    }

    void outputDir(String file) {
        setOutputDir(file)
    }

    void outputDir(File file) {
        setOutputDir(file)
    }

    void setOutputDir(String file) {
        setOutputDir(new File(file))
    }

    void setOutputDir(File file) {
        this.outputDir.set(file)
    }

    void template(String file) {
        setTemplate(file)
    }

    void template(File file) {
        setTemplate(file)
    }

    void setTemplate(String file) {
        setTemplate(new File(file))
    }

    void setTemplate(File file) {
        this.template.set(file)
    }

}
