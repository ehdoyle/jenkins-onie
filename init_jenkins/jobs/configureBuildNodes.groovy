// Basic Imports
// Stolen from https://stash.cumulusnetworks.com/projects/CIAUTO/repos/jenkins-cltest-seed/browse/system_groovy/configureBuildNodes.groovy

import hudson.model.*
import jenkins.model.*
import hudson.slaves.*
import hudson.plugins.sshslaves.*
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy
import hudson.model.Node.*

// Get seed job workspace for setting up devices
// In the container, this resolves to /var/jenkins_home/workspace/mother_seed_job
def seed_job_workspace = binding.variables.SEED_WORKSPACE

println "--> jenkins_package_seed  Running configureBuildNodes.groovy"

// Setting up our slaves in Jenkins
def slave_config_file = new File("${seed_job_workspace}/properties/jenkins_build_slaves.properties")
def slave_properties = new ConfigSlurper().parse(slave_config_file.toURI().toURL()).jenkins_slaves

println "--> jenkins_package_seed iterating through slave node instances."

slave_properties.each { slave_shortname, slave_config ->
    // Create the SSH Launcher for Jenkins to use
    temp_launcher = new SSHLauncher(slave_config.slave_fqdn,            // FQDN for the slave is needed
                                    22,                                 // Port 22 for SSH, no need to parameterize this
                                    slave_config.ssh_creds.user_creds,  // User credentials to use for SSH
                                    "",                                 // JVM Options, none needed
                                    "",                                 // Java Path, none needed
                                    "",                                 // prefixStartCommand, none needed
                                    "",                                 // postStartCommand, none needed
                                    null,                               // Launch timeout seconds, null
                                    null,                               // Maximum retries
                                    null,                               // Retry wait time
                                    new NonVerifyingKeyVerificationStrategy())
    
    // Create a slave with all requested params
    DumbSlave temp_dumb_slave = new DumbSlave(slave_shortname,
                                              slave_config.remote_fs,
                                              temp_launcher)
    temp_dumb_slave.setNumExecutors(slave_config.executors)
    temp_dumb_slave.setLabelString(slave_config.labels.join(" "))
    temp_dumb_slave.setNodeDescription(slave_config.description)

    if (temp_dumb_slave in Jenkins.instance.nodes)
    {
        println "--> jenkins_package_seed Jenkins already knows about ${temp_dumb_slave}"
    }
    else
		{
        println "--> jenkins_package_seed Jenkins is adding ${temp_dumb_slave}"		
        Jenkins.instance.addNode(temp_dumb_slave)
    }
}