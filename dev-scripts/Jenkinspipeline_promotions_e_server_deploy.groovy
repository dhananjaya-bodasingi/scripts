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
        //choice(name: 'environment', choices: ['e-servers', 'production'], description: 'Choose the environment to deploy to')
        //string(name: 'promote_from', defaultValue: 'uat', description: 'Specify environment to promote from (bugfix or uat)')
        //string(name: 'Branch', defaultValue: '', description: 'Specify the release branch name')
        //booleanParam(name: 'blue_green', defaultValue: false, description: 'Enable blue-green deployment')
        //booleanParam(name: 'Promotion', defaultValue: true, description: 'Enable API, UI, NGINX, EDITOR deployment')
    }

    stages {
        stage('Trigger Promotions') {
            steps {
                script {
                    def promoteFromValue = params.promote_from

                    if (!(promoteFromValue in ['bugfix', 'uat'])) {
                        error "Invalid value for promote_from: ${promoteFromValue}. Please use 'bugfix' or 'uat'."
                    }

                    // First condition: Blue-green deployment if enabled
                    if (params.blue_green) {
                        def blueGreenResponse = build job: env.TRIGGER_JOB_NAME,
                            parameters: [
                                string(name: 'promote_from', value: promoteFromValue),
                                booleanParam(name: 'promote_api', value: false),
                                booleanParam(name: 'promote_ui', value: false),
                                booleanParam(name: 'promote_nginx_e-server', value: false),
                                booleanParam(name: 'promote_editor', value: false),
                                booleanParam(name: 'blue_green', value: true),
                                booleanParam(name: 'promote_nginx_production', value: false),
                                booleanParam(name: 'deploy_us_eserver', value: false),
                                booleanParam(name: 'deploy_eu_eserver', value: false),
                                booleanParam(name: 'deploy_us_production', value: false),
                                booleanParam(name: 'deploy_eu_production', value: false),
                                string(name: 'sanityLibraryBranch', value: params.Branch)
                            ], propagate: true

                        if (blueGreenResponse.result != 'SUCCESS') {
                            slackSend color: 'ff0000', message: "Failed to deploy with blue-green enabled. Job: ${env.TRIGGER_JOB_NAME} <${env.BUILD_URL}|(Open)>", tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID, channel: SLACK_CHANNEL
                            error "${env.TRIGGER_JOB_NAME} with blue-green enabled failed."
                        }
                    }

                    // Second condition: Standard promotion
                    def standardResponse = build job: env.TRIGGER_JOB_NAME,
                        parameters: [
                            string(name: 'promote_from', value: promoteFromValue),
                            booleanParam(name: 'promote_api', value: true),
                            booleanParam(name: 'promote_ui', value: true),
                            booleanParam(name: 'promote_nginx_e-server', value: true),
                            booleanParam(name: 'promote_editor', value: true),
                            booleanParam(name: 'blue_green', value: false),
                            booleanParam(name: 'promote_nginx_production', value: false),
                            booleanParam(name: 'deploy_us_eserver', value: false),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false),
                            string(name: 'sanityLibraryBranch', value: params.Branch)
                        ], propagate: true

                    if (standardResponse.result != 'SUCCESS') {
                        slackSend color: 'ff0000', message: "Failed to deploy without blue-green. Job: ${env.TRIGGER_JOB_NAME} <${env.BUILD_URL}|(Open)>", tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID, channel: SLACK_CHANNEL
                        error "${env.TRIGGER_JOB_NAME} without blue-green failed."
                    }
                }
            }
        }

        stage('Initial Success Notification') {
            steps {
                script {
                    def promotionSource = (params.promote_from == 'uat') ? 'UAT_DEPLOYED' : 'MAINTENANCE_DEPLOYED'
                    def version = params.Branch
                    def deployedItems = []

                    if (params.Promotion) {
                        if (params.blue_green) {
                            deployedItems << 'Blue_green'
                        }
                        if (params.promote_api) {
                            deployedItems << 'API'
                        }
                        if (params.promote_ui) {
                            deployedItems << 'UI'
                        }
                        if (params.promote_nginx_e_server) {
                            deployedItems << 'Nginx'
                        }
                        if (params.promote_editor) {
                            deployedItems << 'Editor'
                        }
                    }

                    def deployedList = deployedItems.join(', ')

                    slackSend(
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "Build Promotion is completed from ${promotionSource}. Release branch: ${version}. Promoted to Production → ${deployedList}. Version → ${version}",
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID
                    )
                }
            }
        }

        stage('Deploy EU e-server') {
            when {
                expression {
                    params.environment == 'e-servers'
                }
            }
            steps {
                script {
                    slackSend(
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "E server Deployment has been started on EU.",
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID
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
                            booleanParam(name: 'deploy_eu_eserver', value: true),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false),
                            string(name: 'sanityLibraryBranch', value: params.Branch)
                        ], propagate: true

                    if (euDeployResponse.result == 'SUCCESS') {
                        slackSend(
                            channel: SLACK_CHANNEL,
                            color: 'good',
                            message: "E server Deployment has been completed on EU.",
                            tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID
                        )
                    } else {
                        slackSend color: 'ff0000', message: "Failed to deploy EU e-server. Check logs for details: <${env.BUILD_URL}|(Open)>", tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID, channel: SLACK_CHANNEL
                        error "Failed to deploy EU e-server."
                    }
                }
            }
        }

        stage('Deploy US e-server') {
            when {
                expression {
                    params.environment == 'e-servers'
                }
            }
            steps {
                script {
                    slackSend(
                        channel: SLACK_CHANNEL,
                        color: 'good',
                        message: "E server Deployment has been started on US.",
                        tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID
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
                            booleanParam(name: 'deploy_us_eserver', value: true),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false),
                            string(name: 'sanityLibraryBranch', value: params.Branch)
                        ], propagate: true

                    if (usDeployResponse.result == 'SUCCESS') {
                        slackSend(
                            channel: SLACK_CHANNEL,
                            color: 'good',
                            message: "E server Deployment has been completed on US.",
                            tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID
                        )
                    } else {
                        slackSend color: 'ff0000', message: "Failed to deploy US e-server. Check logs for details: <${env.BUILD_URL}|(Open)>", tokenCredentialId: SLACK_TOKEN_CREDENTIAL_ID, channel: SLACK_CHANNEL
                        error "Failed to deploy US e-server."
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo 'Pipeline completed.'
                // Add any cleanup or final notification steps here
            }
        }
    }
}
