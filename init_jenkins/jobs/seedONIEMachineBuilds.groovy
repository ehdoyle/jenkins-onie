// Seed job seedONIEMachineBuilds.groovy
// Does: Instantiates the Jenkins job that will generate all the the Onie
//       machine builds.
//       The buildComponents.groovy script that this runs does the parsing
//       of gitarchives and then creates individual jenkins jobs.
//
//   Note: This has to be configured as a pipelineJob, or you get all kinds
//       of creation failures.

// JobDSL is the plug in that allows code to generate Jenkins entries.
import javaposse.jobdsl.plugin.*

node("master") {
	// if using this checkout, set lookupStrategy to be JENKINS_ROOT, and it
	// will execute the jobs/buildComponents.groovy from the git repo, rather
	// than the local copy.

	// this gets checked out in a dir called machine_builds_as_pipeline.
	// this was created by the mother seed job and does not have the
	// jobs/buildComponents.groovy script, or any of the other files that were
	// checked out by the MotherSeedJob to create this.

	// So we check ourselves out _again_ to populate this directory.

/*
 // This code caused IO errors further on down. Branch is hardcoded for now...

	// Using Java here:  https://docs.oracle.com/javase/9/docs/api/java/lang/System.html
	def home_dir = System.getenv("JENKINS_HOME")

	// Load configuration from properties file, which the Dockerfile copied from config/
	// into the container under props
	def jenkins_base_props_file = new File("${home_dir}/props/jenkins.properties")

	// This is Groovy http://docs.groovy-lang.org/latest/html/gapi/groovy/util/ConfigSlurper.html
	def jenkins_properties = new ConfigSlurper().parse(jenkins_base_props_file.toURI().toURL())

	println "TRY ${jenkins_properties}"
	println "branch ${jenkins_properties.seedjobs.components.branch}"
*/
	
	println "--> seedONIEMachineBuilds.groovy creating machine build jobs"

	// Make sure we're on the same branch for this second checkout 

	// NOW jobs/buildComponents.groovy is a valid path

	// NOTE if it is not a vailid path, then this fails and returns success, having
	// successfully done nothing.
	


def scriptDir = getClass().protectionDomain.codeSource.location.path

String myDir = new File(".").getAbsoluteFile().getParent()

	println "--> seedONIEMachineBuilds.groovy creating machine build jobs in ${scriptDir} myDir is ${myDir}"


	// this describes the seed job entry that will be created.
	// (again, _running_ this job after it's instantiated will then
	// create all the pacakge jobs.

	//targets definitely runs the jenkins-seed-jobs/jobs/*groovy script.
	// why cant it find the one that is in this directory?

	// Documentation:
	// 	https://jenkins.io/doc/pipeline/steps/job-dsl/

	// Set lookupStrategy ot SEED_JOB to look in local directories,
	// vs JENKINS_ROOT. This will let it find the local
	// buildComponents.Groovy
    step([
        $class: 'ExecuteDslScripts',
        targets: "jobs/buildMachineTargets.groovy",
        ignoreMissingFiles: true,
        ignoreExisting: false,
        removedJobAction: RemovedJobAction.DELETE,
        removedViewAction: RemovedViewAction.DELETE,
        lookupStrategy: LookupStrategy.SEED_JOB,
        additionalClasspath: "src/main/groovy\nlib/*.jar"
    ])

	// You can get some basic debug this way:
	// Commented out for cleanliness
	// Left for sanity checking/education
	/*
	// Debug to see what variables are available:
	println binding.variables
	println "My Dir is ${myDir}"
	println "Script dir is ${scriptDir}"
	stage('build') {
        echo "Debug example code"
        sh "echo Hello from the shell"
        sh "pwd"
        sh "ls"
		
    }
	 */
	println "--> seedONIEMachineBuilds.groovy done generating jobs..."
}