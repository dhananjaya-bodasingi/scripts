pipeline {
    agent any
    parameters {
        string(name: 'TARGET_DC', defaultValue: 'useast', description: 'Target Data Center')
        choice(name: 'ENVIRONMENT', choices: ['production', 'staging'], description: 'Environment')
    }
    stages {
        stage('Run Ansible Playbook') {
            steps {
                script {
                    // Update the Ansible inventory path and playbook path as per your setup
                    def inventoryPath = "inventories/${params.ENVIRONMENT}"
                    def playbookPath = "backup_logs.yml"

                    // Call Ansible playbook and pass parameters like target_hosts
                    sh """
                        ansible-playbook ${playbookPath} \
                        -i ${inventoryPath} \
                        --extra-vars "target_hosts=${params.TARGET_DC}"
                    """
                }
            }
        }
    }
}
