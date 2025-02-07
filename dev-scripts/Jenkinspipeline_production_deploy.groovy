@Library('whatfix-builds@master')_

pipeline {
    agent any

    environment {
        MAMAOPS_JENKINS_URL = 'https://mamaops.quickolabs.com'
        SLACK_TOKEN_CREDENTIAL_ID = 'Slack-Notifications'
        SLACK_CHANNEL = 'release-test-alerts'
        TRIGGER_JOB_NAME = 'PRODUCTION-all-trigger-job-test'
    }

    parameters {
        // Uncomment or define parameters as needed
        // string(name: 'promote_from', defaultValue: '', description: 'Promotion source branch')
        // string(name: 'Branch', defaultValue: 'main', description: 'Branch to be used for sanity')
        // choice(name: 'environment', choices: ['production', 'staging'], description: 'Select the environment for deployment')
    }

    stages {
        stage('Promotion Nginx to Production') {
            when {
                expression {
                    params.environment == 'production'
                }
            }
            steps {
                script {
                    slackSend(
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Nginx promotion is starting to Production"
                    )

                    def usDeployResponse = build job: env.TRIGGER_JOB_NAME,
                        parameters: [
                            string(name: 'promote_from', value: params.promote_from),
                            booleanParam(name: 'promote_api', value: false),
                            booleanParam(name: 'promote_ui', value: false),
                            booleanParam(name: 'promote_nginx_e-server', value: false),
                            booleanParam(name: 'promote_editor', value: false),
                            booleanParam(name: 'blue_green', value: false),
                            booleanParam(name: 'promote_nginx_production', value: true),
                            booleanParam(name: 'deploy_us_eserver', value: false),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false),
                            string(name: 'sanityLibraryBranch', value: params.Branch)
                        ], propagate: true

                    if (usDeployResponse.result != 'SUCCESS') {
                        def jobLink = "${env.BUILD_URL}/job/${usDeployResponse.id}" // Constructing the job link

                        slackSend(
                            tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                            channel: SLACK_CHANNEL,
                            color: 'danger',
                            message: "Production Nginx promotion failed. Check the job details here: ${jobLink}"
                        )
                        error "Production Nginx promotion failed."
                    }

                    slackSend(
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Nginx promotion has been completed to Production"
                    )
                }
            }
        }

        stage('EU Deploy') {
            when {
                expression {
                    params.environment == 'production'
                }
            }
            steps {
                script {
                    slackSend(
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Production deployment has started on EU"
                    )

                    def euDeployResponse = build job: env.TRIGGER_JOB_NAME,
                        parameters: [
                            string(name: 'promote_from', value: params.promote_from),
                            booleanParam(name: 'promote_api', value: false),
                            booleanParam(name: 'promote_ui', value: false),
                            booleanParam(name: 'promote_nginx_e-server', value: false),
                            booleanParam(name: 'promote_editor', value: false),
                            booleanParam(name: 'blue_green', value: false),
                            booleanParam(name: 'promote_nginx_production', value: false),
                            booleanParam(name: 'deploy_us_eserver', value: false),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: true),
                            string(name: 'sanityLibraryBranch', value: params.Branch)
                        ], propagate: true

                    if (euDeployResponse.result != 'SUCCESS') {
                        def jobLink = "${env.BUILD_URL}/job/${euDeployResponse.id}" // Constructing the job link

                        slackSend(
                            tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                            channel: SLACK_CHANNEL,
                            color: 'danger',
                            message: "Deployment failed on EU. Check the job details here: ${jobLink}"
                        )
                        error "Deployment failed on EU. Check the job details here: ${jobLink}"
                    }

                    slackSend(
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Production deployment has been completed on EU"
                    )
                }
            }
        }

        stage('US Deploy') {
            when {
                expression {
                    params.environment == 'production'
                }
            }
            steps {
                script {
                    slackSend(
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Production deployment has started on US"
                    )

                    def usDeployResponse = build job: env.TRIGGER_JOB_NAME,
                        parameters: [
                            string(name: 'promote_from', value: params.promote_from),
                            booleanParam(name: 'promote_api', value: false),
                            booleanParam(name: 'promote_ui', value: false),
                            booleanParam(name: 'promote_nginx_e-server', value: false),
                            booleanParam(name: 'promote_editor', value: false),
                            booleanParam(name: 'blue_green', value: false),
                            booleanParam(name: 'promote_nginx_production', value: false),
                            booleanParam(name: 'deploy_us_eserver', value: false),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: true),
                            booleanParam(name: 'deploy_eu_production', value: false),
                            string(name: 'sanityLibraryBranch', value: params.Branch)
                        ], propagate: true

                    if (usDeployResponse.result != 'SUCCESS') {
                        def jobLink = "${env.BUILD_URL}/job/${usDeployResponse.id}" // Constructing the job link

                        slackSend(
                            tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                            channel: SLACK_CHANNEL,
                            color: 'danger',
                            message: "Deployment failed on US. Check the job details here: ${jobLink}"
                        )
                        error "Deployment failed on US. Check the job details here: ${jobLink}"
                    }

                    slackSend(
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID,
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Production deployment has been completed on US"
                    )
                }
            }
        }
    }
}
