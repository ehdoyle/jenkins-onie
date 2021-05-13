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
// if git cloned into the oniebuild home directory, pull from there.
// This should save bandwidth on pulling 200MB per target when rebuilding the world.
def onieLocalURL="oniebuild@192.168.1.139:/home/oniebuild/onie"

//def onieJenkinsURL="http://onie.mvlab.cumulusnetworks.com/jenkins/jenkins-onie"
def onieJenkinsURL="oniebuild@onie.mvlab.cumulusnetworks.com:/var/www/html/jenkins/jenkins-onie"
// set to override source - see below
//def onieURL= onieMasterURL

def onieBranch="master"
def stageName="checkout ONIE"
def onieURL = onieJenkinsURL
def seriousONIEURL = onieJenkinsURL

println "---> ${curFileName} Starting local."
println "---> Check container's /var/log/jenkins.log for more debug output."
println "---> ${curFileName} Checking out local branch ${onieBranch} from ${onieURL}"


class ONIEPlatform {
    def Vendor
    // Machine model name    
    def Platform
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
	println "  Vendor:        ${Vendor}"
	println "  Platform:      ${Platform}"
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
	def onieLocalURL="oniebuild@onie.mvlab.cumulusnetworks.com:/home/oniebuild/onie"
	def onieJenkinsURL="oniebuild@onie.mvlab.cumulusnetworks.com:/var/www/html/jenkins/jenkins-onie"	
    //alternatively file:///path to onie
	
	// set to override source - see above
    //def onieURL=onieMasterURL
	//def onieURL=onieAlexURL
	//def onieURL="oniebuild@onie.mvlab.cumulusnetworks.com:/home/adoyle/PULL-REQ/onie"
	// use local onie for build
	// NOTE - 'global' values don't apply here. Have to set it from ^^^
	//def onieURL = "http://onie.mvlab.cumulusnetworks.com:/jenkins/build-onie"

	def onieURL="oniebuild@onie.mvlab.cumulusnetworks.com:/var/www/html/jenkins/build-onie"
	
    // store array of platforms parsed out of json file
    
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
    //    VendorArray will have the manufacturers, to create directories.	
    def getMachines() {

        // add commands to list
        def onieCheckoutDir = "/var/jenkins_home/workspace/SeedJobs/Seed_ONIE/oniecheckout"

		// Note: this debug ends up in the container's /var/log/jenkins.og
		//println "---> Commented out delete of ONIE to save debug time."
		println "---> Cleaning out old ${onieCheckoutDir}"
        runCommand "rm -rf ${onieCheckoutDir}"

        println "---> Cloning ${onieURL} to ${onieCheckoutDir}"

		//sshpass -p oniebuild git clone oniebuild@onie.mvlab.cumulusnetworks.com:/var/www/html/jenkins/jenkins-onie
		println "---> install sshpass"
		runCommand "apt-get install sshpass"
		
        //runCommand "git clone ${onieURL} ${onieCheckoutDir}"
		runCommand "sshpass -p oniebuild git clone ${onieURL} ${onieCheckoutDir}"
        println "Got out ${cmdOut}"
        println "Got err ${cmdErr}"
        runCommand "find  ${onieCheckoutDir}/machine -maxdepth 1"
        if( cmdErr.size() > 0 ) {
            println "---> ONIE directory structure looks bad. Deleting and trying again."
            runCommand "rm -rf ${onieCheckoutDir}"
            println "---> Second try checking out ONIE"
            //runCommand "git clone ${onieURL}  ${onieCheckoutDir}"
			runCommand "sshpass -p oniebuild git clone ${onieURL} ${onieCheckoutDir}"			
            if( cmdErr.size() > 0 ) {
                println "ERROR! Failed to clone ${onieURL}"
                exit "Failed to clone ${onieURL}"
            }else {
		println "----> double error fallthrough"
            }
        }
	println "---> Gonna do file"
	def ONIETargetsFile = new File( "${onieCheckoutDir}/build-config/scripts/onie-build-targets.json")
	println "---> Reading file ${ONIETargetsFile}"
	def ONIETargetsProperties = new JsonSlurper().parseText( ONIETargetsFile.getText("UTF-8") )		

	printAllMethods( ONIETargetsProperties )
	
	println "---> mad props!"
	ONIETargetsProperties.each() {
	    println "---> print each"
	    String platName = it.Platform.trim()

	    // Defaults that will be true for most platforms.
	    String makeCommand = "MACHINEROOT=../machine/${it.Vendor.trim()}  MACHINE=${it.Platform.trim()}"
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

	    // set up parameters to initialize object
	    def platFields = [
		Vendor : it.Vendor.trim(),
		Platform  : platName,
		BuildEnv : buildEnv,
		Release : it.Release.trim(),
		Architecture : it.Architecture.trim(),
		Notes : it.Notes.trim(),
		Jobs  : 4,
		MakeTarget : makeCommand 
	    ]

	    println "----> made it to for each ${platName} ${buildEnv}"
	    
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
    exit "Get machine list failed."
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
	String newFolder = it.Vendor.trim()	
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

    targetList.ONIEPlatformArray.each {
        //          println "Will make ${it.machine} with ${it.buildEnv}"
        // save this so it doesn't get replaced
        def buildTargetInfo = it

		// default to not using the host's local package cache.
		def DueMountSystemPackageCacheDir = "" 
		def BuildLocalCache = ""

	// Default to  mount the host local cache of downloadable packages.'
	// comment these lines out if you don't want that
	DueMountSystemPackageCacheDir =  " --mount-dir /var/cache/onie/download:/var/cache/onie/download "
	BuildLocalCache = '  ONIE_USE_SYSTEM_DOWNLOAD_CACHE=\"TRUE\" '
	
        println "Naming job ${buildTargetInfo.Platform}"
        // use leading / to put this job in the top level Vendor folder
        def aJob = freeStyleJob( "/${buildTargetInfo.Vendor}/${buildTargetInfo.Platform}" ) {
            // any system labeled 'onie' can build.
            label 'onie'
            description "Build ONIE for Vendor: ${buildTargetInfo.Vendor} Platform: ${buildTargetInfo.Platform} Arch: ${buildTargetInfo.Architecture} Release: ${buildTargetInfo. Release} Notes: ${buildTargetInfo.Notes}"
            parameters {
		stringParam('BUILD_TARGETS', 'all demo', 'Default = all demo . Options: clean, distclean, recovery-iso, etc' )
		
            }//parameters

	    // check for source changes daily
	    triggers {
		scm('@daily')
	    }

	    // Set checkbox to delete workspace before starting new build
	    // 'Delete workspace before build starts'
	    wrappers {
		preBuildCleanup()
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
	    }//scm

	    // send email when anything exciting happens
	    publishers {
		extendedEmail {
		    recipientList('adoyle@cumulusnetworks.com')
	    defaultSubject("ONIE ${buildTargetInfo.Platform}")
		    defaultContent('something broke?')
		    contentType('text/html')
		    triggers {
			stillUnstable {
			    subject("ONIE busted ${buildTargetInfo.Platform} Triggered: \$BUILD_CAUSE ")
			    content('$BUILD_URL')
			    sendTo {
				developers()
			    }//sendTo
			}// stillUnstable
		    }//triggers
		}//extendedEmail
	    }//publishers
	    
            steps {
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
                    shell (" due --run-image ${buildTargetInfo.BuildEnv} ${DueMountSystemPackageCacheDir}  --command export PATH=\"/sbin:/usr/sbin:\$PATH\"  \\; make -j${buildTargetInfo.Jobs}  ${BuildLocalCache} -C /work/onie/jenkins-node-builds/workspace/${buildTargetInfo.Vendor}/${buildTargetInfo.Platform}/onie/build-config ${buildTargetInfo.MakeTarget} \$BUILD_TARGETS " )
                }catch( Exception e ) {
                    println "---> ERROR BUILDING ONIE"
                    println "=====> ${e}"
                    println "---> Exiting..."
                    exit 1
                }

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




