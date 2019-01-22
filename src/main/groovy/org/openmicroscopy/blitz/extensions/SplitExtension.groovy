package org.openmicroscopy.blitz.extensions

import org.gradle.api.GradleException
import org.openmicroscopy.blitz.Language

import java.util.regex.Pattern

class SplitExtension {
    final String name

    Language language

    File outputDir

    File combinedDir

    String outputName

    void setLanguage(String languageString) {
        Language lang = Language.find(languageString)
        if (lang == null) {
            throw new GradleException("Unsupported language: ${languageString}")
        }
        this.language = lang
    }

    void setCombinedDir(String path) {
        combinedDir = new File(path)
    }

    void setOutputDir(String path) {
        outputDir = new File(path)
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

    void combinedDir(String path) {
        setCombinedDir(path)
    }

    void combinedDir(File path) {
        combinedDir = path
    }

    void outputDir(String dir) {
        setOutputDir(dir)
    }

    void outputDir(File dir) {
        outputDir = dir
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
        def lang = Language.values().find { lang ->
            name.toUpperCase().contains(lang.name())
        }
        if (lang) {
            this.language = lang
        }
    }
}