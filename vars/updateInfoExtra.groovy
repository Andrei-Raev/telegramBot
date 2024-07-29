#!/usr/bin/env groovy

def call() {
    def buildInfo = [:]

    // Время выполнения сборки
    buildInfo.duration = currentBuild.duration as float

    // Использование CPU
    buildInfo.cpuUsage = sh(script: "mpstat | grep 'all' | awk '{print 100-\$13}'", returnStdout: true).trim() as float

    // Потребление памяти
    buildInfo.memoryUsage = sh(script: "free | grep Mem | awk '{print \$3/\$2 * 100.0}'", returnStdout: true).trim() as float

    // Артефакты
    archiveArtifacts artifacts: 'app.tar.gz', fingerprint: true
    buildInfo.artifactName = 'app.tar.gz'
    buildInfo.artifactUrl = "${env.BUILD_URL}artifact/${buildInfo.artifactName}"
    buildInfo.artifactSize = sh(script: "ls -lh ${buildInfo.artifactName} | awk '{print \$5}'", returnStdout: true).trim()

    return buildInfo
}