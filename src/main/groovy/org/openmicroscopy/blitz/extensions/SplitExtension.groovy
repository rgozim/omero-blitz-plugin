package org.openmicroscopy.blitz.extensions

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.openmicroscopy.blitz.Language

import java.util.regex.Pattern

class SplitExtension {
    final String name

    final Project project

    Language language

    File outputDir

    File combinedDir

    String outputName

    void setLanguage(String language) {
        Language lang = Language.find(language)
        if (lang == null) {
            throw new GradleException("Unsupported language: ${language}")
        }
        this.language = lang
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

    void language(Language lang) {
        this.language = lang
    }

    void outputDir(Object dir) {
        setOutputDir(dir)
    }

    void combinedDir(Object dir) {
        setCombinedDir(dir)
    }

    def rename(Pattern sourceRegEx, String replaceWith) {
        this.nameTransformer = new Tuple(
                sourceRegEx,
                replaceWith
        )
    }

    def rename(String sourceRegEx, String replaceWith) {
        this.nameTransformer = new Tuple(
                sourceRegEx,
                replaceWith
        )
    }

    def outputName(String name) {
        this.outputName = name
    }

    SplitExtension(String name, Project project) {
        this.name = name
        this.project = project
    }
}