package org.openmicroscopy.blitz

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.openmicroscopy.blitz.extensions.BlitzExtension

class BlitzPlugin implements Plugin<Project> {

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        // Apply base plugin
        def basePlugin = project.plugins.apply(BlitzBasePlugin)

        // Get BlitzExtension
        blitzExt = basePlugin.blitzExt
    }

}

