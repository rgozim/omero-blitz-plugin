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
