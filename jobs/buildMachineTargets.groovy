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

//
// Class to hold data to generate actual build jobs from
class BuildTarget {
    def manufacturer
    def machine
    def buildEnv
    def makeTarget
}

class BuildTargetList {
    // scope these outside runCommand to hold return output
    def cmdOut
    def cmdErr

    // list of strings
    List<String> checkoutCmds = new ArrayList<>()

    String myMsg="---> REBELLL HEY!"
    String dirPaths
    String machineList

    // Create a list of Manufacturers to create folders in Jenkins
    def ManufacturerArray = []

    // Create a list of BuildTarget objects
    def BuildTargetArray = []

    // Create list of buildable machine targets
    def BuildArray = []

    String myCmdResult
    def yell() {
        //  prints to syslog
		//	println myMsg
		//  System.out.println(myMsg)
		dirPaths = System.getenv("JENKINS_HOME")
    }//yell

	// debug to see what our options are
	void printAllMethods( obj ){
		if( !obj ){
    	    println( "Object is null\r\n" );
 			return;
        }
		if( !obj.metaClass && obj.getClass() ){
	        printAllMethods( obj.getClass() );
			return;
		}
		def str = "class ${obj.getClass().name} functions:\r\n";
		obj.metaClass.methods.name.unique().each{
			str += it+"(); ";
		}
		println "${str}\r\n";
	}

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
	println "---> Cloning to ${onieCheckoutDir}"
		runCommand "git clone https://github.com/opencomputeproject/onie.git ${onieCheckoutDir}"
		println "Got out ${cmdOut}"
		println "Got err ${cmdErr}"
	runCommand "find  ${onieCheckoutDir}/machine -maxdepth 1"
	if( cmdErr.size() > 0 ) {
	    println "---> ONIE directory structure looks bad. Deleting and trying again."
	    runCommand "rm -rf ${onieCheckoutDir}"
	    println "---> Second try checking out ONIE"
	    runCommand "git clone https://github.com/opencomputeproject/onie.git ${onieCheckoutDir}"
	    if( cmdErr.size() > 0 ) {

	    }
	}
		println "Got out ${cmdOut}"
		println "Got err ${cmdErr}"

		// put the data where it can be read by the Jenkins Job

		machineList = cmdOut

		//     printAllMethods( cmdOut )
		// Load the array with everything. This trims off the path.
		def MachineArray = machineList.split( "${onieCheckoutDir}/machine/")


		MachineArray.each {
            println "Machine array iteration $it"

			// get rid of trailing spaces as it breaks the _* below,
			// but add the string as 'it' with whitespace, otherwise the
			// array becomes a comma separated list.
			String  dirName = it.trim()

	        if ( it.contains( "kvm_x86_64") ) {
				println "Hit kvm_x86_64"
				BuildArray.add( it )

				BuildTargetArray.add( new BuildTarget( manufacturer: it.trim(), machine: it.trim(), buildEnv: "debian9", makeTarget: "  MACHINE=kvm_x86_64 " ) )
			}
			else if ( it.contains( "qemu_armv") ) {
				println "Hit qemu_armv target "
				BuildArray.add( it )
				BuildTargetArray.add( new BuildTarget( manufacturer: it.trim(), machine: it.trim(), buildEnv: "debian9", makeTarget: "  MACHINE=${dirName}" ) )
            }

			else {

				println "Getting build targets for $dirName"
				// This build has subdirectories - get them
				// Build targets are <dirname>_<machinename> so filter on <dirname>_*
				runCommand "find ${onieCheckoutDir}/machine/${dirName} -maxdepth 1 -iname ${dirName}_*"
				//				println "Got out here ${cmdOut}"
				//				println "Got err here ${cmdErr}"
				if( cmdErr.size() > 0 ) {
				    println "Command returned error ${cmdErr}. Continuing"
				}
				else {
				    // This being explicitly a String is VERY IMPORTANT
					String targetString = cmdOut
					println "PreSplit Using $targetString "

					// Since there are valid build targets, add this manufacturer to the list of
					//  manufacturers so a folder for it can be created in Jenkins.

					ManufacturerArray.add( dirName )

					def localTargets = targetString.split( "${onieCheckoutDir}/machine/" )

					if ( 1 == 1 ) {
						localTargets.each {
							println "----> local target $it "
							//					 printAllMethods( it )
         					if ( !it ) {
	         				    println "Skipping null it"
		        			}
			        		else {
				BuildArray.add( it )
				//
				BuildTargetArray.add( new BuildTarget( manufacturer: dirName.trim(), machine: it.trim(), buildEnv: "debian9", makeTarget: "MACHINEROOT=../machine/${dirName}  MACHINE=${dirName}" ) )
							}
						}//localTargets.each
						//					BuildArray.add(  localTargets )
					} // comment out

				}//else
				//http://docs.groovy-lang.org/docs/groovy-2.4.0/html/api/org/codehaus/groovy/runtime/ProcessGroovyMethods.html

			}// else

			BuildArray.each {
				println "BuildArray: Finally got $it"
			}

			println "---> Doing clone"


		}//MachineArray.each

		println "---> Dumping build target list"
		BuildTargetArray.each {
			def BuildTarget theTarget = it
			println "Manufacturer: ${theTarget.manufacturer} Machine: ${theTarget.machine} BuildEnv: ${theTarget.buildEnv} Make command: ${theTarget.makeTarget}"

		}
    }//getMachines
}// BuildTargetList

def targetList = new BuildTargetList()

//shell( "pwd ; ls -l ")
//shell("find onie/machine -maxdepth 1 > outfile.txt" )
//def fileContents =readFileFromWorkspace('outfile.txt' )
//println "File contents ${filecontents}"

def outputFile = "outfile.txt"

//
// get all the information about what can be built and how
targetList.getMachines()

//
// create folders so that the machine paths below that use
// buildTargetInfo.machine will be valid.
//
targetList.ManufacturerArray.each {
	println "---> Creating manufacturer folder ${it}"
	// Without the / in front, folders get created in the Seed Jobs folder.
	folder( "/${it}" ) {
		description "${it} build targets"
	}

}

targetList.BuildTargetArray.each {
	//		println "Will make ${it.machine} with ${it.buildEnv}"
	// save this so it doesn't get replaced
	def buildTargetInfo = it

	println "Naming job ${buildTargetInfo.machine}"
	// use leading / to put this job in the top level Manufacturer folder
	def aJob = job( "/${buildTargetInfo.machine}" ) {
		// any system labeled 'onie' can build.
		label 'onie'
		description "Build ONIE for ${buildTargetInfo.manufacturer} ${buildTargetInfo.machine}"
		parameters {
			choiceParam('Build Targets', [ 'all','recovery-iso','demo' ], 'ONIE argument to use')
			stringParam("buildDebug", "none", "Debug build. Default = none. Options: skipDownload, skipToolBuild ")


		}//parameters
		//			def filecontents = readfile( outputFile )

		steps {
			shell( "if [ !  -d onie ]; then git clone --branch ${onieBranch} ${onieURL} ; fi" )

		}

	steps {
//	    def buildCommand = "
				//								BuildTargetArray.add( new BuildTarget( manufacturer: dirName.trim(), machine: it.trim(), buildEnv: "debian9", makeTarget: "make -j4 MACHINEROOT=../machine/${dirName} MACHINE=${dirName} all demo " ) )
	    shell "due --run-image due-onie-build --command export PATH=\"/sbin:/usr/sbin:\$PATH\" \\; make -C /build MACHINEROOT=../machine/${buildTargetInfo.machine}  MACHINE=${buildTargetInfo.machine} all demo "	    
//			shell "make -C onie/build-config -j4 ${buildTargetInfo.makeTarget}"
			//	println "Machine result ${targetList.myCmdResult}"
			//		println "Machine list ${targetList.machineList}"
			//println "Machine list ${targetList.BuildArray}"
			//		println( "done!" )

		}//steps
	}//test job

}// targetList.BuildTargetArray.each
println "---> ${curFileName} Done."
