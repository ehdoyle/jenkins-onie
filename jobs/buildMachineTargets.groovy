// File is jenkins-onie/jobs/buildMachineTargets.groovy

def curFileName="buildMachineTargets.groovy"

println "actual parsing code goes here."

def onieURL="https://github.com/opencomputeproject/onie.git"
def onieBranch="master"

println "---> ${curFileName} Checking out branch ${onieBranch} from ${onieURL}"

def aJob = job('test job') {
	label 'test job label'
	stage( 'Running Test Job' ) {
	
	steps {
		shell( "pwd ; ls -l ")
		println( "done!" )

	}//steps
	}//stage

}//test job

println "---> ${curFileName} Done."
