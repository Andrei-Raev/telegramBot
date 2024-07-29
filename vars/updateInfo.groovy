#!/usr/bin/env groovy

def call() {
    def buildInfo = [:]

    // Название проекта
    buildInfo.projectName = env.JOB_NAME

    // Номер сборки
    buildInfo.buildNumber = env.BUILD_NUMBER.toInteger()

    // Ссылка на сборку
    buildInfo.buildUrl = env.BUILD_URL

    // Репозиторий
    def repoUrl = scm.userRemoteConfigs[0].url
    buildInfo.repoUrl = repoUrl
    buildInfo.repoName = repoUrl.tokenize('/').last().replaceFirst(/\.git$/, '')

    // Начало сборки
    buildInfo.buildTimestamp = new Date(currentBuild.startTimeInMillis).format("yyyy-MM-dd HH:mm:ss")

    // Автор изменений
    buildInfo.author = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
    buildInfo.authorUrl = sh(script: "git config user.url", returnStdout: true).trim()

    // Ветка
    buildInfo.branchName = env.BRANCH_NAME

    // Инициирующий коммит
    def commitName = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
    buildInfo.commitName = commitName
    buildInfo.commitUrl = "${repoUrl}/commit/${commitName}"

    return buildInfo
}