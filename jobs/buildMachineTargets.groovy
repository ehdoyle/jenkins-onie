// File is jenkins-onie/jobs/buildMachineTargets.groovy

def curFileName="buildMachineTargets.groovy"

println "actual parsing code goes here."

def onieURL="https://github.com/opencomputeproject/onie.git"
def onieBranch="master"
def stageName="checkout ONIE"
println "---> ${curFileName} Checking out branch ${onieBranch} from ${onieURL}"

def aJob = job('test job') {
	label 'test job label'
	//stage( stageName ) {
	steps {
			shell( "pwd ; ls -l ")

			shell("git clone --branch ${onieBranch} ${ONIE_URL} ")
			shell( "ls -l onie/machine" )
			println( "done!" )

	}//steps
//}
}//test job

println "---> ${curFileName} Done."
