package org.openmicroscopy.extensions

import org.gradle.api.Project
import org.openmicroscopy.Language

class SplitExtension {
    final String name
    final Project project

    Language language
    File outputDir
    File combinedDir

    void setLanguage(String language) {
        this.language = Language.valueOf(language.trim().toUpperCase())
    }

    void setOutputDir(Object dir) {
        this.outputDir = project.file(dir)
    }

    void setCombinedDir(Object combinedDir) {
        this.combinedDir = project.file(combinedDir)
    }

    void language(String language) {
        setLanguage(language)
    }

    void language(Language language) {
        this.language = language
    }

    void outputDir(Object dir) {
        setOutputDir(dir)
    }

    void combinedDir(Object dir) {
        setCombinedDir(dir)
    }

    SplitExtension(String name, Project project) {
        this.name = name
        this.project = project
    }
}