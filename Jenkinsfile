pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'develop', description: 'Branch for build')
        choice(name: 'DEPLOY_TARGET', choices: ['all', 'backend', 'frontend'], description: 'What to deploy?')
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

        stage('Prepare Version') {
            steps {
                script {
                    def mvnHome = tool 'MAVEN_3'
                    env.PATH = "${mvnHome}/bin:${env.PATH}"

                    sh "${mvnHome}/bin/mvn -v"
                    sh "${mvnHome}/bin/mvn -q -DskipTests clean compile"

                    def mavenVersion = sh(
                            script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                            returnStdout: true
                    ).trim()

                    def shortCommit = sh(
                            script: "git rev-parse --short HEAD",
                            returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = "${mavenVersion}-${shortCommit}"

                    echo "Version from Maven: ${mavenVersion}"
                    echo "Version: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build Images') {
            steps {
                script {

                    if (params.DEPLOY_TARGET in ['all', 'backend']) {
                        sh """
                            docker build -t $BACKEND_IMAGE:${IMAGE_TAG} ./backend
                        """
                    }

                    if (params.DEPLOY_TARGET in ['all', 'frontend']) {
                        sh """
                            docker build -t $FRONTEND_IMAGE:${IMAGE_TAG} ./frontend
                        """
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

                    sh """
                        echo $PASS | docker login -u $USER --password-stdin
                    """

                    script {

                        if (params.DEPLOY_TARGET in ['all', 'backend']) {
                            sh """
                                docker push $BACKEND_IMAGE:${IMAGE_TAG}
                            """
                        }

                        if (params.DEPLOY_TARGET in ['all', 'frontend']) {
                            sh """
                                docker push $FRONTEND_IMAGE:${IMAGE_TAG}
                            """
                        }
                    }
                }
            }
        }

        stage('Prepare Server') {
            steps {
                sshagent(['server-ssh']) {
                    sh """
                        ssh root@${SERVER_IP} "mkdir -p ${SERVER_PATH}"
                    """
                }
            }
        }

        stage('Upload Config') {
            when {
                expression { params.DEPLOY_TARGET == 'all' }
            }
            steps {
                sshagent(['server-ssh']) {
                    withCredentials([
                            file(credentialsId: 'env-minio', variable: 'MINIO_ENV'),
                            file(credentialsId: 'env-mongodb', variable: 'MONGO_ENV'),
                            file(credentialsId: 'env-spring', variable: 'SPRING_ENV')
                    ]) {
                        sh """
                            scp $MINIO_ENV root@${SERVER_IP}:${SERVER_PATH}/.env.minio
                            scp $MONGO_ENV root@${SERVER_IP}:${SERVER_PATH}/.env.mongodb
                            scp $SPRING_ENV root@${SERVER_IP}:${SERVER_PATH}/.env.spring
                            scp docker-compose.yml root@${SERVER_IP}:${SERVER_PATH}/
                        """
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sshagent(['server-ssh']) {
                    script {

                        def deployCmd = ""

                        if (params.DEPLOY_TARGET == "all") {
                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose pull &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d
                            """
                        } else if (params.DEPLOY_TARGET == "backend") {
                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose pull catalog-service &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d catalog-service
                            """
                        } else if (params.DEPLOY_TARGET == "frontend") {
                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose pull front &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d front
                            """
                        }

                        sh """
                            ssh root@${SERVER_IP} '${deployCmd}'
                        """
                    }
                }
            }
        }

        stage('Cleanup Old Images') {
            steps {
                sshagent(['server-ssh']) {
                    sh '''
                    ssh root@${SERVER_IP} "cat > /tmp/cleanup.sh << 'SCRIPT'
                    #!/bin/bash
                    set -e
                    
                    echo 'Cleaning old Docker images...'
                    
                    docker images ${DOCKER_USER}/catalog-service --format '{{.ID}} {{.Tag}}' |
                    sort -k2 -V -r | 
                    tail -n +4 |     
                    awk '{print \$1}' |
                    xargs -r docker rmi -f || true

                    docker images ${DOCKER_USER}/front --format '{{.ID}} {{.Tag}}' |
                    sort -k2 -V -r |
                    tail -n +4 |
                    awk '{print \$1}' |
                    xargs -r docker rmi -f || true

                    docker image prune -f
                    SCRIPT
            
                    chmod +x /tmp/cleanup.sh
                    /tmp/cleanup.sh
                    "
                    '''
                }
            }
        }
    }


    post {
        success {
            echo "Deployed version: ${env.IMAGE_TAG}"
        }
        failure {
            echo "Deployment failed"
        }
    }
}