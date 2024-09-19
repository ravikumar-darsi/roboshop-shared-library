def call(Map configMap){
pipeline {
    agent {
        node {
            label 'AGENT-1' // Specifies the node with label 'AGENT-1' to run the pipeline on
        }
    }
    environment { 
        packageVersion = '' // Placeholder for the package version extracted from the package.json file
        //nexusURL = '172.31.21.186:8081' // URL for Nexus repository
        //above private ip was used as global variables 
    }
    options {
        timeout(time: 1, unit: 'HOURS') // Set timeout of 1 hour for the entire pipeline
        disableConcurrentBuilds() // Prevents concurrent builds of the same job
    }

    // Uncomment the parameters block if you need to pass parameters
     parameters {
    //     string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')
    //     text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')
           booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
    //     choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')
    //     password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
     }

    stages {
        stage('Get the Version') {
            steps {
                script {
                    // Read the version from the package.json file
                    //def is used to declare variables in groovy script
                    def packageJson = readJSON file: 'package.json'
                    packageVersion = packageJson.version // Set packageVersion to the version in package.json
                    echo "application version: $packageVersion" // Display the version in the console output
                }
            }
        }
        stage('Install dependencies') {
            steps {
              // Install project dependencies using npm
                sh """
                    npm install 
                """
            }
        }
        stage('Unit tests') {
            steps {
              // Placeholder for running unit tests
                sh """
                    echo "unit tests will run here" 
                """
            }
        }
        stage('Sonar Scan'){
            steps{
                sh """
                    sonar-scanner
                """
            }
        }
        stage('Build') {
            steps {
              // List the current files and directories
              // Create a zip file, excluding .git and any .zip files
              // zip -q -r catalogue.zip ./* -x ".git" -x "*.zip"  previously like this changed into below line
                sh """
                    ls -la 
                    zip -q -r ${configMap.component}.zip ./* -x ".git" -x "*.zip"
                    ls -ltr
                """
            }
        }
        stage('Publish Artifact') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3', // Specify Nexus version (Nexus 3 in this case)
                    protocol: 'http', // Use HTTP protocol to connect to Nexus
                    //nexusUrl: "${nexusURL}", // Nexus URL defined in environment variables
                    nexusUrl: pipelineGlobals.nexusURL(),
                    groupId: 'com.roboshop', // Define the groupId for the artifact
                    version: "${packageVersion}", // Use the package version fetched from the package.json
                    //repository: 'catalogue', // Repository name in Nexus
                    repository: "${configMap.component}",
                    credentialsId: 'nexus-auth', // Jenkins credentials ID for Nexus authentication
                    artifacts: [
                       // [artifactId: 'catalogue', // Define artifactId for the artifact
                        [artifactId: "${configMap.component}",
                        classifier: '', // No classifier needed for this artifact
                      //  file: 'catalogue.zip', // File to be uploaded
                        file: "${configMap.component}.zip",
                        type: 'zip'] // Define the type of the file (zip)
                    ]
                )
            }
        }
        stage('Deploy'){
            when {
                expression {
                    params.Deploy == 'true'
                }
            }
            steps {
                script {
                    def params = [
                            string(name: 'version', value: "$packageVersion"),
                            string(name: 'environment', value: "dev")
                        ]
                   // build job: "catalogue-deploy", wait: true, parameters: params  
                      build job: "../${configMap.component}-deploy", wait: true, parameters: params             
                }
            }
        }
    }

    post {
        always {
            echo 'I will always say Hello again!..' // This will always run, regardless of the build result
            deleteDir() // Clean up workspace after the build
        }
        failure {
            echo 'This block of scripts runs only when the pipeline has failed, used generally to send some alerts' // Execute only on failure
        }
        success {
            echo 'I will say Hello when the pipeline is executed successfully' // Execute only on successful execution
        }
    }
}
}