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

                    echo "PREVIOUS COMMIT: ${previousCommit}"
                    echo "CURRENT COMMIT : ${env.GIT_COMMIT}"

                    def changes = previousCommit
                            ? sh(script: "git diff --name-only ${previousCommit} ${env.GIT_COMMIT}", returnStdout: true).trim()
                            : sh(script: "git ls-files", returnStdout: true).trim()

                    echo "Changed files:\n${changes}"

                    def changedFiles = changes?.trim()
                            ? changes.split("\n").collect { it.trim() }
                            : []

                    def changed = { path ->
                        changedFiles.any { it.startsWith(path) }
                    }

                    def services = [
                            "catalog-service"     : "BUILD_CATALOG_SERVICE",
                            "user-service"        : "BUILD_USER_SERVICE",
                            "notification-service": "BUILD_NOTIFICATION_SERVICE",
                            "api-gateway"         : "BUILD_API_GATEWAY",
                            "discovery-service"   : "BUILD_DISCOVERY_SERVICE",
                            "frontend"            : "BUILD_FRONTEND"
                    ]
                    for (String path : services.keySet()) {

                        String envName = services[path]

                        env[envName] = changed(path).toString()

                        echo "${envName} = ${env[envName]}"
                    }
                    env.UPLOAD_CONFIG = changed("docker-compose.prod.yml").toString()

                    echo "BUILD_CATALOG_SERVICE = ${env.BUILD_CATALOG_SERVICE}"
                    echo "BUILD_USER_SERVICE = ${env.BUILD_USER_SERVICE}"
                    echo "BUILD_NOTIFICATION_SERVICE = ${env.BUILD_NOTIFICATION_SERVICE}"
                    echo "BUILD_API_GATEWAY = ${env.BUILD_API_GATEWAY}"
                    echo "BUILD_DISCOVERY_SERVICE = ${env.BUILD_DISCOVERY_SERVICE}"
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
        //=========================================================
        //Docker Login
        //=========================================================
        stage('Docker Login') {
            when {
                branch 'develop'
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
        // Resolve deploy tags
        // =========================================================
        stage('Resolve Deploy Tags') {

            when {
                branch 'develop'
            }

            steps {
                sshagent(['server-ssh']) {
                    script {

                        def services = [
                                "catalog-service"     : [tagFile: "current_catalog_tag", image: BACKEND_IMAGE],
                                "user-service"        : [tagFile: "current_user_tag", image: USER_IMAGE],
                                "notification-service": [tagFile: "current_notification_tag", image: NOTIFICATION_IMAGE],
                                "api-gateway"         : [tagFile: "current_gateway_tag", image: GATEWAY_IMAGE],
                                "discovery-service"   : [tagFile: "current_discovery_tag", image: DISCOVERY_IMAGE],
                                "frontend"            : [tagFile: "current_frontend_tag", image: FRONTEND_IMAGE]
                        ]

                        for (String service : services.keySet()) {


                            String tagFile = services[service]["tagFile"]
                            String image = services[service]["image"]

                            def serviceName = service.replace('-', '_').toUpperCase()

                            def serverTag = sh(
                                    script: """
                                ssh -o StrictHostKeyChecking=no root@${SERVER_IP} \
                                "cat ${SERVER_PATH}/${tagFile} 2>/dev/null || true"
                            """,
                                    returnStdout: true
                            ).trim()

                            def buildFlag = env["BUILD_${serviceName}"] == "true"

                            if (!serverTag) {

                                echo "No saved tag for ${service}. Rebuild required."

                                env["BUILD_${serviceName}"] = "true"
                                env.UPLOAD_CONFIG = "true"

                                buildFlag = true
                            }

                            if (!buildFlag && serverTag) {

                                def exists = sh(
                                        script: """
                                    docker manifest inspect ${image}:${serverTag} >/dev/null 2>&1
                                """,
                                        returnStatus: true
                                )

                                if (exists != 0) {

                                    echo "${image}:${serverTag} not found in Docker Hub. Rebuild required."

                                    env["BUILD_${serviceName}"] = "true"

                                    buildFlag = true
                                }
                            }

                            def deployTag = buildFlag
                                    ? env.IMAGE_TAG
                                    : serverTag

                            env["DEPLOY_${serviceName}"] = deployTag

                            echo "BUILD_${serviceName} = ${env["BUILD_${serviceName}"]}"
                            echo "DEPLOY_${serviceName} = ${deployTag}"
                            echo "UPLOAD_CONFIG = ${env.UPLOAD_CONFIG}"
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
                            env.BUILD_API_GATEWAY == 'true' ||
                            env.BUILD_DISCOVERY_SERVICE == 'true'
                }
            }

            steps {
                script {
                    def builds = [
                            "catalog-service"     : [
                                    path : "./catalog-service",
                                    image: "${DOCKERHUB_USER}/catalog-service",
                                    build: env.BUILD_CATALOG_SERVICE
                            ],
                            "user-service"        : [
                                    path : "./user-service",
                                    image: "${DOCKERHUB_USER}/user-service",
                                    build: env.BUILD_USER_SERVICE
                            ],
                            "notification-service": [
                                    path : "./notification-service",
                                    image: "${DOCKERHUB_USER}/notification-service",
                                    build: env.BUILD_NOTIFICATION_SERVICE
                            ],
                            "api-gateway"         : [
                                    path : "./api-gateway",
                                    image: "${DOCKERHUB_USER}/api-gateway",
                                    build: env.BUILD_API_GATEWAY
                            ],
                            "discovery-service"   : [
                                    path : "./discovery-service",
                                    image: "${DOCKERHUB_USER}/discovery-service",
                                    build: env.BUILD_DISCOVERY_SERVICE
                            ],
                            "frontend"            : [
                                    path : "./frontend",
                                    image: "${DOCKERHUB_USER}/front",
                                    build: env.BUILD_FRONTEND
                            ]
                    ]
                    def mvnHome = tool 'MAVEN_3'

                    for (String name : builds.keySet()) {

                        def path = builds[name].path
                        def image = builds[name].image
                        def build = builds[name].build

                        if (build == 'true') {

                            if (name != "frontend") {

                                echo "Packaging ${name}"

                                dir(path) {
                                    sh "${mvnHome}/bin/mvn clean package -DskipTests"
                                    sh "find target -name '*.jar'"
                                }
                            }

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
                            env.BUILD_API_GATEWAY == 'true' ||
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

                    for (String service : images.keySet()) {

                        String image = images[service]

                        String serviceName =
                                service.replace('-', '_').toUpperCase()

                        if (env["BUILD_${serviceName}"] == 'true') {

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
                                "catalog-service"     : "current_catalog_tag",
                                "user-service"        : "current_user_tag",
                                "notification-service": "current_notification_tag",
                                "api-gateway"         : "current_gateway_tag",
                                "discovery-service"   : "current_discovery_tag",
                                "frontend"            : "current_frontend_tag"
                        ]

                        def remoteScript = """
                    set -e
                    mkdir -p ${SERVER_PATH}
                    cd ${SERVER_PATH}

                    TIMESTAMP=\$(date +%F_%T)

                    echo "== SAVE DEPLOY HISTORY =="
                """

                        for (String service : services.keySet()) {

                            String tagFile = services[service]

                            String serviceName =
                                    service.replace('-', '_').toUpperCase()

                            String tag =
                                    env["DEPLOY_${serviceName}"]

                            remoteScript += """
                            echo "\$TIMESTAMP ${tag}" >> deploy_history_${service}.log
                            echo "${tag}" > ${tagFile}.tmp
                            mv ${tagFile}.tmp ${tagFile}
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
                            file(credentialsId: 'env-postgres', variable: 'POSTGRES_ENV'),
                            file(credentialsId: 'env-kafka', variable: 'KAFKA_ENV'),
                            file(credentialsId: 'env-keycloak', variable: 'KEYCLOAK_ENV')
                    ]) {

                        script {

                            def configs = [
                                    "MINIO_ENV"       : ".env.minio",
                                    "MONGO_ENV"       : ".env.mongodb",
                                    "CATALOG_ENV"     : ".env.catalog",
                                    "USER_ENV"        : ".env.user",
                                    "NOTIFICATION_ENV": ".env.notification",
                                    "POSTGRES_ENV"    : ".env.postgres",
                                    "KAFKA_ENV"       : ".env.kafka",
                                    "KEYCLOAK_ENV"    : ".env.keycloak"
                            ]

                            for (String envVar : configs.keySet()) {

                                def remoteFile = configs[envVar]

                                sh """
                                  scp -o StrictHostKeyChecking=no \$${envVar} \
                                      root@${SERVER_IP}:${SERVER_PATH}/${remoteFile}
                              """

                                echo "Uploaded ${remoteFile}"
                            }

                            sh """
                        scp -o StrictHostKeyChecking=no \
                            docker-compose.prod.yml \
                            root@${SERVER_IP}:${SERVER_PATH}/docker-compose.yml
                    """
                        }
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

                        def pullNeeded = [
                                env.BUILD_CATALOG_SERVICE,
                                env.BUILD_USER_SERVICE,
                                env.BUILD_NOTIFICATION_SERVICE,
                                env.BUILD_API_GATEWAY,
                                env.BUILD_DISCOVERY_SERVICE,
                                env.BUILD_FRONTEND
                        ].any { it == 'true' }


                        def remoteCmd = """
                    set -e

                    cd ${SERVER_PATH}

                    echo "== DEPLOY START =="

                    export PULL_NEEDED=${pullNeeded}

                    echo "PULL_NEEDED=\$PULL_NEEDED"
                """


                        def services = [
                                "catalog-service",
                                "user-service",
                                "notification-service",
                                "api-gateway",
                                "discovery-service",
                                "frontend"
                        ]


                        for (service in services) {

                            def serviceName = service.replace('-', '_').toUpperCase()

                            def envName = "DEPLOY_${serviceName}"

                            def composeName = "${serviceName}_TAG"

                            remoteCmd += """
                    export ${composeName}="${env[envName]}"
                    """
                        }


                        remoteCmd += """

                    echo "Checking nginx config..."

                    if [ ! -f "./nginx/nginx.prod.conf" ]; then
                        echo "ERROR: nginx/nginx.prod.conf is missing"
                        exit 1
                    fi

                    if [ -d "./nginx/nginx.prod.conf" ]; then
                        echo "ERROR: nginx/nginx.prod.conf is a directory"
                        exit 1
                    fi

                    echo "nginx config OK"


                    echo "Docker compose services:"
                    docker compose config --services


                    if [ "\$PULL_NEEDED" = "true" ]; then
                        echo "Pulling images..."
                        docker compose pull
                    else
                        echo "Skipping pull"
                    fi


                    echo "Starting containers..."

                    docker compose up -d


                    echo "Container status:"
                    docker compose ps


                    echo "== DEPLOY DONE =="
                """


                        echo "Remote deploy command:"
                        echo remoteCmd


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
            echo "Catalog tag: ${env.DEPLOY_CATALOG_SERVICE}"
            echo "User tag: ${env.DEPLOY_USER_SERVICE}"
            echo "Notification tag: ${env.DEPLOY_NOTIFICATION_SERVICE}"
            echo "Gateway tag: ${env.DEPLOY_API_GATEWAY}"
            echo "Discovery tag: ${env.DEPLOY_DISCOVERY_SERVICE}"
            echo "Frontend tag: ${env.DEPLOY_FRONTEND}"
        }
    }
}