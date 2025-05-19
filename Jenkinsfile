pipeline {
    agent any
    
    options {
        // Add build discarder to free up disk space
        buildDiscarder(logRotator(numToKeepStr: '5'))
        // Skip default checkout to do it manually with more control
        skipDefaultCheckout()
        // Don't run concurrent builds of the same branch
        disableConcurrentBuilds()
    }
    
    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }
    
    environment {
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "http"
        NEXUS_URL = "192.168.1.192:8081"
        NEXUS_REPOSITORY = "maven-releases"
        NEXUS_CREDENTIAL_ID = "nexus-credentials"
        DOCKER_CREDENTIAL_ID = "docker-credentials"
        SONAR_CREDENTIAL_ID = "sonar-credentials"
        // Add Maven options for better performance
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
        // Flag to track build success
        BUILD_SUCCESS = 'false'
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Clean before checkout for a fresh build
                cleanWs()
                checkout scm
                // Cache Maven dependencies
                sh 'mkdir -p ~/.m2'
            }
        }
        
        stage('Build and Test Microservices') {
            parallel {
                stage('Authentication Service') {
                    steps {
                        dir('authentication') {
                            // Use -T for parallel threads in Maven
                            sh 'mvn -T 1C clean package -DskipTests'
                            sh 'mvn -T 1C test'
                        }
                    }
                    post {
                        success {
                            echo 'Authentication service build and tests successful'
                            junit 'authentication/target/surefire-reports/*.xml'
                        }
                        failure {
                            echo 'Authentication service build or tests failed'
                            script {
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
                
                stage('Config Server') {
                    steps {
                        dir('config-server') {
                            sh 'mvn -T 1C clean package -DskipTests'
                            sh 'mvn -T 1C test'
                        }
                    }
                    post {
                        success {
                            echo 'Config Server build and tests successful'
                            junit 'config-server/target/surefire-reports/*.xml'
                        }
                        failure {
                            echo 'Config Server build or tests failed'
                            script {
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
                
                stage('Discovery Service') {
                    steps {
                        dir('discovery') {
                            sh 'mvn -T 1C clean package -DskipTests'
                            sh 'mvn -T 1C test'
                        }
                    }
                    post {
                        success {
                            echo 'Discovery Service build and tests successful'
                            junit 'discovery/target/surefire-reports/*.xml'
                        }
                        failure {
                            echo 'Discovery Service build or tests failed'
                            script {
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
                
                stage('Gateway Service') {
                    steps {
                        dir('gateway') {
                            sh 'mvn -T 1C clean package -DskipTests'
                            sh 'mvn -T 1C test'
                        }
                    }
                    post {
                        success {
                            echo 'Gateway Service build and tests successful'
                            junit 'gateway/target/surefire-reports/*.xml'
                        }
                        failure {
                            echo 'Gateway Service build or tests failed'
                            script {
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
                
                stage('Notification Service') {
                    steps {
                        dir('notification') {
                            sh 'mvn -T 1C clean package -DskipTests'
                            sh 'mvn -T 1C test'
                        }
                    }
                    post {
                        success {
                            echo 'Notification Service build and tests successful'
                            junit 'notification/target/surefire-reports/*.xml'
                        }
                        failure {
                            echo 'Notification Service build or tests failed'
                            script {
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
                
                stage('Tickets Service') {
                    steps {
                        dir('tickets') {
                            sh 'mvn -T 1C clean package -DskipTests'
                            sh 'mvn -T 1C test'
                        }
                    }
                    post {
                        success {
                            echo 'Tickets Service build and tests successful'
                            junit 'tickets/target/surefire-reports/*.xml'
                        }
                        failure {
                            echo 'Tickets Service build or tests failed'
                            script {
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
            }
        }
        
        // Verification stage to ensure all builds completed successfully
        stage('Verify Build Success') {
            steps {
                script {
                    if (currentBuild.result == 'FAILURE') {
                        error "One or more microservices failed to build. Aborting pipeline."
                    } else {
                        echo "All microservices built successfully. Proceeding with next stages."
                        env.BUILD_SUCCESS = 'true'
                    }
                }
            }
        }
        
        stage('Code Quality Analysis') {
            when {
                allOf {
                    expression { env.BUILD_SUCCESS == 'true' }
                    expression { return env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop' }
                }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=windlogs -Dsonar.projectName="Windlogs"'
                }
            }
        }
        
        stage('Quality Gate') {
            when {
                allOf {
                    expression { env.BUILD_SUCCESS == 'true' }
                    expression { return env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop' }
                }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Publish to Nexus') {
            when {
                allOf {
                    expression { env.BUILD_SUCCESS == 'true' }
                    expression { return env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop' }
                }
            }
            steps {
                script {
                    def microservices = ['authentication', 'config-server', 'discovery', 'gateway', 'notification', 'tickets']
                    
                    for (service in microservices) {
                        dir(service) {
                            sh "mvn deploy -DskipTests"
                        }
                    }
                }
            }
        }
        
        stage('Build Docker Images') {
            when {
                expression { env.BUILD_SUCCESS == 'true' }
            }
            steps {
                script {
                    def microservices = ['authentication', 'config-server', 'discovery', 'gateway', 'notification', 'tickets']
                    
                    // Build images in parallel
                    def parallelStages = [:]
                    
                    for (service in microservices) {
                        def serviceName = service
                        parallelStages["Build ${serviceName} image"] = {
                            dir(serviceName) {
                                // Use Docker buildkit for faster builds
                                sh "DOCKER_BUILDKIT=1 docker build --build-arg BUILDKIT_INLINE_CACHE=1 -t windlogs/${serviceName}:${BUILD_NUMBER} ."
                            }
                        }
                    }
                    
                    parallel parallelStages
                }
            }
        }
        
        stage('Push Docker Images') {
            when {
                allOf {
                    expression { env.BUILD_SUCCESS == 'true' }
                    expression { return env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop' }
                }
            }
            steps {
                script {
                    withCredentials([string(credentialsId: DOCKER_CREDENTIAL_ID, variable: 'DOCKER_PASSWORD')]) {
                        sh 'echo $DOCKER_PASSWORD | docker login -u windlogs --password-stdin'
                        
                        def microservices = ['authentication', 'config-server', 'discovery', 'gateway', 'notification', 'tickets']
                        
                        // Push images in parallel
                        def parallelStages = [:]
                        
                        for (service in microservices) {
                            def serviceName = service
                            parallelStages["Push ${serviceName} image"] = {
                                sh "docker push windlogs/${serviceName}:${BUILD_NUMBER}"
                            }
                        }
                        
                        parallel parallelStages
                    }
                }
            }
        }
        
        stage('Deploy to Development') {
            when {
                allOf {
                    expression { env.BUILD_SUCCESS == 'true' }
                    branch 'develop'
                }
            }
            steps {
                sh 'docker-compose down || true'
                sh "BUILD_NUMBER=${BUILD_NUMBER} docker-compose up -d"
            }
        }
        
        stage('Deploy to Production') {
            when {
                allOf {
                    expression { env.BUILD_SUCCESS == 'true' }
                    branch 'main'
                }
            }
            steps {
                input message: 'Approve deployment to production?', ok: 'Deploy'
                sh 'docker-compose -f docker-compose.prod.yml down || true'
                sh "BUILD_NUMBER=${BUILD_NUMBER} docker-compose -f docker-compose.prod.yml up -d"
            }
        }
    }
    
    post {
        always {
            // Archive artifacts for debugging
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            // Clean workspace but keep the Maven cache
            sh 'find . -type d -name target -exec rm -rf {} +|| true'
        }
        success {
            echo 'Pipeline completed successfully!'
            emailext (
                subject: "Pipeline Success: ${currentBuild.fullDisplayName}",
                body: "The pipeline has completed successfully.",
                to: 'zbedichaima@gmail.com'
            )
        }
        failure {
            echo 'Pipeline failed!'
            emailext (
                subject: "Pipeline Failure: ${currentBuild.fullDisplayName}",
                body: "The pipeline has failed. Please check the logs for details.",
                to: 'zbedichaima@gmail.com'
            )
        }
    }
} 