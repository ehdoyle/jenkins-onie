// File is jenkins-onie/jobs/buildMachineTargets.groovy
// This is jobdsl context

def curFileName="buildMachineTargets.groovy"

println "actual parsing code goes here."

def onieURL="https://github.com/opencomputeproject/onie.git"
def onieBranch="master"
def stageName="checkout ONIE"
println "---> ${curFileName} Checking out branch ${onieBranch} from ${onieURL}"

class BuildTarget {
      def manufacturer
      def machine
      def buildEnv
      def makeTarget

}// BuildTarget 
def aJob = job('ONIE build') {
    // any system labeled 'onie' can build.
	label 'onie'
	description 'Create ONIE builds'
	parameters {
	choiceParam('Build Targets', [ 'all','recovery-iso','demo' ], 'ONIE argument to use')
	stringParam("buildDebug", "none", "Debug build. Default = none. Options: skipDownload, skipToolBuild ")


	}//parameters
	steps {	
		shell("git clone --branch ${onieBranch} ${onieURL} ")
		}

	steps {
			shell( "pwd ; ls -l ")
			shell( "ls -l onie/machine" )
			//shell( readFileFromWorkspace("find onie/machine -maxdepth 1" ))



			shell("find onie/machine -maxdepth 1" )
			
			//def manulist =  sh( returnStdout: true, script: 'find onie/machine -maxdepth ').trim
			//println "Manufacturer list: ${manulist}"
			println( "done!" )

	}//steps
}//test job

println "---> ${curFileName} Done."
