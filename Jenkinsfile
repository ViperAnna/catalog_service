pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'develop', description: 'Branch for build')
        choice(name: 'DEPLOY_TARGET', choices: ['all', 'backend', 'frontend'], description: 'What do I want to deploy?')
    }

    environment {
        DOCKER_USER = 'viperanna'
        BACKEND_IMAGE = "${DOCKER_USER}/catalog-service"
        FRONTEND_IMAGE = "${DOCKER_USER}/front"

        SERVER_IP = '144.124.250.82'
        SERVER_PATH = '/home/user/catalog_service'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                        $class           : 'GitSCM',
                        branches         : [[name: "*/${params.BRANCH_NAME}"]],
                        userRemoteConfigs: [[url: 'https://github.com/ViperAnna/catalog_service.git']]
                ])
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    if (params.DEPLOY_TARGET == 'all' || params.DEPLOY_TARGET == 'backend') {
                        echo "Building backend image..."
                        sh "docker build -t $BACKEND_IMAGE:latest ./backend"
                    }

                    if (params.DEPLOY_TARGET == 'all' || params.DEPLOY_TARGET == 'frontend') {
                        echo "Building frontend image..."
                        sh "docker build -t $FRONTEND_IMAGE:latest ./frontend"
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
                    script {
                        if (params.DEPLOY_TARGET == 'all' || params.DEPLOY_TARGET == 'backend') {
                            sh '''
                                echo $PASS | docker login -u $USER --password-stdin
                                docker push $BACKEND_IMAGE:latest
                            '''
                        }
                        if (params.DEPLOY_TARGET == 'all' || params.DEPLOY_TARGET == 'frontend') {
                            sh '''
                                echo $PASS | docker login -u $USER --password-stdin
                                docker push $FRONTEND_IMAGE:latest
                            '''
                        }
                    }
                }
            }
        }

        stage('Deploy Application') {
            steps {
                sshagent(['server-ssh']) {
                    script {
                        def cleanCmd = ""
                        if (params.DEPLOY_TARGET == "all") {
                            cleanCmd = "docker compose down && docker system prune -af --volumes"
                        } else if (params.DEPLOY_TARGET == "backend") {
                            cleanCmd = "docker compose down catalog-service mongodb && docker image prune -af"
                        } else if (params.DEPLOY_TARGET == "frontend") {
                            cleanCmd = "docker compose down front && docker image prune -af"
                        }

                        def deployCmd = ""
                        if (params.DEPLOY_TARGET == "all") {
                            deployCmd = "docker compose pull && docker compose up -d"
                        } else if (params.DEPLOY_TARGET == "backend") {
                            deployCmd = "docker compose pull catalog-service && docker compose up -d catalog-service mongodb"
                        } else if (params.DEPLOY_TARGET == "frontend") {
                            deployCmd = "docker compose pull front && docker compose up -d front"
                        }

                        sh """
                            ssh root@${SERVER_IP} "cd ${SERVER_PATH} && ${cleanCmd}"
                            ssh root@${SERVER_IP} "cd ${SERVER_PATH} && ${deployCmd}"
                        """
                    }
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