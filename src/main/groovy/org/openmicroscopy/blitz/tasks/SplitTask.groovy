package org.openmicroscopy.blitz.tasks

import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.copy.RegExpNameMapper
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.openmicroscopy.blitz.Language
import org.openmicroscopy.blitz.Prefix

import java.util.regex.Pattern

class SplitTask extends DefaultTask {

    public static final String DEFAULT_SOURCE_NAME = "(.*?)I[.]combined"
    public static final String DEFAULT_RESULT_NAME = "\$1I"

    /**
     * List of the languages we want to split from .combined files
     */
    @Input
    Language language

    /**
     * Directory to spit out source files
     */
    @OutputDirectory
    File outputDir

    /**
     * Collection of .combined files to process
     */
    @InputFiles
    FileCollection combined

    /**
     * Optional rename params (from, to) that support
     * regex
     */
    @Optional
    @Input
    Tuple2<String, String> renameParams

    void setLanguage(String language) {
        Language lang = Language.find(language)
        if (lang == null) {
            throw new GradleException("Unsupported language : ${language}")
        }
        this.language = lang
    }

    void language(String language) {
        setLanguage(language)
    }

    void language(Language lang) {
        this.language = lang
    }

    /**
     * Directory to spit out source files
     * @param dir
     * @return
     */
    void outputDir(File dir) {
        this.outputDir = dir
    }

    /**
     * Directory to spit out source files
     * @param dir
     * @return
     */
    void outputDir(String dir) {
        this.outputDir = new File(dir)
    }

    /**
     * Custom set method for concatenating FileCollections
     * @param combinedFiles
     */
    void combined(FileCollection combinedFiles) {
        if (this.combined) {
            this.combined = this.combinedFiles + combinedFiles
        } else {
            this.combined = combinedFiles
        }
    }

    void rename(Pattern sourceRegEx, String replaceWith) {
        this.rename(sourceRegEx.pattern(), replaceWith)
    }

    void rename(String sourceRegEx, String replaceWith) {
        this.renameParams = new Tuple2<>(sourceRegEx, replaceWith)
    }

    void setReplaceWith(String replaceWith) {
        this.rename(DEFAULT_SOURCE_NAME, replaceWith)
    }

    @TaskAction
    void action() {
        language.prefixes.each { Prefix prefix ->
            // Transform prefix enum to lower case for naming
            final def prefixName = prefix.name().toLowerCase()
            final def extension = prefix.extension

            // Assign default to rename
            def nameTransformer
            if (!renameParams) {
                nameTransformer = new RegExpNameMapper(DEFAULT_SOURCE_NAME,
                        DEFAULT_RESULT_NAME + ".${extension}")
            } else {
                nameTransformer = tupleToNameTransformer(prefix)
            }

            project.copy { c ->
                c.from combined
                c.into outputDir
                c.rename nameTransformer
                c.filter { String line -> filerLine(line, prefixName) }
            }
        }
    }

    def tupleToNameTransformer(Prefix prefix) {
        def first = renameParams.getFirst()
        if (textIsNullOrEmpty(first)) {
            first = DEFAULT_SOURCE_NAME
        }
        def second = renameParams.getSecond()
        if (textIsNullOrEmpty(second)) {
            second = DEFAULT_RESULT_NAME + ".${prefix.extension}"
        } else {
            second = formatSecond(prefix, second)
        }

        println "Renaming from: ${first} \t to: ${second}"
        return new RegExpNameMapper(first, second)
    }

    static def textIsNullOrEmpty(String text) {
        return !text?.trim()
    }

    static def formatSecond(Prefix prefix, String second) {
        final int index = FilenameUtils.indexOfExtension(second)
        if (index == -1) {
            return "${second}.${prefix.extension}"
        } else {
            return second
        }
    }

    static def filerLine(String line, String prefix) {
        return line.matches("^\\[all](.*)|^\\[${prefix}](.*)") ?
                line.replaceAll("^\\[all]\\s?|^\\[${prefix}]\\s?", "") :
                null
    }
}
