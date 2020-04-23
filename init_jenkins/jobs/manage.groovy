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
println "---> ${curFileName} Script setting up"

job('management')
{
    println "---> management job set up"
    label("mgmt")

    step("sayhi")
    {
	println "HI Managment job exists."

    }

}
