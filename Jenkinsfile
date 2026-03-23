pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'develop', description: 'Branch for build')
    }

    environment {
        DOCKER_USER = 'viperanna'
        BACKEND_IMAGE = "${DOCKER_USER}/catalog-service"
        FRONTEND_IMAGE = "${DOCKER_USER}/front"

        SERVER_IP = '144.124.250.82'
        SERVER_PATH = '/home/user/catalog_service/'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        userRemoteConfigs: [[url: 'https://github.com/ViperAnna/catalog_service.git']]
                ])
            }
        }

        stage('Build Maven Project') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    sh "git fetch --all"

                    def backendChanged = sh(
                            script: "git diff --name-only origin/${params.BRANCH_NAME} HEAD | grep '^backend/' || true",
                            returnStdout: true
                    ).trim()

                    def frontendChanged = sh(
                            script: "git diff --name-only origin/${params.BRANCH_NAME} HEAD | grep '^frontend/' || true",
                            returnStdout: true
                    ).trim()

                    if (backendChanged) {
                        echo "Backend changes detected, building image..."
                        sh "docker build -t $BACKEND_IMAGE:latest ./backend"
                    } else {
                        echo "Backend unchanged, skipping build"
                    }

                    if (frontendChanged) {
                        echo "Frontend changes detected, building image..."
                        sh "docker build -t $FRONTEND_IMAGE:latest ./frontend"
                    } else {
                        echo "Frontend unchanged, skipping build"
                    }
                }
            }
        }
        stage('Push Images') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'USER',
                        passwordVariable: 'PASS'
                )]) {
                    sh '''
                    echo $PASS | docker login -u $USER --password-stdin
                    docker push viperanna/catalog-service:latest
                    docker push viperanna/front:latest
                    '''
                }
            }
        }

        stage('Deploy Application') {
            steps {
                sshagent(['server-ssh']) {
                    sh """
                    ssh root@${SERVER_IP} '
                        cd ${SERVER_PATH} &&
                        docker compose pull &&
                        docker compose up -d
                    '
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Build and deployment completed successfully!'
        }
        failure {
            echo 'Something went wrong. Check the logs.'
        }
    }
}