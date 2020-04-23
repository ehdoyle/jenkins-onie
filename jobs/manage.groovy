// File is jenkins-onie/jobs/manage.groovy
// This is jobdsl context

// NOTE: all println output from the classes will end up in
//       /var/log/jenkins/jenkins.log
//
//       println issued as part of DSL context will show up
//       in the Jenkins console output


def curFileName="manage.groovy"



def onieURL="https://github.com/opencomputeproject/onie.git"
def onieBranch="master"
def stageName="checkout ONIE"
println "---> ${curFileName} Checking out branch ${onieBranch} from ${onieURL}"

def aJob = freeStyleJob('JobManagement'){

    label("Management" )

    steps {
	println "Starting management"
    }
    
//    jenkins.model.Jenkins.instance.getAllItems(jenkins.model.ParameterizedJobMixIn.ParameterizedJob.class).each {	
//	if(it.isDisabled()){
//	    println it.fullName;
//	}
		

}// node master
