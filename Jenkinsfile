pipeline {
    agent any
    
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
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build and Test Microservices') {
            parallel {
                stage('Authentication Service') {
                    steps {
                        dir('authentication') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                        
                        post {
                            success {
                                echo 'Authentication service build and tests successful'
                                junit 'authentication/target/surefire-reports/*.xml'
                            }
                            failure {
                                echo 'Authentication service build or tests failed'
                            }
                        }
                    }
                }
                
                stage('Config Server') {
                    steps {
                        dir('config-server') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                        
                        post {
                            success {
                                echo 'Config Server build and tests successful'
                                junit 'config-server/target/surefire-reports/*.xml'
                            }
                            failure {
                                echo 'Config Server build or tests failed'
                            }
                        }
                    }
                }
                
                stage('Discovery Service') {
                    steps {
                        dir('discovery') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                        
                        post {
                            success {
                                echo 'Discovery Service build and tests successful'
                                junit 'discovery/target/surefire-reports/*.xml'
                            }
                            failure {
                                echo 'Discovery Service build or tests failed'
                            }
                        }
                    }
                }
                
                stage('Gateway Service') {
                    steps {
                        dir('gateway') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                        
                        post {
                            success {
                                echo 'Gateway Service build and tests successful'
                                junit 'gateway/target/surefire-reports/*.xml'
                            }
                            failure {
                                echo 'Gateway Service build or tests failed'
                            }
                        }
                    }
                }
                
                stage('Notification Service') {
                    steps {
                        dir('notification') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                        
                        post {
                            success {
                                echo 'Notification Service build and tests successful'
                                junit 'notification/target/surefire-reports/*.xml'
                            }
                            failure {
                                echo 'Notification Service build or tests failed'
                            }
                        }
                    }
                }
                
                stage('Tickets Service') {
                    steps {
                        dir('tickets') {
                            sh 'mvn clean package -DskipTests'
                            sh 'mvn test'
                        }
                        
                        post {
                            success {
                                echo 'Tickets Service build and tests successful'
                                junit 'tickets/target/surefire-reports/*.xml'
                            }
                            failure {
                                echo 'Tickets Service build or tests failed'
                            }
                        }
                    }
                }
            }
        }
        
        stage('Code Quality Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Publish to Nexus') {
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
            steps {
                script {
                    def microservices = ['authentication', 'config-server', 'discovery', 'gateway', 'notification', 'tickets']
                    
                    for (service in microservices) {
                        dir(service) {
                            sh "docker build -t windlogs/${service}:${BUILD_NUMBER} ."
                        }
                    }
                }
            }
        }
        
        stage('Push Docker Images') {
            steps {
                script {
                    withCredentials([string(credentialsId: DOCKER_CREDENTIAL_ID, variable: 'DOCKER_PASSWORD')]) {
                        sh 'echo $DOCKER_PASSWORD | docker login -u windlogs --password-stdin'
                        
                        def microservices = ['authentication', 'config-server', 'discovery', 'gateway', 'notification', 'tickets']
                        
                        for (service in microservices) {
                            sh "docker push windlogs/${service}:${BUILD_NUMBER}"
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                sh 'docker-compose down || true'
                sh "BUILD_NUMBER=${BUILD_NUMBER} docker-compose up -d"
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
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
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
            emailext (
                subject: "Pipeline Success: ${currentBuild.fullDisplayName}",
                body: "The pipeline has completed successfully.",
                to: 'team@windlogs.com'
            )
        }
        failure {
            echo 'Pipeline failed!'
            emailext (
                subject: "Pipeline Failure: ${currentBuild.fullDisplayName}",
                body: "The pipeline has failed. Please check the logs for details.",
                to: 'team@windlogs.com'
            )
        }
    }
} 