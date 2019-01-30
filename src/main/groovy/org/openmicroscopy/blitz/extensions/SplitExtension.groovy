package org.openmicroscopy.blitz.extensions

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.openmicroscopy.blitz.Language

import java.util.regex.Pattern

class SplitExtension {

    final String name

    final Project project

    FileCollection combinedFiles

    Language language

    File outputDir

    String outputName

    SplitExtension(String name, Project project) {
        this.name = name
        this.project = project
        this.combinedFiles = project.files()

        // Optionally set language based on name of extension
        Language lang = Language.values().find { lang ->
            name.toUpperCase().contains(lang.name())
        }
        if (lang) {
            this.language = lang
        }
    }

    void combinedFiles(FileCollection files) {
        setCombinedFiles(files)
    }

    void combinedFiles(Object... files) {
        setCombinedFiles(files)
    }

    void setCombinedFiles(FileCollection files) {
        this.combinedFiles = files
    }

    void setCombinedFiles(Object... files) {
        this.combinedFiles = project.files(files)
    }

    void language(String language) {
        setLanguage(language)
    }

    void language(Language lang) {
        setLanguage(lang)
    }

    void setLanguage(Language lang) {
        language = lang
    }

    void setLanguage(String languageString) {
        Language lang = Language.find(languageString)
        if (lang == null) {
            throw new GradleException("Unsupported language: ${languageString}")
        }
        this.language = lang
    }

    void outputDir(String dir) {
        setOutputDir(dir)
    }

    void outputDir(File dir) {
        outputDir = dir
    }

    void setOutputDir(String path) {
        outputDir = new File(path)
    }

    void outputName(String name) {
        setOutputName(name)
    }

    void setOutputName(String name) {
        outputName = name
    }

    void rename(Pattern sourceRegEx, String replaceWith) {
        this.nameTransformer = new Tuple(
                sourceRegEx,
                replaceWith
        )
    }

    void rename(String sourceRegEx, String replaceWith) {
        this.nameTransformer = new Tuple(
                sourceRegEx,
                replaceWith
        )
    }

}