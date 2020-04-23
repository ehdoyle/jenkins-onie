// Import YAML, also grabbing it from upstream here
@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

// If this script is under init_jenkins, it will generate jobs.
// In that case, the mother_seed_job will run it as a DSL script
// with init_jenkins/*.groovy

//
// This script executes in DSL context
//

// Loading defaults for job creation
println "--> Configuring Jenkins Machine build jobs."

// /var/jenkins_home/workspace/SeedJobs/Mother_Seed_Cumulus_Linux_Image_Build
def seed_job_workspace = binding.variables.WORKSPACE
println "--> jenkins-machine-seed/init_jenkins/component_seed.groovy:  Seed workspace is ${seed_job_workspace}"

// Create subfolder to hold seed jobs for machine builds
def seed_jobs_folder = "Seed_ONIE"
// Create the subfolder
folder(seed_jobs_folder)

def init_jobs_folder = "init_jenkins"
// folder(init_jobs_folder)

//
// this will create the seed job that generates pacakge builds.
//

// Jenkins Example: accessing binding variables
def releaseTrack = "${binding.variables.JENKINS_RELEASE_TRACK}"

println "--> In component_seed.groovy creating Onie machine seed job."

//Make sure this directory name matches what's checked out of seed jobs
pipelineJob("${seed_jobs_folder}/onie_builds")
{
	println "--> component_seed.groovy Running seed job from to create ${releaseTrack} jobs/seedONIEMachineBuilds.groovy"

	// Clickable text
	displayName("Create ${releaseTrack} component/machine build jobs.")

	// This text is at the top once you click on the job
	description("\nCreate machine build jobs from gitarchives file.")

	// additional optional configuration
	/*
	logRotator
    {
        numToKeep(50)
        artifactNumToKeep(50)
    }
	 */
//    triggers
//    {
//        cron("*/30 * * * *")
//    }


    definition {
		// Continuation-Passing-Style transform?
		cps {
			//			script( readFileFromWorkspace("${init_jobs_folder}/jobs/seedONIEMachineBuilds.groovy"))
			script( readFileFromWorkspace("${init_jobs_folder}/jobs/seedONIEMachineBuilds.groovy"))			
			sandbox()
		}
    }
}

// See if instantiating 'job' works to have an example of another way to do things
// Job for creating/updating Jenkins slaves
println "--> Creating job for refreshing Jenkins component build slaves"
job("${seed_jobs_folder}/refresh_jenkins_component_slaves")
{
	println "---> Reading from workspace configureBuildNodes.groovy as type JOB "

	// This is the clickable link text for the job
	displayName("Machine Seed Create ${releaseTrack} Build Nodes")
	
	// This text is at the top once you click on the job
	description("\nCreate ${releaseTrack} nodes in Jenkins for Cumulus Linux machine builds.")

    label("master")
    logRotator
    {
        numToKeep(5)
        artifactNumToKeep(5)
    }
    steps 
    {
        systemGroovyCommand(readFileFromWorkspace("${init_jobs_folder}/jobs/configureBuildNodes.groovy"))
        {
            binding("SEED_WORKSPACE", "$WORKSPACE")
            sandbox()
        }
    }
}



// To add other pipeline jobs, just cut and paste the
// above example
