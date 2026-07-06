pipeline {

    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        DOCKERHUB_USER = 'viperanna'

        FRONTEND_IMAGE = "${DOCKERHUB_USER}/front"
        BACKEND_IMAGE = "${DOCKERHUB_USER}/catalog-service"
        USER_IMAGE = "${DOCKERHUB_USER}/user-service"
        NOTIFICATION_IMAGE = "${DOCKERHUB_USER}/notification-service"
        DISCOVERY_IMAGE = "${DOCKERHUB_USER}/discovery-service"
        GATEWAY_IMAGE = "${DOCKERHUB_USER}/api-gateway"

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

                    def changed = { path ->
                        changedFiles.any { it.startsWith(path) }
                    }

                    def changedFiles = changes?.trim()
                            ? changes.split("\n").collect { it.trim() }
                            : []


                    def services = [
                            "catalog-service"     : "BUILD_CATALOG_SERVICE",
                            "user-service"        : "BUILD_USER_SERVICE",
                            "notification-service": "BUILD_NOTIFICATION_SERVICE",
                            "api-gateway"         : "BUILD_GATEWAY_SERVICE",
                            "discovery-service"   : "BUILD_DISCOVERY_SERVICE",
                            "frontend"            : "BUILD_FRONTEND"
                    ]
                    for (entry in services) {

                        def path = entry.key
                        def envName = entry.value

                        env[envName] = changed(path).toString()
                        echo "${envName} = ${env[envName]}"
                    }


                    echo "BUILD_CATALOG_SERVICE = ${env.BUILD_CATALOG_SERVICE}"
                    echo "BUILD_USER_SERVICE = ${env.BUILD_USER_SERVICE}"
                    echo "BUILD_NOTIFICATION_SERVICE = ${env.BUILD_NOTIFICATION_SERVICE}"
                    echo "BUILD_GATEWAY_SERVICE = ${env.BUILD_GATEWAY_SERVICE}"
                    echo "BUILD_DISCOVERY_SERVICE = ${env.BUILD_DISCOVERY_SERVICE}"
                    echo "BUILD_FRONTEND = ${env.BUILD_FRONTEND}"
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
            def deployTags = [:]

            steps {
                sshagent(['server-ssh']) {
                    script {
                        def services = [
                                "catalog-service"     : "current_catalog_tag",
                                "user-service"        : "current_user_tag",
                                "notification-service": "current_notification_tag",
                                "api-gateway"         : "current_gateway_tag",
                                "discovery-service"   : "current_discovery_tag",
                                "frontend"            : "current_frontend_tag"
                        ]


                        for (entry in services) {

                            def service = entry.key
                            def tagFile = entry.value

                            def serverTag = sh(
                                    script: """
                                    ssh -o StrictHostKeyChecking=no root@${SERVER_IP} \
                                    "cat ${SERVER_PATH}/${tagFile} 2>/dev/null || true"
                                    """,
                                    returnStdout: true
                            ).trim()

                            def buildFlag = env["BUILD_${service.replace('-', '_').toUpperCase()}"]

                            deployTags.put(service,
                                    (buildFlag == 'true')
                                            ? env.IMAGE_TAG
                                            : (serverTag ?: env.IMAGE_TAG)
                            )

                            echo "DEPLOY_${service.toUpperCase().replace('-', '_')} = ${deployTags[service]}"
                        }
                    }
                }
            }
        }

        // =========================================================
        // Build images
        // =========================================================
        stage('Build Images') {
            when {
                expression {
                    env.BUILD_FRONTEND == 'true' ||
                            env.BUILD_CATALOG_SERVICE == 'true' ||
                            env.BUILD_USER_SERVICE == 'true' ||
                            env.BUILD_NOTIFICATION_SERVICE == 'true' ||
                            env.BUILD_GATEWAY_SERVICE == 'true' ||
                            env.BUILD_DISCOVERY_SERVICE == 'true'
                }
            }

            steps {
                script {
                    def builds = [
                            "catalog-service"     : ["./catalog-service", "${DOCKERHUB_USER}/catalog-service", env.BUILD_CATALOG_SERVICE],
                            "user-service"        : ["./user-service", "${DOCKERHUB_USER}/user-service", env.BUILD_USER_SERVICE],
                            "notification-service": ["./notification-service", "${DOCKERHUB_USER}/notification-service", env.BUILD_NOTIFICATION_SERVICE],
                            "api-gateway"         : ["./api-gateway", "${DOCKERHUB_USER}/api-gateway", env.BUILD_GATEWAY_SERVICE],
                            "discovery-service"   : ["./discovery-service", "${DOCKERHUB_USER}/discovery-service", env.BUILD_DISCOVERY_SERVICE],
                            "frontend"            : ["./frontend", "${DOCKERHUB_USER}/front", env.BUILD_FRONTEND]
                    ]
                    for (entry in builds) {

                        def name = entry.key
                        def cfg = entry.value

                        def path = cfg[0]
                        def image = cfg[1]
                        def flag = cfg[2]

                        if (flag == 'true') {
                            echo "Building ${name}"

                            sh """
                        docker build \
                          -t ${image}:${IMAGE_TAG} \
                          ${path}
                         """
                        }
                    }
                }
            }
        }


        //=========================================================
        //Docker Login
        //=========================================================
        stage('Docker Login') {
            when {
                expression {
                    env.BUILD_FRONTEND == 'true' ||
                            env.BUILD_CATALOG_SERVICE == 'true' ||
                            env.BUILD_USER_SERVICE == 'true' ||
                            env.BUILD_NOTIFICATION_SERVICE == 'true' ||
                            env.BUILD_GATEWAY_SERVICE == 'true' ||
                            env.BUILD_DISCOVERY_SERVICE == 'true'
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
        // Push images
        // =========================================================
        stage('Push Images') {
            when {
                expression {
                    env.BUILD_FRONTEND == 'true' ||
                            env.BUILD_CATALOG_SERVICE == 'true' ||
                            env.BUILD_USER_SERVICE == 'true' ||
                            env.BUILD_NOTIFICATION_SERVICE == 'true' ||
                            env.BUILD_GATEWAY_SERVICE == 'true' ||
                            env.BUILD_DISCOVERY_SERVICE == 'true'
                }
            }

            steps {
                script {

                    def images = [
                            "catalog-service"     : "${DOCKERHUB_USER}/catalog-service",
                            "user-service"        : "${DOCKERHUB_USER}/user-service",
                            "notification-service": "${DOCKERHUB_USER}/notification-service",
                            "api-gateway"         : "${DOCKERHUB_USER}/api-gateway",
                            "discovery-service"   : "${DOCKERHUB_USER}/discovery-service",
                            "frontend"            : "${DOCKERHUB_USER}/front"
                    ]

                    for (entry in images) {

                        def service = entry.key
                        def image = entry.value

                        def flag = env["BUILD_${service.replace('-', '_').toUpperCase()}"]

                        if (flag == 'true') {
                            echo "Pushing ${service}"

                            sh "docker push ${image}:${IMAGE_TAG}"
                        }
                    }
                }
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
                    script {

                        def services = [
                                "catalog-service",
                                "user-service",
                                "notification-service",
                                "api-gateway",
                                "discovery-service",
                                "frontend"
                        ]
                        def remoteScript = ""
                        remoteScript += """
                    set -e
                    mkdir -p ${SERVER_PATH}
                    cd ${SERVER_PATH}

                    TIMESTAMP=\$(date +%F_%T)
                    echo "== SAVE DEPLOY HISTORY =="
                """
                        for (service in services) {

                            def envName = "DEPLOY_${service.replace('-', '_').toUpperCase()}"

                            remoteScript += """
                        echo "\$TIMESTAMP ${env[envName]}" >> deploy_history_${service}.log
                        echo "${env[envName]}" > current_${service}_tag.tmp
                        mv current_${service}_tag.tmp current_${service}_tag
                    """
                        }

                        remoteScript += """
                    echo "== DONE =="
                """

                        sh """
                    ssh -o StrictHostKeyChecking=no root@${SERVER_IP} '${remoteScript}'
                """
                    }
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
                            file(credentialsId: 'env-catalog', variable: 'CATALOG_ENV'),
                            file(credentialsId: 'env-user', variable: 'USER_ENV'),
                            file(credentialsId: 'env-notification', variable: 'NOTIFICATION_ENV'),
                            file(credentialsId: 'env-postgres', variable: 'POSTGRES_ENV')

                    ]) {

                        for (entry in configs) {

                            def envVar = entry.key
                            def remoteFile = entry.value

                            sh """
                            scp -o StrictHostKeyChecking=no \$${envVar} \
                                root@${SERVER_IP}:${SERVER_PATH}/${remoteFile}
                        """

                            echo "Uploaded ${remoteFile}"
                        }

                        sh """
                        scp -o StrictHostKeyChecking=no \
                            docker-compose.prod.yml \
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
                    script {

                        def pullNeeded = env.BUILD_CATALOG_SERVICE == 'true' ||
                                env.BUILD_USER_SERVICE == 'true' ||
                                env.BUILD_FRONTEND == 'true'

                        def remoteCmd = """
                    set -e
                    cd ${SERVER_PATH}

                    echo "== DEPLOY START =="

                    export PULL_NEEDED=${pullNeeded}
                """

                        deployTags.each { service, tag ->

                            def envName = service.replace('-', '_').toUpperCase() + "_TAG"

                            remoteCmd += """
                        export ${envName}="${tag}"
                    """
                        }

                        remoteCmd += """
                    if [ "\$PULL_NEEDED" = "true" ]; then
                        echo "Pulling images..."
                        docker compose pull
                    else
                        echo "Skipping pull"
                    fi

                    docker compose up -d

                    echo "== DEPLOY DONE =="
                """

                        sh """
                    ssh -o StrictHostKeyChecking=no root@${SERVER_IP} '${remoteCmd}'
                """
                    }
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