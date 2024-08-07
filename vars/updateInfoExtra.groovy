#!/usr/bin/env groovy

def call() {
    def buildInfo = [:]

    // Время выполнения сборки
    buildInfo.duration = (currentBuild.duration as float) / 1000

    // Использование CPU
    buildInfo.cpuUsage = sh(script: "mpstat | grep 'all' | awk '{print 100-\$13}'", returnStdout: true).trim() as float

    // Потребление памяти
    buildInfo.memoryUsage = (sh(script: "free | grep Mem | awk '{print \$3}'", returnStdout: true).trim() as float) / 1024
    buildInfo.memoryMax = (sh(script: "free | grep Mem | awk '{print \$2}'", returnStdout: true).trim() as float) / 1024
    artifact_name = null
    // Артефакты
    if(artifact_name) {
    archiveArtifacts artifacts: artifact_name, fingerprint: true
    buildInfo.artifactName = artifact_name
    buildInfo.artifactUrl = "${env.BUILD_URL}artifact/${buildInfo.artifactName}"
    buildInfo.artifactSize = sh(script: "ls -lh ${buildInfo.artifactName} | awk '{print \$5}'", returnStdout: true).trim()}
    else{buildInfo.artifactName = null}

    def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim().replaceFirst(/\.git$/, '').replaceFirst(/:\/\/[^@]+@/, '://')
    // Инициирующий коммит
    def commitName = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
    buildInfo.commitName = commitName
    buildInfo.commitUrl = "${repoUrl}/commit/${sh(script: "git rev-parse HEAD", returnStdout: true).trim()}"

    return buildInfo
}