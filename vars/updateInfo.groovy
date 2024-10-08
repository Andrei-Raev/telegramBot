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
    def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim().replaceFirst(/\.git$/, '').replaceFirst(/:\/\/[^@]+@/, '://')
    buildInfo.repoUrl = repoUrl
    buildInfo.repoName = repoUrl.tokenize('/').last()

    // Начало сборки
    buildInfo.buildTimestamp = new Date(currentBuild.startTimeInMillis).format("yyyy-MM-dd HH:mm:ss")

    // Автор изменений
    buildInfo.author = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
    buildInfo.authorUrl = "None"// sh(script: "git config user.url", returnStdout: true).trim()

    // Ветка
    buildInfo.branchName = sh(script: "git branch --show-current", returnStdout: true).trim()

    return buildInfo
}