import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

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

            project.afterEvaluate {
                def task = project.tasks.create('processCombine', DslTask) {
                    group = GROUP
                    description = "Processes the combined.vm"
                    profile = "psql"
                    velocityProperties = ve.data.get()
                    template = new File("src/main/resources/templates/combined.vm")
                    omeXmlFiles = blitzExt.omeXmlFiles
                    outputPath = blitzExt.combinedDir
                    formatOutput = { st -> "${st.getShortname()}I.combined" }
                    dependsOn 'importMappings'
                }
            }
        }
    }

    def configureSplitTasks(Project project) {
        project.blitz.api.all { SplitExtension split ->
            def taskName = "split${split.name.capitalize()}"

            // Create task and assign group name
            def task = project.tasks.create(taskName, SplitTask) {
                group = GROUP
                description = "Splits ${split.language} from .combined files"
            }

            // Assign property values to task inputs
            project.afterEvaluate {
                task.combined = project.fileTree(dir: blitzExt.combinedDir, include: '**/*.combined')
                task.language = split.language
                task.outputDir = split.outputDir
            }

            if (project.plugins.hasPlugin(JavaPlugin)) {
                // Ensure the dsltask runs before compileJava
                project.tasks.getByName("compileJava").dependsOn(taskName)
            }
        }
    }

    def loadCombineFile() {
        def classLoader = this.getClass().getClassLoader()
        return new File(classLoader.getResource("templates/combined.vm").getFile())
    }
}

