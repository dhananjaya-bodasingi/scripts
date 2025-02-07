pipeline {
    agent any
    
    // Job Links
    // (Your job links here...)

    environment {
        DEVOPS_JENKINS_API_TOKEN = "1196c185ed328fcb7b6daa91a17d4e63f1"
        MAMAOPS_JENKINS_API_TOKEN = "1196c185ed328fcb7b6daa91a17d4e63f1"
        PRODOPS_JENKINS_API_TOKEN = "1130b43a17ee8c8396134c262226d30f41"
        JENKINS_USER = "dhananjaya.bodasingi"
    }

    stages {
        stage('Trigger Mamaops Job 1') {
            when {
                expression {
                    return params.triggerMamaopsJob1 == 'true' 
                }
            }
            steps {
                script {
                    def promoteFromValue = params.promote_from
                    
                    if (!(promoteFromValue in ['bugfix', 'uat'])) {
                        error "Invalid value for promote_from: ${promoteFromValue}. Please use 'bugfix' or 'uat'."
                    }

                    def response = build job: 'PRODUCTION-all-trigger-job-v2',
                        parameters: [
                            string(name: 'promote_from', value: promoteFromValue),
                            booleanParam(name: 'promote_api', value: true),
                            booleanParam(name: 'promote_ui', value: true),
                            booleanParam(name: 'promote_editor', value: true),
                            booleanParam(name: 'blue_green', value: false), 
                            booleanParam(name: 'deploy_us_eserver', value: false),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false)
                        ], propagate: true

                    // Check if successful
                    if (response.result == 'SUCCESS') {
                        def envValue = (promoteFromValue == 'bugfix') ? 'maintenance' : 'uataz'

                        def promoteApi = response.getEnvironment().get('promote_api')?.toBoolean()
                        def promoteUi = response.getEnvironment().get('promote_ui')?.toBoolean()
                        def promoteEditor = response.getEnvironment().get('promote_editor')?.toBoolean()

                        if (promoteApi && promoteUi && promoteEditor) {
                            def promoteResponse = build job: 'promote-whatfix-nginx-eserver', 
                                parameters: [
                                    string(name: 'environment', value: envValue)
                                ], propagate: true
                            if (promoteResponse.result != 'SUCCESS') {
                                error "promote-whatfix-nginx-eserver job failed."
                            }
                        }
                    } else {
                        error "PRODUCTION-all-trigger-job-v2 failed."
                    }
                }
            }
        }

        // Stage 2: 
        stage('Trigger Mamaops Job 2') {
            steps {
                script {
                    def promoteFromValue = params.promote_from 

                    def response = build job: 'PRODUCTION-all-trigger-job-v2',
                        parameters: [
                            string(name: 'promote_from', value: promoteFromValue),
                            booleanParam(name: 'promote_api', value: false),
                            booleanParam(name: 'promote_ui', value: false),
                            booleanParam(name: 'promote_editor', value: false),
                            booleanParam(name: 'blue_green', value: false),
                            booleanParam(name: 'deploy_us_eserver', value = false),
                            booleanParam(name: 'deploy_eu_eserver', value: true),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false)
                        ], propagate: true

                    if (response.result == 'SUCCESS') {
                        def deploymentResponse = build job: 'PROD-Nginx-UI-Deployment-EU-E-Server', 
                            propagate: true

                        if (deploymentResponse.result == 'SUCCESS') {
                            def sreResponse = build job: '<SRE_manual_test>', 
                                propagate: true

                            if (sreResponse.result == 'SUCCESS') {
                                def sanityResponse = build job: 'ui-test-automation-prod-eu-sanity-browserstack', 
                                    propagate: true
                                if (sanityResponse.result != 'SUCCESS') {
                                    error "ui-test-automation-prod-eu-sanity-browserstack job failed"
                                }
                            } else {
                                error "SRE_manual_test failed."
                            }
                        } else {
                            error "PROD-Nginx-UI-Deployment-EU-E-Server failed."
                        }
                    } else {
                        error "PRODUCTION-all-trigger-job-v2 failed."
                    }
                }
            }
        }

        // Stage 3: 
        stage('Trigger Mamaops Job 3') {
            steps {
                script {
                    def promoteFromValue = params.promote_from 

                    def response = build job: 'PRODUCTION-all-trigger-job-v2',
                        parameters: [
                            string(name: 'promote_from', value: promoteFromValue), 
                            booleanParam(name: 'promote_api', value: false),
                            booleanParam(name: 'promote_ui', value: false),
                            booleanParam(name: 'promote_editor', value: false),
                            booleanParam(name: 'blue_green', value: false),
                            booleanParam(name: 'deploy_us_eserver', value: true),
                            booleanParam(name: 'deploy_eu_eserver', value: false),
                            booleanParam(name: 'deploy_us_production', value: false),
                            booleanParam(name: 'deploy_eu_production', value: false)
                        ], propagate: true

                    if (response.result == 'SUCCESS') {
                        def deploymentResponse = build job: 'PROD-Nginx-UI-Deployment-East-EA-E-Server', 
                            propagate: true

                        if (deploymentResponse.result == 'SUCCESS') {
                            def sreResponse = build job: '<SRE_manual_test>', 
                                propagate: true

                            if (sreResponse.result == 'SUCCESS') {
                                def sanityResponse = build job: 'ui-test-automation-prod-us-sanity-browserstack', 
                                    propagate: true
                                if (sanityResponse.result != 'SUCCESS') {
                                    error "ui-test-automation-prod-us-sanity-browserstack job failed."
                                }
                            } else {
                                error "SRE_manual_test failed."
                            }
                        } else {
                            error "PROD-Nginx-UI-Deployment-East-EA-E-Server failed."
                        }
                    } else {
                        error "PRODUCTION-all-trigger-job-v2 failed."
                    }
                }
            }
        }
    }
}
