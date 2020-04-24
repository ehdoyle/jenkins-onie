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
println "---> ${curFileName} Running"


class allJobs {

    def JobArray = []


    void GetJobs() {
	jenkins.model.Jenkins.instance.getAllItems().each {
	    jobArray.add( it )
	}
    }

}
    
 
//jenkins.model.Jenkins.instance.getAllItems(jenkins.model.ParameterizedJobMixIn.ParameterizedJob.class).each {
//jenkins.model.Jenkins.instance.getAllItems().each {	
//    if(it.isDisabled()){
//	println "Getallitems Disabled: ${it.fullName}"
//    }
//    println "Getallitems - Got: ${it.fullName} "
//}

println "Starting management"

//
// Define a scripted groovy command that can run as a step
// in this job. Pre creation Groovy doesn't work once the job
// is set up, so getting jobs has to be done in 'steps'

def theScript = '''
import hudson.model.*
for (job in jenkins.model.Jenkins.instance.getAllItems()) 
 {   println job.fullName }
 println holdresult
 println System.getenv()
def pa = new ParametersAction([
  new StringParameterValue("miniVersion", "Hereisyourdata" )
])

'''
// end script

job('JobManagement') {
    def myData
    println "pre steps"
    myData = System.getenv('miniVersion')
    println "Data is ${myData}"
    steps {
	// binding () passes a variable in to the script.
	systemGroovyCommand(theScript) {
	    binding( "holdresult", "42" )

    myData = System.getenv('miniVersion')
    println "Data is now ${myData}"
	    
	}

	// figure out getting a value here
    }//steps
}//aJob    


println "---> ${curFileName} Done running."
