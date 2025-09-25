pipeline {
    agent {
        label 'DS agent'
    }

    environment {
        MVN_SETTINGS = '/etc/m2/settings.xml' //This should be changed in Jenkins config for the DS agent
        PROJECT = 'ds-discover'
        BUILD_TO_TRIGGER = 'ds-image'
    }

    triggers {
        // This triggers the pipeline when a PR is opened or updated or so I hope
        githubPush()
    }

    parameters {
        string(name: 'ORIGINAL_BRANCH', defaultValue: "${env.BRANCH_NAME}", description: 'Branch of first job to run, will also be PI_ID for a PR')
        string(name: 'ORIGINAL_JOB', defaultValue: "${env.PROJECT}", description: 'What job was the first to build?')
        string(name: 'TARGET_BRANCH', defaultValue: "${env.CHANGE_TARGET}", description: 'Target branch if PR')
        string(name: 'SOURCE_BRANCH', defaultValue: "${env.CHANGE_BRANCH}", description: 'Source branch if PR')
    }

    stages {
        stage('Echo Environment Variables') {
            steps {
                echo "ORIGINAL_BRANCH: ${env.ORIGINAL_BRANCH}"
                echo "PROJECT: ${env.PROJECT}"
                echo "ORIGINAL_JOB: ${env.ORIGINAL_JOB}"
                echo "BUILD_TO_TRIGGER: ${env.BUILD_TO_TRIGGER}"
                echo "TARGET_BRANCH: ${env.TARGET_BRANCH}"
                echo "SOURCE_BRANCH: ${env.SOURCE_BRANCH}"
            }
        }

        stage('Change version if part of PR') {
            when {
                expression {
                    env.ORIGINAL_BRANCH ==~ "PR-[0-9]+"
                }
            }
            steps {
                script {
                    sh "mvn -s ${env.MVN_SETTINGS} versions:set -DnewVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.PROJECT}-SNAPSHOT"
                    echo "Changing MVN version to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.PROJECT}-SNAPSHOT"
                }
            }
        }

        stage('Change dependencies') {
            when {
                expression {
                    env.ORIGINAL_BRANCH ==~ "PR-[0-9]+"
                }
            }
            steps {
                script {

                    if ( env.ORIGINAL_JOB == 'ds-storage' || env.ORIGINAL_JOB == 'ds-license' ){
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.license:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-license-SNAPSHOT -DforceVersion=true"
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.present:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-present-SNAPSHOT -DforceVersion=true"

                        echo "Changing MVN dependency license to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-license-SNAPSHOT"
                        echo "Changing MVN dependency present to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-present-SNAPSHOT"
                    }
                    if ( env.ORIGINAL_JOB == 'ds-present' ){
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.present:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.ORIGINAL_JOB}-SNAPSHOT -DforceVersion=true"
                        echo "Changing MVN dependency present to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.ORIGINAL_JOB}-SNAPSHOT"
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    // Execute Maven build
                    sh "mvn -s ${env.MVN_SETTINGS} clean package"
                }
            }
        }

        stage('Push to Nexus') {
            when {
                // Check if Build was successful
                expression {
                    currentBuild.currentResult == "SUCCESS" && env.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+|PR-[0-9]+"
                }
            }
            steps {
                sh "mvn -s ${env.MVN_SETTINGS} clean deploy -DskipTests=true"
            }
        }

        stage('Trigger Image Build') {
            when {
                expression {
                    currentBuild.currentResult == "SUCCESS" && env.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+|PR-[0-9]+"
                }
            }
            steps {
                script {
                    if ( env.ORIGINAL_BRANCH ==~ "PR-[0-9]+" ) {
                        def empty_if_no_branch = sh(script: "git ls-remote --heads git@github.com:kb-dk/${env.BUILD_TO_TRIGGER}.git | grep 'refs/heads/${env.SOURCE_BRANCH}'", returnStdout:true).trim()
                        echo "Test String: ${empty_if_no_branch}"
                        echo "Triggering: DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.TARGET_BRANCH}"

                        def result = build job: "DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.TARGET_BRANCH}",
                        parameters: [
                            string(name: 'ORIGINAL_BRANCH', value: env.ORIGINAL_BRANCH),
                            string(name: 'ORIGINAL_JOB', value: env.ORIGINAL_JOB),
                            string(name: 'TARGET_BRANCH', value: env.TARGET_BRANCH)
                        ]
                        wait: true // Wait for the pipeline to finish
                    }

                    else if ( env.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+" ){
                        echo "Triggering: DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.ORIGINAL_BRANCH}"

                        def result = build job: "DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.ORIGINAL_BRANCH}"
                        wait: true // Wait for the pipeline to finish
                    }
                }
            }
        }
    }
}