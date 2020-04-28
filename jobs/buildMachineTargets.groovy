// File is jenkins-onie/jobs/buildMachineTargets.groovy
// This is jobdsl context

// NOTE: all println output from the classes will end up in
//       /var/log/jenkins/jenkins.log
//
//       println issued as part of DSL context will show up
//       in the Jenkins console output

import groovy.json.JsonSlurper

def curFileName="buildMachineTargets.groovy"

println "actual parsing code goes here."

def onieMasterURL="https://github.com/opencomputeproject/onie.git"
def onieAlexURL="https://github.com/ehdoyle/onie.git"
def onieURL= onieAlexURL
def onieBranch="master"
def stageName="checkout ONIE"
println "---> ${curFileName} Checking out branch ${onieBranch} from ${onieURL}"


class ONIEPlatform {
    def Manufacturer
    // Machine model name    
    def Name
    // Build environment to use for it
    def BuildEnv
    // build commands to make it
    def MakeTarget 
    // Suggested number of parallel build jobs
    def Jobs = 4
    // release it built for
    def Release
    // CPU Architecture
    def Architecture
    // Notes
    def Notes

    def printContents () {
	println "--> Platform object."
	println "  Manufacturer:  ${Manufacturer}"
	println "  Name:          ${Name}"
	println "  Build env:     ${BuildEnv}"
	println "  Release:       ${Release}"
	println "  Architecture:  ${Architecture}"
	println "  Notes:         ${Notes}"
	println "  Make target:   ${MakeTarget}"
	println "  Jobs:          ${Jobs}"
    }
}

class BuildTargetList {
    // scope these outside runCommand to hold return output
    def cmdOut
    def cmdErr
    // top of file definition is out of scope for this...
    def onieMasterURL="https://github.com/opencomputeproject/onie.git"
    def onieAlexURL="https://github.com/ehdoyle/onie.git"
    def onieURL= onieAlexURL
    

    // list of strings
//    List<String> checkoutCmds = new ArrayList<>()

//    String machineList

    // Create a list of Manufacturers to create folders in Jenkins
//    def ManufacturerArray = []

    // Create a list of BuildTarget objects
//    def BuildTargetArray = []

    // Create list of buildable machine targets
//    def BuildArray = []


    def ONIEPlatformArray = []
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
	        runCommand "rm -rf ${onieCheckoutDir}"

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
                exit 1
            }else {
		println "----> double error fallthrough"
            }
        }
	println "---> Gonna do file"
	def ONIETargetsFile = new File( "${onieCheckoutDir}/build-config/scripts/onie-build-targets.json")
	println "---> Reading file ${ONIETargetsFile}"
	Map ONIETargetsProperties = new JsonSlurper().parseText( ONIETargetsFile.getText("UTF-8") )		

	println "---> mad props!"
	ONIETargetsProperties.Platform.each() {
	    String platName = it.Name.trim()

	    // Defaults that will be true for most platforms.
	    String makeCommand = "MACHINEROOT=../machine/${it.Manufacturer.trim()}  MACHINE=${it.Name.trim()}"
	    String buildEnv = "due-onie-build-debian-9"
	    // Virtual systems don't have a manufacturer dir
	    switch ( platName ) {
		case "kvm_x86_64":
		case "qemu_armv7a":
		case "qemu_armv8a":
		    makeCommand = " MACHINE=${platName} "
		    break
		}// switch

	    // Support multiple build environments
	    switch ( it.BuildEnv.trim() ) {
		case "Debian8":
		    buildEnv = "due-onie-build-debian-8"
		    break
	    }//switch

	    def platFields = [
		Manufacturer : it.Manufacturer.trim(),
		Name  : platName,
		BuildEnv : buildEnv,
		Release : it.Release.trim(),
		Architecture : it.Architecture.trim(),
		Notes : it.Notes.trim(),
		Jobs  : 4,
		MakeTarget : makeCommand 
	    ]

            ONIEPlatformArray.add( new ONIEPlatform( platFields ) )
	    println "Test debug Platform Name: ${platName} DONE"
	    println "--->>"
	    
	}

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
    String lastFolder
    targetList.ONIEPlatformArray.each {
	// Every entry has a manufacturer string, so only make the
	// folder once per unique entry
	String newFolder = it.Manufacturer.trim()	
	if ( lastFolder != newFolder ) {
	    lastFolder = newFolder
            println "---> Creating manufacturer folder ${newFolder}"
            // Without the / in front, folders get created in the Seed Jobs folder.
            folder( "/${newFolder}" ) {
            description "${newFolder} build targets"
            } // folder
	}//lastfolder 
    } //each
} catch ( Exception e) {
    println "---> ERROR CREATING MANUFACTURER FOLDERS."
    println "=====> ${e}"
    println "---> Exiting..."
    exit 1
}


try {

//    println "--> dumping JSON parse."
//    targetList.ONIEPlatformArray.each {
//	it.printContents()
//    }
    targetList.ONIEPlatformArray.each {
        //          println "Will make ${it.machine} with ${it.buildEnv}"
        // save this so it doesn't get replaced
        def buildTargetInfo = it

		// default to not using the host's local package cache.
		def DueMountSystemPackageCacheDir = "" 
		def BuildLocalCache = ""

		if ( 1 == 1 ) {
		// mount the host local cache of downloadable packages.			
			 DueMountSystemPackageCacheDir =  " --mount-dir /var/cache/onie/download:/var/cache/onie/download "
			 BuildLocalCache = ' export ENV_USE_ONIE_SYSTEM_CACHE=\"TRUE\" '
		}
        println "Naming job ${buildTargetInfo.Name}"
        // use leading / to put this job in the top level Manufacturer folder
        def aJob = freeStyleJob( "/${buildTargetInfo.Manufacturer}/${buildTargetInfo.Name}" ) {
            // any system labeled 'onie' can build.
            label 'onie'
            description "Build ONIE for ${buildTargetInfo.Manufacturer} ${buildTargetInfo.Name} Arch: ${buildTargetInfo.Architecture} Release: ${buildTargetInfo. Release} Notes: ${buildTargetInfo.Notes}"
            parameters {
		stringParam('BUILD_TARGETS', 'all demo', 'Default = all demo . Options: clean, distclean, recovery-iso, etc' )
		
            }//parameters

	    // check for source changes daily
	    triggers {
		scm('@daily')
	    }
	    
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
			// doc found at https://jenkinsci.github.io/job-dsl-plugin/#path/job-scm-git-extensions-cloneOptions
			cloneOptions {
			    depth( 1 )
			    shallow( true )
			    timeout( 120)
			}
		    }
		    
		}
	    }

	    // send email when anything exciting happens
	    publishers {
		extendedEmail {
		    recipientList('adoyle@cumulusnetworks.com')
	    defaultSubject("ONIE ${buildTargetInfo.Name}")
		    defaultContent('something broke?')
		    contentType('text/html')
		    triggers {
			stillUnstable {
			    subject("ONIE busted ${buildTargetInfo.Name} Triggered: \$BUILD_CAUSE ")
			    content('$BUILD_URL')
			    sendTo {
				developers()
			    }//sendTo
			}// stillUnstable
		    }//triggers
		}//extendedEmail
	    }//publishers
	    
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
                    shell (" due --run-image ${buildTargetInfo.BuildEnv} ${DueMountSystemPackageCacheDir}  --command export PATH=\"/sbin:/usr/sbin:\$PATH\" ${BuildLocalCache} \\; make -j ${buildTargetInfo.Jobs}   -C /work/onie/jenkins-node-builds/workspace/${buildTargetInfo.Name}/onie/build-config ${buildTargetInfo.MakeTarget} \$BUILD_TARGETS " )
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




