import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project


class BlitzPlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final def GROUP = 'omero'

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {

        setupBlitzExtension(project)
        configureImportMappingsTask(project)
        configureCombineTask(project)
        configureSplitTasks(project)
    }


    def setupBlitzExtension(Project project) {
        // Add the 'blitz' extension object
        blitzExt = project.extensions.create('blitz', BlitzExtension, project)

        // Default config for blitz
        blitzExt.omeXmlFiles =
                project.files(dir: blitzExt.omeXmlDir, include: '**/*.ome.xml')
        blitzExt.combinedDir =
                project.file("${project.buildDir}/combined")

        // Add container for blitz
        blitzExt.extensions.add('api', project.container(SplitExtension))
    }

    def configureImportMappingsTask(Project project) {
        def task = project.task('importMappings', type: ImportMappingsTask) {
            group GROUP
            description 'Extracts mapping files from omero-model.jar'
        }

//        final def config = project.configurations.create("omeXmlFiles")
//                .setVisible(false)
//                .setDescription("The data artifacts to be processed for this plugin.");
//
//        config.defaultDependencies { DependencySet dependencies ->
//            dependencies.add project.dependencies.create("org.openmicroscopy:omero-model:1.0-SNAPSHOT")
//        }

        project.afterEvaluate {
            task.extractDir = blitzExt.omeXmlDir
        }
    }

    def configureCombineTask(Project project) {
        project.afterEvaluate {
            // Config for velocity
            VelocityExtension ve = project.dsl.velocity
            ve.loggerClassName = project.getLogger().getClass().getName()

            DslTask task = project.tasks.create("dslCombine", DslTask) {
                group = GROUP
                description = "Processes the combined.vm"
                dependsOn project.tasks.getByName("importMappings")
            }

            project.afterEvaluate {
                task.profile = "psql"
                task.template = loadCombineFile(project)
                task.velocityProperties = ve.data.get()
                task.omeXmlFiles = blitzExt.omeXmlFiles
                task.outputPath = blitzExt.combinedDir
                task.formatOutput = { st -> "${st.getShortname()}I.combined" }
            }
        }
    }

    def configureSplitTasks(Project project) {
        project.blitz.api.all { SplitExtension split ->
            String taskName = "split${split.name.capitalize()}"

            SplitTask task = project.tasks.create(taskName, SplitTask) {
                group = GROUP
                description = "Splits ${split.language} from .combined files"
            }

            project.afterEvaluate {
                task.combined = project.fileTree(dir: blitzExt.combinedDir, include: '**/*.combined')
                task.language = split.language
                task.outputDir = split.outputDir
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

