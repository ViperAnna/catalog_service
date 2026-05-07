pipeline {

    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {

        DOCKER_USER = 'viperanna'

        BACKEND_IMAGE = "${DOCKER_USER}/catalog-service"
        FRONTEND_IMAGE = "${DOCKER_USER}/front"

        SERVER_IP = '144.124.250.82'
        SERVER_PATH = '/home/user/catalog_service'
    }

    stages {

        stage('Detect changes') {
            steps {

                script {

                    def changes = sh(
                            script: """
                                git diff --name-only \
                                \$GIT_PREVIOUS_SUCCESSFUL_COMMIT \
                                \$GIT_COMMIT || true
                            """,
                            returnStdout: true
                    ).trim()

                    echo "Changed files:"
                    echo "${changes}"

                    env.BUILD_BACKEND = (
                            changes.contains("backend/") ||
                                    changes.contains("pom.xml")
                    ).toString()

                    env.BUILD_FRONTEND = (
                            changes.contains("frontend/") ||
                                    changes.contains("docker-compose.yml")
                    ).toString()

                    env.UPLOAD_CONFIG = (
                            changes.contains("docker-compose.yml")
                    ).toString()

                    echo "BUILD_BACKEND = ${env.BUILD_BACKEND}"
                    echo "BUILD_FRONTEND = ${env.BUILD_FRONTEND}"
                    echo "UPLOAD_CONFIG = ${env.UPLOAD_CONFIG}"
                }
            }
        }

        stage('Prepare Version') {
            steps {

                script {

                    def mvnHome = tool 'MAVEN_3'

                    env.PATH = "${mvnHome}/bin:${env.PATH}"

                    sh "${mvnHome}/bin/mvn -q -DskipTests clean compile"

                    def mavenVersion = sh(
                            script: """
                                ${mvnHome}/bin/mvn \
                                help:evaluate \
                                -Dexpression=project.version \
                                -q \
                                -DforceStdout
                            """,
                            returnStdout: true
                    ).trim()

                    def shortCommit = sh(
                            script: "git rev-parse --short HEAD",
                            returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = "${mavenVersion}-${shortCommit}"

                    echo "Maven Version: ${mavenVersion}"
                    echo "Image Tag: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build Images') {

            when {
                expression {
                    env.BUILD_BACKEND == 'true' ||
                            env.BUILD_FRONTEND == 'true'
                }
            }

            steps {

                script {

                    if (env.BUILD_BACKEND == 'true') {

                        sh """
                            docker build \
                            -t ${BACKEND_IMAGE}:${IMAGE_TAG} \
                            ./backend
                        """
                    }

                    if (env.BUILD_FRONTEND == 'true') {

                        sh """
                            docker build \
                            -t ${FRONTEND_IMAGE}:${IMAGE_TAG} \
                            ./frontend
                        """
                    }
                }
            }
        }

        stage('Push Images') {

            when {
                expression {
                    env.BUILD_BACKEND == 'true' ||
                            env.BUILD_FRONTEND == 'true'
                }
            }

            steps {

                withCredentials([
                        usernamePassword(
                                credentialsId: 'dockerhub-creds',
                                usernameVariable: 'USER',
                                passwordVariable: 'PASS'
                        )
                ]) {

                    sh "echo $PASS | docker login -u $USER --password-stdin"

                    script {

                        if (env.BUILD_BACKEND == 'true') {

                            sh """
                                docker push ${BACKEND_IMAGE}:${IMAGE_TAG}
                            """
                        }

                        if (env.BUILD_FRONTEND == 'true') {

                            sh """
                                docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}
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
                        ssh -o StrictHostKeyChecking=no root@${SERVER_IP} '
                            mkdir -p ${SERVER_PATH} &&
                            chmod 755 ${SERVER_PATH}
                        '
                    """
                }
            }
        }

        stage('Upload Config') {

            when {
                expression {
                    env.UPLOAD_CONFIG == 'true'
                }
            }

            steps {

                sshagent(['server-ssh']) {

                    withCredentials([
                            file(credentialsId: 'env-minio', variable: 'MINIO_ENV'),
                            file(credentialsId: 'env-mongodb', variable: 'MONGO_ENV'),
                            file(credentialsId: 'env-spring', variable: 'SPRING_ENV')
                    ]) {

                        sh """
                            scp -o StrictHostKeyChecking=no \
                            \$MINIO_ENV \
                            root@${SERVER_IP}:${SERVER_PATH}/.env.minio

                            scp -o StrictHostKeyChecking=no \
                            \$MONGO_ENV \
                            root@${SERVER_IP}:${SERVER_PATH}/.env.mongodb

                            scp -o StrictHostKeyChecking=no \
                            \$SPRING_ENV \
                            root@${SERVER_IP}:${SERVER_PATH}/.env.spring

                            scp -o StrictHostKeyChecking=no \
                            docker-compose.yml \
                            root@${SERVER_IP}:${SERVER_PATH}/
                        """
                    }
                }
            }
        }

        stage('Deploy') {

            when {
                expression {
                    env.BUILD_BACKEND == 'true' ||
                            env.BUILD_FRONTEND == 'true' ||
                            env.UPLOAD_CONFIG == 'true'
                }
            }

            steps {

                sshagent(['server-ssh']) {

                    script {

                        def deployCmd = ""

                        if (
                                env.BUILD_BACKEND == 'true' &&
                                        env.BUILD_FRONTEND == 'true'
                        ) {

                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose pull &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d --remove-orphans
                            """

                        } else if (env.BUILD_BACKEND == 'true') {

                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose pull catalog-service &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d catalog-service --remove-orphans
                            """

                        } else if (env.BUILD_FRONTEND == 'true') {

                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose pull front &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d front --remove-orphans
                            """

                        } else if (env.UPLOAD_CONFIG == 'true') {

                            deployCmd = """
                                cd ${SERVER_PATH} &&
                                IMAGE_TAG=${IMAGE_TAG} docker compose up -d --remove-orphans
                            """
                        }

                        sh """
                            ssh -o StrictHostKeyChecking=no \
                            root@${SERVER_IP} '
                                ${deployCmd}
                            '
                        """
                    }
                }
            }
        }

        stage('Cleanup Old Images') {
            steps {

                sshagent(['server-ssh']) {

                    sh '''
                ssh -o StrictHostKeyChecking=no root@${SERVER_IP} "
                cat > /tmp/cleanup.sh << 'SCRIPT'
                #!/bin/bash

                for IMAGE in ${DOCKER_USER}/catalog-service ${DOCKER_USER}/front; do

                    echo \\"Cleaning old images for \$IMAGE...\\" 

                    docker images \\"\$IMAGE\\" --format '{{.ID}} {{.Tag}}' |
                    sort -k2 -V -r |
                    tail -n +4 |
                    awk '{print \$1}' |
                    xargs -r docker rmi -f || true

                done

                docker system prune -f --volumes || true

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
            echo "Deployed version: ${env.IMAGE_TAG} to branch ${env.BRANCH_NAME}"
        }

        failure {
            echo "Deployment failed! Check the console output."
        }
    }
}