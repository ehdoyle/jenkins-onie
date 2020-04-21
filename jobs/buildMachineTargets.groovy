// File is jenkins-onie/jobs/buildMachineTargets.groovy
// This is jobdsl context
//import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

//import java.io

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

      //ProcessGroovyMethods  runCommands 
      Process  processOut

      // list of strings
      List<String> checkoutCmds = new ArrayList<>()
      
      def environmentVars = [] // empty vars
      String myMsg="---> REBELLL HEY!"
      String myCWD=""
      String myCmd="find onie/machine -maxdepth 1 " 
      String dirPaths
      String machineList


      String myCmdResult
      def yell() {
          //  prints to syslog
	  //	println myMsg
	  //  System.out.println(myMsg)
	  dirPaths = System.getenv("JENKINS_HOME")
      }//yell
      
      def listFiles() {
      // Groovy can execute strings as shell commands
       myCWD = "pwd".execute().text
       myCmdResult= myCmd.execute().text
       println myCmdResult

      }

      // scope these outside runCommand to hold return output
      def cmdOut
      def cmdErr

      //
      // Do not stack multiple commands.
      // Things like ; and && cause cryptic java errors
      // Also, println statements go into the local jenkins.log
      String runCommand( String theCmd ) {
	  println "---> Executing:  ${theCmd}"
          cmdOut = new StringBuffer()
          cmdErr = new StringBuffer()
	  Process cmdProc = theCmd.execute()
	  cmdProc.waitForProcessOutput( cmdOut, cmdErr )
 	  cmdProc.waitFor()
//	  println "Got out ${cmdOut}"
//	  println "Got err ${cmdErr}"
//	  if( cmdOut.size() > 0 ) println "Big enough to print" + cmdOut
//	  if( cmdErr.size() > 0 ) println "Big enough to print " + cmdErr      	     

      }
      
      def getMachines() {


      // add commands to list
      def onieCheckoutDir = "/var/jenkins_home/workspace/SeedJobs/Seed_ONIE/oniecheckout" 
//      checkoutCmds.add( "git clone https://github.com/opencomputeproject/onie.git ${onieCheckoutDir}" )
//      checkoutCmds.add ( "find ${onieCheckoutDir}/machine -maxdepth 1" )

	// don't particularly care if this works or not
	//	runCommand "rm -rf ${onieCheckoutDir}"
	println "Commented out delete of ONIE to save debug time."
	runCommand "git clone https://github.com/opencomputeproject/onie.git ${onieCheckoutDir}"
	  println "Got out ${cmdOut}"
	  println "Got err ${cmdErr}"
	  runCommand "find  ${onieCheckoutDir}/machine -maxdepth 1" 
	  println "Got out ${cmdOut}"
	  println "Got err ${cmdErr}"

      //http://docs.groovy-lang.org/docs/groovy-2.4.0/html/api/org/codehaus/groovy/runtime/ProcessGroovyMethods.html

	def testCmd = "git clone https://github.com/opencomputeproject/onie.git"
	def checkoutPath = "/var/jenkins_home/workspace/SeedJobs/Seed_ONIE/"

	//http://docs.groovy-lang.org/next/html/documentation/working-with-collections.html
//      checkoutCmds.each {
//      			// 'it' represents the current element
//          println "ITERATE:  Use command $it"
//	  def leCommand = "$it"
//      }

      println "---> Doing clone"


      }//getMachines
}// BuildTarget

def foo = new BuildTarget()




//shell( "pwd ; ls -l ")
//shell("find onie/machine -maxdepth 1 > outfile.txt" )
//def fileContents =readFileFromWorkspace('outfile.txt' )
//println "File contents ${filecontents}"

def outputFile = "outfile.txt"

def aJob = job('ONIE build') {
    // any system labeled 'onie' can build.
	label 'onie'
	description 'Create ONIE builds'
	parameters {
	choiceParam('Build Targets', [ 'all','recovery-iso','demo' ], 'ONIE argument to use')
	stringParam("buildDebug", "none", "Debug build. Default = none. Options: skipDownload, skipToolBuild ")


	}//parameters
//			def filecontents = readfile( outputFile )	
	
	steps {
		shell( "if [ !  -d onie ]; then git clone --branch ${onieBranch} ${onieURL} ; fi" )

		}

	steps {
	 println "Java pwd is ${foo.myCWD}"
	 foo.getMachines()
	 println "Machine result ${foo.myCmdResult}"	 
	 println "Machine list ${foo.machineList}"
	 
//			shell( "ls -l onie/machine > outfile.txt" )
//			def filecontents = readfile( outputFile )	
//			shell( "pwd ; ls -l ")

//			shell("find onie/machine -maxdepth 1 > outfile.txt" )

//			println "File contents ${filecontents}"
//			def fileContents =readFileFromWorkspace('outfile.txt' )
//			echo "${fileContents}"
//			output = ${ shell("find onie/machine -maxdepth 1" ) }
			
			//def manulist =  sh( returnStdout: true, script: 'find onie/machine -maxdepth ').trim
			//println "Manufacturer list: ${manulist}"
			println( "done!" )

	}//steps
}//test job

println "---> ${curFileName} Done."
