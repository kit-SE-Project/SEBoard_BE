def component = [
		seboard: false,
]

pipeline {
	agent any
	stages {
		stage("Checkout") {
			steps {
				checkout scm
			}
		}
		stage("Build") {
			steps {
                script {
					component.each{ entry ->
						stage ("${entry.key} Build"){
							if(entry.value){
								var = entry.key
								sh "docker-compose build ${var.toLowerCase()}"
							}	
						}
					}
				}
			}
		}
		stage("Tag and Push") {
			steps {
                script {
					component.each{ entry ->
						stage ("${entry.key} Push"){
							if(entry.value){
								var = entry.key
								withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: 'maanjong-docker-hub',
								usernameVariable: 'DOCKER_USER_ID',
								passwordVariable: 'DOCKER_USER_PASSWORD'
								]]){
								sh "docker tag seboard_pipeline_${var.toLowerCase()}:latest ${DOCKER_USER_ID}/seboard_pipeline_${var.toLowerCase()}:${BUILD_NUMBER}"
								sh "docker login -u ${DOCKER_USER_ID} -p ${DOCKER_USER_PASSWORD}"
								sh "docker push ${DOCKER_USER_ID}/seboard_pipeline_${var.toLowerCase()}:${BUILD_NUMBER}"
								}
							}
						}
					}
				}
			}	
		}
	}
}
