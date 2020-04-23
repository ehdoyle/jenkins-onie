// File is jenkins-onie/jobs/buildMachineTargets.groovy
// This is jobdsl context

// NOTE: all println output from the classes will end up in
//       /var/log/jenkins/jenkins.log
//
//       println issued as part of DSL context will show up
//       in the Jenkins console output


def curFileName="buildMachineTargets.groovy"

println "actual parsing code goes here."

def onieURL="https://github.com/opencomputeproject/onie.git"
def onieBranch="master"
def stageName="checkout ONIE"
println "---> ${curFileName} Checking out branch ${onieBranch} from ${onieURL}"

//
// Class to hold data to generate actual build jobs from
class BuildTarget {
    // commpany making the switch (not set for virtual machines)
    def manufacturer
    // Machine model name
    def machine
    // Build environment to use for it
    def buildEnv
    // Build commands to make it
    def makeTarget
    // Suggested number of parallel build jobs
    def jobs
}

class BuildTargetList {
    // scope these outside runCommand to hold return output
    def cmdOut
    def cmdErr
    // top of file definition is out of scope for this...
    def onieURL="https://github.com/opencomputeproject/onie.git"
    // list of strings
    List<String> checkoutCmds = new ArrayList<>()

    String machineList

    // Create a list of Manufacturers to create folders in Jenkins
    def ManufacturerArray = []

    // Create a list of BuildTarget objects
    def BuildTargetArray = []

    // Create list of buildable machine targets
    def BuildArray = []


    // debug to see what our options are with any given object
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

    // Does:     runs a shell command passed in
    // Modifies: sets cmdOut and cmdErr to have stdout/stderr contents
    // Notes:
    //    Do not stack multiple commands.
    //    Things like ; and && cause cryptic java errors
    //    Also, println statements go into the local jenkins.log
    String runCommand( String theCmd ) {
        println "---> Executing:  ${theCmd}"
        cmdOut = new StringBuffer()
        cmdErr = new StringBuffer()
        Process cmdProc = theCmd.execute()
        cmdProc.waitForProcessOutput( cmdOut, cmdErr )
        cmdProc.waitFor()
        //        println "Got out ${cmdOut}"
        //        println "Got err ${cmdErr}"
        // Use size of string returned to check for errors
        //        if( cmdOut.size() > 0 ) println "Big enough to print" + cmdOut
        //        if( cmdErr.size() > 0 ) println "Big enough to print " + cmdErr

    }

    // Does:
    // Checks out onie and parses the machines directory to get build targets
    // Modifies:
    //    BuildTargetArray should have a bunch of build target objects
    //    ManufacturerArray will have the manufacturers, to create directories.
    def getMachines() {
        // add commands to list
        def onieCheckoutDir = "/var/jenkins_home/workspace/SeedJobs/Seed_ONIE/oniecheckout"

        println "---> Commented out delete of ONIE to save debug time."
        //      runCommand "rm -rf ${onieCheckoutDir}"

        println "---> Cloning to ${onieCheckoutDir}"
        runCommand "git clone ${onieURL} ${onieCheckoutDir}"
        println "Got out ${cmdOut}"
        println "Got err ${cmdErr}"
        runCommand "find  ${onieCheckoutDir}/machine -maxdepth 1"
        if( cmdErr.size() > 0 ) {
            println "---> ONIE directory structure looks bad. Deleting and trying again."
            runCommand "rm -rf ${onieCheckoutDir}"
            println "---> Second try checking out ONIE"
            runCommand "git clone ${onieURL}  ${onieCheckoutDir}"
            if( cmdErr.size() > 0 ) {
                println "ERROR! Failed to clone ${onieURL}"
                return -1
            }else {
                runCommand "find  ${onieCheckoutDir}/machine -maxdepth 1"
            }

        }
        println "Got out ${cmdOut}"
        println "Got err ${cmdErr}"

        // put the data where it can be read by the Jenkins Job
        machineList = cmdOut

        // Load the array with everything. This trims off the leading path.
        def MachineArray = machineList.split( "${onieCheckoutDir}/machine/")

        // for every entry in MachineArray
        MachineArray.each {
            // it is a language supplied iterator that refreences the current object in MachineArray
            println "Machine array iteration $it"

            // Name of DUE container to build with
            def DUEDebianNine="due-onie-build-debian-9"
            def DUEDebianEight="due-onie-build-debian-8"

            // get rid of trailing spaces as it breaks the _* below,
            // but add the string as 'it' with whitespace, otherwise the
            // array becomes a comma separated list.
            String  dirName = it.trim()

            if ( it.contains( "kvm_x86_64") ) {
                println "Hit kvm_x86_64"
                BuildArray.add( it )

                BuildTargetArray.add( new BuildTarget( manufacturer: it.trim(),\
                                                      machine: it.trim(), \
                                                      buildEnv: DUEDebianNine, \
                                                      makeTarget: "  MACHINE=${dirName} ",\
                                                      jobs: 4 ) )
            }
            else if ( it.contains( "qemu_armv") ) {
                println "Hit qemu_armv target "
                BuildArray.add( it )
                BuildTargetArray.add( new BuildTarget( manufacturer: it.trim(), \
                                                      machine: it.trim(), \
                                                      buildEnv: DUEDebianNine, \
                                                      makeTarget: "  MACHINE=${dirName}", \
                                                      jobs: 4 ) )
            }

            else {
                println "Getting build targets for $dirName"
                // This build has subdirectories - get them
                // Build targets are <dirname>_<machinename> so filter on <dirname>_*
                runCommand "find ${onieCheckoutDir}/machine/${dirName} -maxdepth 1 -iname ${dirName}_*"
                //                              println "Got out here ${cmdOut}"
                //                              println "Got err here ${cmdErr}"
                if( cmdErr.size() > 0 ) {
                    println "Command returned error ${cmdErr}. Continuing"
                }
                else {
                    // This being explicitly a String is VERY IMPORTANT
                    String targetString = cmdOut

                    // Since there are valid build targets, add this manufacturer to the list of
                    //  manufacturers so a folder for it can be created in Jenkins.
                    ManufacturerArray.add( dirName )

                    def localTargets = targetString.split( "${onieCheckoutDir}/machine/" )

                    localTargets.each {
                        println "----> local target $it "
                        //                                   printAllMethods( it )
                        if ( !it ) {
                            println "Skipping null it"
                        }
                        else {
                            // list of things that can be built, for reference
                            BuildArray.add( it )

                            // target to build
                            String holder = "${it}"
                            println "got holder as ${holder}"
                            String machineName = holder.minus( "${dirName}/" )
                            println "got machine name as ${machineName}"
                            BuildTargetArray.add( new BuildTarget( manufacturer: dirName.trim(),\
                                                                  machine: it.trim(),\
                                                                  buildEnv: DUEDebianNine, \
                                                                  makeTarget: "MACHINEROOT=../machine/${dirName}  MACHINE=${machineName.trim()}", \
                                                                  jobs: 4 ) )
                        }
                    }//localTargets.each
                }//else find of subdirectory worked
            }// else not emulation special case and is regular switch

        }//MachineArray.each

        // keep this around for reference.
        if( 1 == 0 ) {
            println "---> Dumping build target list"
            BuildTargetArray.each {
                def BuildTarget theTarget = it
                println "Manufacturer: ${theTarget.manufacturer} Machine: ${theTarget.machine} BuildEnv: ${theTarget.buildEnv} Make command: ${theTarget.makeTarget}"
            }// each
        }// optional debug printout to jenkins.log
    }//getMachines
}// BuildTargetList

//
// Main - run java object code that dumps its printouts to /var/log/jenkins/jenkins.log,
//        then use returned data in Job DSL context to create Jenkins jobs.
//

def targetList = new BuildTargetList()

//
// get all the information about what can be built and how
try {
    targetList.getMachines()
}catch( Exception e){
    println "---> ERROR WITH TARGET LIST."
    println "=====> ${e}"
    println "---> Exiting..."
    // up and die
    exit 1
}


//
// create folders so that the machine paths below that use
// buildTargetInfo.machine will be valid.
//
try {
    targetList.ManufacturerArray.each {
        println "---> Creating manufacturer folder ${it}"
        // Without the / in front, folders get created in the Seed Jobs folder.
        folder( "/${it}" ) {
            description "${it} build targets"
        } // folder

    } //each
} catch ( Exception e) {
    println "---> ERROR CREATING MANUFACTURER FOLDERS."
    println "=====> ${e}"
    println "---> Exiting..."
    exit 1
}


try {
    targetList.BuildTargetArray.each {
        //          println "Will make ${it.machine} with ${it.buildEnv}"
        // save this so it doesn't get replaced
        def buildTargetInfo = it

        println "Naming job ${buildTargetInfo.machine}"
        // use leading / to put this job in the top level Manufacturer folder
        def aJob = freeStyleJob( "/${buildTargetInfo.machine}" ) {
            // any system labeled 'onie' can build.
            label 'onie'
            description "Build ONIE for ${buildTargetInfo.manufacturer} ${buildTargetInfo.machine}"
            parameters {
		//stringParam('buildDebug', "none", "Debug build. Default = none. Options: skipDownload, skipToolBuild ")
		//Takes:  var name, default, description
	       stringParam('BUILD_TARGETS', 'all demo', 'Default = all demo . Options: clean, distclean, recovery-iso, etc' )
		
            }//parameters
	    // use JobDSL to create a Git panel in the job
	    scm {
		git {
		    remote {
			name('ONIE upstream')
			url( onieURL )
			branch('master')
		    }
		    extensions {
			//cleanAfterCheckout()
			// check out as /*.../<manufacturer>/<platform>/onie 
			relativeTargetDirectory('onie')
		    }
		}
	    }

            steps {

                //      def buildCommand = "
                //                                                          BuildTargetArray.add( new BuildTarget( manufacturer: dirName.trim(), machine: it.trim(), buildEnv: "debian9", makeTarget: "make -j4 MACHINEROOT=../machine/${dirName} MACHINE=${dirName} all demo " ) )


                // Invoke a due build that will mount the workspace and reference the oniebuild user's home directory for config
                // Jenkins Example: accessing binding variables
                def seed_job_workspace = "${binding.variables.WORKSPACE}"
                //println "HEY - workspace is ${seed_job_workspace}"
                //println "Your VARS"
                //println "${binding.variables}"]
                //println "Workspace is ${WORKSPACE}"
                try {
                    // TODO: The  /work/onie/jenkins-node-builds/workspace/ should be a variable
		    // NOTE: BUILD_TARGETS is set as a parameter above. If you don't \ the $, you'll get errors
		    //       about Jenkins not recognizing it, and waste an hour plus figuring it out.
                    shell (" due --run-image ${buildTargetInfo.buildEnv}  --command export PATH=\"/sbin:/usr/sbin:\$PATH\" \\; make -j ${buildTargetInfo.jobs}   -C /work/onie/jenkins-node-builds/workspace/${buildTargetInfo.machine}/onie/build-config ${buildTargetInfo.makeTarget} \$BUILD_TARGETS " )
                }catch( Exception e ) {
                    println "---> ERROR BUILDING ONIE"
                    println "=====> ${e}"
                    println "---> Exiting..."
                    exit 1
                }
                //                  shell "make -C onie/build-config -j4 ${buildTargetInfo.makeTarget}"
                //  println "Machine result ${targetList.myCmdResult}"
                //          println "Machine list ${targetList.machineList}"
                //println "Machine list ${targetList.BuildArray}"
                //          println( "done!" )

            }//steps
        }//test job

    }// targetList.BuildTargetArray.each
} catch ( Exception e) {
    println "---> ERROR CREATING ONIE BUILD JOBS."
    println "=====> ${e}"
    println "---> Exiting..."
    exit 1
}

println "---> ${curFileName} Done."
