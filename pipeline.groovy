pipeline {
    agent any
    stages {
        stage('Checkout Code in master') {
            steps {
                script {
                    sh '''
                        rm -rf projCert
                        git clone https://github.com/tharik007pec/projCert.git
                        cd projCert
                        ls
                    '''
                }
            }
        }
        stage('Install Docker with Ansible') {
            steps {
                // Run Ansible playbook to install Docker on the test server
                script {
                    sh '''
                        ls
                        pwd
                        cd projCert
                        ansible-playbook -i inventory.ini instal-docker.yml --extra-vars "ansible_password=admin ansible_sudo_pass=admin"
                    '''
                }
            }
        }
        stage('Checkout Code in slave') {
            agent {node { label 'jenkins_slave' }}
            steps {
                script {
                    sh '''
                        rm -rf projCert
                        git clone https://github.com/tharik007pec/projCert.git
                        cd projCert
                        ls
                    '''
                }
            }
        }
        stage('Build Docker Container') {
            agent {node { label 'jenkins_slave' }}
            steps {
                script {
                    try {
                        // Build the Docker image (if not already built)
                        sh '''
                            ls
                            pwd
                            cd projCert
                            sudo docker build -t my-php-app .
                        '''
                        // docker.build('my-php-app')
                        echo 'Docker container is running successfully.'
                    } catch (Exception e) {
                         echo "Error: ${e.getMessage()}"
                    }
                }
            }
        }
        stage('Run Docker Container') {
            agent {node { label 'jenkins_slave' }}
            steps {
                // Run the Docker container
                script {
                    try{
                        // sh '''
                        //   docker run -p 8080:80 -d my-php-app:latest > containId
                        // '''
                        // echo "Docker runing ${containId}"
                        // docker.image('my-php-app:latest').withRun('-p 8080:80', '--name php-app-container')
                        def containerId = sh(script: 'docker run --name sample-one -p 8080:80 -d my-php-app:latest', returnStdout: true).trim()
                        // Wait for a few seconds to allow the container to start (optional)
                        sleep(time: 10, unit: 'SECONDS')
                        echo "Docker container started with ID: $containerId"
                        // Run the docker inspect command and capture the JSON output
                        def inspectOutput = sh(script: "docker inspect -f '{{.State.Running}}' $containerId", returnStdout: true).trim()
    
                        // Convert the JSON output to a boolean value
                        def isRunning = inspectOutput.toBoolean()
    
                        if (isRunning) {
                            echo "Container $containerId is running."
                        } else {
                            throw new Exception('Docker container is not running.')
                        }
                    } catch (Exception e) {
                        echo "Error: ${e.getMessage()}"
                         // If an error occurs, stop and remove the container
                        try {
                            sh "docker stop sample-one || true"
                        } catch (Exception stopError) {
                            echo "Error stopping container: ${stopError.getMessage()}"
                        }

                        try {
                            sh "docker rm sample-one || true"
                        } catch (Exception rmError) {
                            echo "Error removing container: ${rmError.getMessage()}"
                        }

                        currentBuild.result = 'FAILURE'
                        error 'Docker container failed to build and deploy.'
                    }
                }
            }
        }
    }
}