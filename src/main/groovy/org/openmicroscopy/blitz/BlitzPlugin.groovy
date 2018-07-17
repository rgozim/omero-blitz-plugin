package org.openmicroscopy.blitz

import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.openmicroscopy.blitz.extensions.BlitzExtension
import org.openmicroscopy.blitz.extensions.SplitExtension
import org.openmicroscopy.blitz.tasks.ImportMappingsTask
import org.openmicroscopy.blitz.tasks.SplitTask
import org.openmicroscopy.dsl.extensions.VelocityExtension
import org.openmicroscopy.dsl.tasks.DslMultiFileTask

class BlitzPlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final def GROUP = "omero-blitz"

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        init(project)
    }

    def init(Project project) {
        this.init(project, project.extensions)
    }

    def init(Project project, ExtensionContainer baseExt) {
        setupBlitzExtension(project, baseExt)
        configureImportMappingsTask(project)
        configureCombineTask(project)
        configureSplitTasks(project)
    }

    def setupBlitzExtension(Project project, ExtensionContainer baseExt) {
        // Add the 'blitz' extension object
        blitzExt = baseExt.create('blitz', BlitzExtension, project)

        // Add container for blitz
        blitzExt.extensions.add('api', project.container(SplitExtension, {
            String name -> new SplitExtension(name, project)
        }))
    }

    /**
     * Creates task to extract .ome.xml files from omero-model
     * and place them in {@code omeXmlDir}
     * @param project
     * @return
     */
    def configureImportMappingsTask(Project project) {
        project.afterEvaluate {
            project.tasks.create('importOmeXmlFiles', ImportMappingsTask) {
                group GROUP
                description 'Extracts mapping files from omero-model.jar'
                extractDir "${project.buildDir}/${BlitzExtension.OME_XML_FILES_DIR}"
            }
        }
    }

    /**
     * Creates task to process combined.vm template and spit out .combined
     * files for generating sources.
     * @param project
     * @return
     */
    def configureCombineTask(Project project) {
        project.afterEvaluate {
            VelocityExtension ve = project.extensions
                    .findByName("velocity") as VelocityExtension
            if (ve) {
                // Config for velocity
                ve.loggerClassName = project.getLogger().getClass().getName()
            }

            project.tasks.create("generateCombinedFiles", DslMultiFileTask) {
                dependsOn project.tasks.getByName("importOmeXmlFiles")
                group = GROUP
                description = "Processes combined.vm and generates .combined files"
                profile = "psql"
                template = loadCombineFile(project)
                velocityProperties = ve.data.get()
                omeXmlFiles = blitzExt.omeXmlFiles
                outputPath = blitzExt.combinedDir
                formatOutput = { st -> "${st.getShortname()}I.combined" }
            }
        }
    }

    def configureSplitTasks(Project project) {
        project.afterEvaluate {
            blitzExt.api.all { SplitExtension split ->
                String taskName = "split${split.name.capitalize()}"
                def task = project.tasks.create(taskName, SplitTask)
                task.dependsOn project.tasks.getByName("generateCombinedFiles")
                task.group = GROUP
                task.description = "Splits ${split.language} from .combined files"
                task.combined = project.fileTree(dir: blitzExt.combinedDir, include: '**/*.combined')
                task.language = split.language
                task.outputDir = split.outputDir
                task.replaceWith = split.outputName
            }
        }
    }

    def loadCombineFile(Project project) {
        final def fileLocation = "templates/combined.vm"
        final def outPutDir = "${project.buildDir}/resources/${fileLocation}"

        // Check if combined file exists
        def result = new File(outPutDir)
        if (!result.exists()) {
            def classLoader = getClass().getClassLoader()
            def inputStream = classLoader.getResourceAsStream(fileLocation)
            // Copy it to the projects build directory
            FileUtils.copyInputStreamToFile(inputStream, result)
        }
        return result
    }

}

