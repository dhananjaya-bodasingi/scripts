pipeline {
    agent any
    
    // Job Links
    // PRODUCTION-all-trigger-job-v2: "https://mamaops.quickolabs.com/jenkins/job/PRODUCTION-all-trigger-job-v2/build?delay=0sec"
    // promote-whatfix-nginx-eserver: "https://devops.quickolabs.com/view/Build%20Promotions/job/promote-whatfix-nginx-eserver/build?delay=0sec"
    // PROD-Nginx-UI-Deployment-East-EA-E-Server: "https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-Nginx-UI-Deployment-East-EA-E-Server/"
    // PROD-Nginx-UI-Deployment-EU-E-Server: "https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-Nginx-UI-Deployment-EU-E-Server/"
    // promote-whatfix-nginx: "https://devops.quickolabs.com/view/Build%20Promotions/job/promote-whatfix-nginx/build?delay=0sec"
    // PROD-US-West-nginx-Deployment: "https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-US-West-nginx-Deployment/"
    // PROD-US-East-nginx-Deployment: "https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-US-East-nginx-Deployment/"
    // production-Cloudflare-CachePurge: "https://prodops.whatfix.com/job/production-Cloudflare-CachePurge/build?delay=0sec"
    // PROD-EU-northeurope-nginx-deployment: "https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-EU-northeurope-nginx-deployment/"
    // PROD-EU-switzerland-nginx-deployment: "https://prodops.whatfix.com/view/Production%20Deployments%20WIP/job/PROD-EU-switzerland-nginx-deployment/"



    stages {
        stage('Trigger Mamaops Job 1') {
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
                        // Determine the environment based on promote_from value for promote-whatfix-nginx-eserver
                        def envValue = (promoteFromValue == 'bugfix') ? 'maintenance' : 'uataz'

                        // Check parameters for downstream job
                        def promoteApi = response.getEnvironment().get('promote_api')?.toBoolean()
                        def promoteUi = response.getEnvironment().get('promote_ui')?.toBoolean()
                        def promoteEditor = response.getEnvironment().get('promote_editor')?.toBoolean()

                        // Triggering promote-whatfix-nginx-eserver job based on conditions
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

                    // Check if success
                    if (response.result == 'SUCCESS') {
                        def deploymentResponse = build job: 'PROD-Nginx-UI-Deployment-EU-E-Server', 
                            propagate: true

                        // Check status PROD-Nginx-UI-Deployment-EU-E-Server
                        if (deploymentResponse.result == 'SUCCESS') {
                            def sreResponse = build job: '<SRE_manual_test>', 
                                propagate: true

                            // Check status SRE_manual_test job
                            if (sreResponse.result == 'SUCCESS') {
                                // Trigger prod-eu-eserver sanity test
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

                    // Check if success
                    if (response.result == 'SUCCESS') {
                        def deploymentResponse = build job: 'PROD-Nginx-UI-Deployment-East-EA-E-Server', 
                            propagate: true

                        // Check the status of PROD-Nginx-UI-Deployment-East-EA-E-Server
                        if (deploymentResponse.result == 'SUCCESS') {
                            // Trigger SRE_manual_test
                            def sreResponse = build job: '<SRE_manual_test>', 
                                propagate: true

                            // Check status SRE_manual_test
                            if (sreResponse.result == 'SUCCESS') {
                                // Trigger prod-us-eserver sanity test
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

