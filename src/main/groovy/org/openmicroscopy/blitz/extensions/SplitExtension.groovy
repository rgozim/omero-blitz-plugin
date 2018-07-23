package org.openmicroscopy.blitz.extensions

import org.gradle.api.GradleException
import org.openmicroscopy.blitz.Language

import java.util.regex.Pattern

class SplitExtension {
    final String name

    Language language

    File outputPath

    File combinedPath

    String outputName

    void setLanguage(String languageString) {
        Language lang = Language.find(languageString)
        if (lang == null) {
            throw new GradleException("Unsupported language: ${languageString}")
        }
        this.language = lang
    }

    void setCombinedPath(String path) {
        combinedPath = new File(path)
    }

    void setOutputPath(String path) {
        outputPath = new File(path)
    }

    void setOutputName(String name) {
        outputName = name
    }

    void language(String language) {
        setLanguage(language)
    }

    void language(Language lang) {
        language = lang
    }

    void combinedPath(String path) {
        setCombinedPath(path)
    }

    void combinedPath(File path) {
        combinedPath = path
    }

    void outputPath(String dir) {
        setOutputPath(dir)
    }

    void outputPath(File dir) {
        outputPath = dir
    }

    void outputName(String name) {
        setOutputName(name)
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

    SplitExtension(String name) {
        this.name = name
    }
}