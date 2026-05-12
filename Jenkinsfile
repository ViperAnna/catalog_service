pipeline {

    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        DOCKERHUB_USER = 'viperanna'

        BACKEND_IMAGE = "${DOCKERHUB_USER}/catalog-service"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/front"

        SERVER_IP = '144.124.250.82'
        SERVER_PATH = '/home/user/catalog_service'
    }

    stages {
        // =========================================================
        // Detect changes
        // =========================================================
        stage('Detect changes') {
            steps {
                script {

                    def previousCommit = sh(
                            script: "git rev-parse HEAD~1 || echo ''",
                            returnStdout: true
                    ).trim()

                    def firstDeploy = (previousCommit == '')

                    echo "PREVIOUS COMMIT: ${previousCommit}"
                    echo "CURRENT COMMIT : ${env.GIT_COMMIT}"

                    def changes = previousCommit
                            ? sh(script: "git diff --name-only ${previousCommit} ${env.GIT_COMMIT}", returnStdout: true).trim()
                            : sh(script: "git ls-files", returnStdout: true).trim()

                    echo "Changed files:\n${changes}"

                    env.BUILD_BACKEND = (
                            changes.contains("backend/") ||
                                    changes.contains("pom.xml")
                    ).toString()

                    env.BUILD_FRONTEND = (
                            changes.contains("frontend/") ||
                                    changes.contains("package.json")
                    ).toString()

                    env.UPLOAD_CONFIG = (
                            changes.contains("docker-compose.yml") || firstDeploy
                    ).toString()

                    echo "BUILD_BACKEND = ${env.BUILD_BACKEND}"
                    echo "BUILD_FRONTEND = ${env.BUILD_FRONTEND}"
                    echo "UPLOAD_CONFIG = ${env.UPLOAD_CONFIG}"
                }
            }
        }

        // =========================================================
        // Prepare version
        // =========================================================
        stage('Prepare Version') {
            steps {
                script {

                    def mvnHome = tool 'MAVEN_3'

                    sh "${mvnHome}/bin/mvn -q -DskipTests clean compile"

                    env.APP_VERSION = sh(
                            script: """
                            ${mvnHome}/bin/mvn help:evaluate \
                            -Dexpression=project.version \
                            -q -DforceStdout
                        """,
                            returnStdout: true
                    ).trim()

                    env.GIT_SHORT = sh(
                            script: 'git rev-parse --short HEAD',
                            returnStdout: true
                    ).trim()

                    env.IMAGE_TAG =
                            "${env.APP_VERSION}-${env.GIT_SHORT}"

                    echo "IMAGE_TAG = ${env.IMAGE_TAG}"
                }
            }
        }

        // =========================================================
        // Resolve deploy tags
        // =========================================================
        stage('Resolve Deploy Tags') {

            when {
                branch 'develop'
            }

            steps {
                sshagent(['server-ssh']) {
                    script {

                        //
                        // BACKEND TAG
                        //

                        def backendTagFromServer = sh(
                                script: """
                        ssh -o StrictHostKeyChecking=no root@${SERVER_IP} \
                        "cat ${SERVER_PATH}/current_backend_tag 2>/dev/null || true"
                    """,
                                returnStdout: true
                        ).trim()

                        if (env.BUILD_BACKEND == 'true') {

                            env.DEPLOY_BACKEND_TAG =
                                    env.IMAGE_TAG

                        } else {

                            env.DEPLOY_BACKEND_TAG =
                                    backendTagFromServer ?: env.IMAGE_TAG
                        }

                        //
                        // FRONTEND TAG
                        //

                        def frontendTagFromServer = sh(
                                script: """
                        ssh -o StrictHostKeyChecking=no root@${SERVER_IP} \
                        "cat ${SERVER_PATH}/current_frontend_tag 2>/dev/null || true"
                    """,
                                returnStdout: true
                        ).trim()

                        if (env.BUILD_FRONTEND == 'true') {

                            env.DEPLOY_FRONTEND_TAG =
                                    env.IMAGE_TAG

                        } else {

                            env.DEPLOY_FRONTEND_TAG =
                                    frontendTagFromServer ?: env.IMAGE_TAG
                        }

                        echo "DEPLOY_BACKEND_TAG = ${env.DEPLOY_BACKEND_TAG}"
                        echo "DEPLOY_FRONTEND_TAG = ${env.DEPLOY_FRONTEND_TAG}"
                    }
                }
            }
        }

        // =========================================================
        // Build backend
        // =========================================================
        stage('Build Backend Image') {

            when {
                expression {
                    env.BUILD_BACKEND == 'true'
                }
            }

            steps {

                sh """
                    ${echo ls} && \
                    docker build \
                      -t ${BACKEND_IMAGE}:${IMAGE_TAG} \
                      ./backend
                """
            }
        }

        // =========================================================
        // Build frontend
        // =========================================================
        stage('Build Frontend Image') {

            when {
                expression {
                    env.BUILD_FRONTEND == 'true'
                }
            }

            steps {
                sh """
                    docker build \
                      -t ${FRONTEND_IMAGE}:${IMAGE_TAG} \
                      ./frontend
                """
            }
        }

        //=========================================================
        //Docker Login
        //=========================================================
        stage('Docker Login') {
            when {
                expression {
                    env.BUILD_BACKEND == 'true' || env.BUILD_FRONTEND == 'true'
                }
            }
            steps {
                withCredentials([
                        usernamePassword(
                                credentialsId: 'dockerhub-creds',
                                usernameVariable: 'DOCKER_LOGIN_USER',
                                passwordVariable: 'DOCKER_PASS'
                        )
                ]) {

                    sh """
                        echo "$DOCKER_PASS" | docker login \
                          -u "$DOCKER_LOGIN_USER" \
                          --password-stdin
                    """
                }
            }
        }

        // =========================================================
        // Push backend
        // =========================================================
        stage('Push Backend Image') {

            when {
                allOf {
                    expression { env.BUILD_BACKEND == 'true' }
                }
            }

            steps {
                sh """
                        docker push ${BACKEND_IMAGE}:${IMAGE_TAG}
                    """
            }
        }

        // =========================================================
        // Push frontend
        // =========================================================
        stage('Push Frontend Image') {
            when {
                allOf {
                    expression { env.BUILD_FRONTEND == 'true' }
                }
            }

            steps {
                sh """
                        docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}
                    """
            }
        }

        // =========================================================
        // Save deployed tags
        // =========================================================
        stage('Save Deploy Tags') {

            when {
                branch 'develop'
            }

            steps {
                sshagent(['server-ssh']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no \
                        root@${SERVER_IP} '

                            mkdir -p ${SERVER_PATH}

                            echo "${DEPLOY_BACKEND_TAG}" \
                                > ${SERVER_PATH}/current_backend_tag

                            echo "${DEPLOY_FRONTEND_TAG}" \
                                > ${SERVER_PATH}/current_frontend_tag
                        '
                    """
                }
            }
        }

        // =========================================================
        // Upload Config
        // =========================================================
        stage('Upload Config') {

            when {
                allOf {
                    branch 'develop'
                    expression {
                        env.UPLOAD_CONFIG == 'true'
                    }
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
                    scp -o StrictHostKeyChecking=no \$MINIO_ENV \
                        root@${SERVER_IP}:${SERVER_PATH}/.env.minio

                    scp -o StrictHostKeyChecking=no \$MONGO_ENV \
                        root@${SERVER_IP}:${SERVER_PATH}/.env.mongodb

                    scp -o StrictHostKeyChecking=no \$SPRING_ENV \
                        root@${SERVER_IP}:${SERVER_PATH}/.env.spring

                    scp -o StrictHostKeyChecking=no \
                        docker-compose.yml \
                        root@${SERVER_IP}:${SERVER_PATH}/
                """
                    }
                }
            }
        }

        // =========================================================
        // Deploy
        // =========================================================
        stage('Deploy') {

            when {
                branch 'develop'
            }

            steps {
                sshagent(['server-ssh']) {
                    sh """
                    ssh -o StrictHostKeyChecking=no root@${SERVER_IP} '
                    cd ${SERVER_PATH}

                    export BACKEND_TAG=${DEPLOY_BACKEND_TAG}
                    export FRONTEND_TAG=${DEPLOY_FRONTEND_TAG}

                    docker compose pull
                    docker compose up -d
                '
            """
                }
            }
        }
    }

    post {
        always {

            echo "Backend tag: ${env.DEPLOY_BACKEND_TAG}"
            echo "Frontend tag: ${env.DEPLOY_FRONTEND_TAG}"
        }
    }
}