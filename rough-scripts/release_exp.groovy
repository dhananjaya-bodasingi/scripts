pipeline {
    agent any
    parameters {
        choice(name: 'promote_from', choices: ['uat', 'bugfix'], description: 'Promotion Source')
        booleanParam(name: 'blue_green', defaultValue: false, description: 'Blue-Green Deployment Required')
        booleanParam(name: 'deploy_us_eserver', defaultValue: true, description: 'Deploy to US E Server')
        booleanParam(name: 'deploy_eu_eserver', defaultValue: true, description: 'Deploy to EU E Server')
    }
    
    stages {
        stage('Promotion') {
            steps {
                script {
                    if (params.blue_green) {
                        echo "Starting Blue-Green promotion"
                        build job: 'promote_api', parameters: [[$class: 'StringParameterValue', name: 'promote_from', value: params.promote_from]]
                        build job: 'blue_green'
                        build job: 'promote_ui'
                        build job: 'promote_editor'
                    } else {
                        echo "Promoting all at once"
                        build job: 'promote_all', parameters: [[$class: 'StringParameterValue', name: 'promote_from', value: params.promote_from]]
                    }
                }
            }
        }
        
        stage('Deploy to E Servers') {
            parallel {
                stage('Deploy to US E Server') {
                    when { expression { params.deploy_us_eserver } }
                    steps {
                        build job: 'deploy_us_eserver'
                    }
                }
                stage('Deploy to EU E Server') {
                    when { expression { params.deploy_eu_eserver } }
                    steps {
                        build job: 'deploy_eu_eserver'
                    }
                }
            }
        }
        
        stage('Post Deployment') {
            steps {
                echo "Triggering Nginx deployment and cache purge"
                build job: 'PROD-US-West-nginx-Deployment'
                build job: 'PROD-EU-northeurope-nginx-deployment'
                build job: 'production-Cloudflare-CachePurge'
            }
        }
    }
}
