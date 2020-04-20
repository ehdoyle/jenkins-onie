println "actual parsing code goes here."

def onieURL="https://github.com/opencomputeproject/onie.git"
def onieBranch="master"

println "Checking out branch ${onieBranch} from ${onieURL}"

def aJob = job('test job') {
	label 'test job label'
	steps {
		shell( "pwd ; ls -l ")
		println( "done!" )

	}//steps


}//test job
