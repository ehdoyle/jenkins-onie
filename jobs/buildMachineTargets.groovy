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

      def getMachines() {
      println "---> Doing clone"
      "git clone https://github.com/opencomputeproject/onie.git".execute().text
//      myCmdResult = "git clone -b master https://github.com/opencomputeproject/onie.git ".execute().text
	println "---> Running find."
  //    machineList = "find /var/jenkins_home/onie/machine -maxdepth 1".execute().text
      machineList = "find /onie/machine -maxdepth 1".execute().text


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
