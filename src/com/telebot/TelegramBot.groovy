#!/usr/bin/env groovy
package com.telebot

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


class TelegramBot {
    int chatId
    int messageId
    String token

    String messageTemplate = '''%s

🛠 *Название проекта:* `%s`
🔢 Номер сборки: #%d
🔗 Ссылка на сборку: [Jenkins Build #%d](%s)
📂 Репозиторий: [%s](%s)

🕒 Начало сборки: %s
📊 Статус сборки: %s

🔧 *Шаги сборки:*
%s'''
    String messageTemplateExtra = '''

----------------------------
👤 *Автор изменений:* [%s](%s)
🌿 *Ветка:* %s
📜 *Инициирующий коммит:* [%s](%s)

📈 **Метрики:**
- Время выполнения сборки: `%s секунд`
- Использование CPU: `%s%`
- Потребление памяти: `%s`

📦 **Артефакты:**
- Главный артефакт: [%s (%s)](%s)
'''

    String title = "🚀 *Начата сборка!*"
    String projectName = null
    int buildNumber = 0
    String buildUrl = null
    String repoUrl = null
    String repoName = null
    String buildTimestamp = null
    String buildStatus = "🔄 Выполняется"
    List<Step> steps = []

    boolean extraInfoReady = false
    String author = null
    String authorUrl = null
    String commitName = null
    String commitUrl = null
    float duration = 0
    int cpuUsage = 0
    int memoryUsage = 0
    String artifactName = null
    String artifactSize = null
    String artifactUrl = null

    int stepIndex = 0
    def stepTimer = null


    private String renderTemplate() {
        String tmp = messageTemplate.formatted(this.title, this.projectName, this.buildNumber, this.buildNumber, this.buildUrl,
                this.repoName, this.repoUrl, this.buildTimestamp, this.buildStatus, this.steps.collect { it.render() }.join("\n"))
        if (extraInfoReady) {
            tmp += messageTemplateExtra.formatted(this.author, this.authorUrl, this.commitName, this.commitUrl, this.duration.round(2),
                    this.cpuUsage, this.memoryUsage, this.artifactName, this.artifactSize, this.artifactUrl)
        }
        return tmp
    }

    void updateInfo() {
        // Название проекта
        this.projectName = env.JOB_NAME

        // Номер сборки
        this.buildNumber = env.BUILD_NUMBER.toInteger()

        // Ссылка на сборку
        this.buildUrl = env.BUILD_URL

        // Репозиторий
        this.repoUrl = scm.userRemoteConfigs[0].url
        this.repoName = repoUrl.tokenize('/').last().replaceFirst(/\.git$/, '')

        // Начало сборки
        this.buildTimestamp = new Date(currentBuild.startTimeInMillis).format("yyyy-MM-dd HH:mm:ss")

        // Автор изменений
        this.author = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
        this.authorUrl = sh(script: "git config user.url", returnStdout: true).trim()

        // Ветка
        this.branchName = env.BRANCH_NAME

        // Инициирующий коммит
        this.commitName = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        this.commitUrl = "${repoUrl}/commit/${commitName}"
    }

    void updateInfoExtra() {

        // Время выполнения сборки
        this.duration = currentBuild.duration as float

        // Использование CPU
        this.cpuUsage = sh(script: "mpstat | grep 'all' | awk '{print 100-\$13}'", returnStdout: true).trim().toInteger()

        // Потребление памяти
        this.memoryUsage = sh(script: "free | grep Mem | awk '{print \$3/\$2 * 100.0}'", returnStdout: true).trim().toInteger()

        // Артефакты
        this.artifactName = 'artifact.jar' // Замените на свое имя артефакта
        this.artifactUrl = "${env.BUILD_URL}artifact/${artifactName}"
        this.artifactSize = sh(script: "ls -lh ${artifactName} | awk '{print \$5}'", returnStdout: true).trim()

    }

    void addStep(String step) {
        this.steps.add(new Step(name: step))
    }


    TelegramBot(String chatId, String token) {
        this.chatId = chatId.toInteger()
        this.token = token
    }

    void init() {
        this.messageId = sendMessage(renderTemplate())

        updateInfo()
    }

    void begin() {
        this.steps[stepIndex].status += 1
        editMessage(renderTemplate())
        this.stepTimer = new Date().time / 1000
    }

    void end() {
        this.steps[stepIndex].status += 1
        def tmp_t = new Date().time / 1000
        this.steps[stepIndex].duration = (tmp_t - stepTimer) / 2
        editMessage(renderTemplate())

        this.stepIndex += 1
    }

    void success() {
        for (int i = 0; i < this.steps.size(); i++) {
            this.steps[i].status = 2
        }

        this.buildStatus = "✅ Успех"


        this.extraInfoReady = true
        updateInfoExtra()

        editMessage(renderTemplate())
    }

    void fail() {
        for (int i = stepIndex; i < this.steps.size(); i++) {
            this.steps[i].status = -1
        }

        this.buildStatus = "❌ Ошибка"


        this.extraInfoReady = true
        updateInfoExtra()

        editMessage(renderTemplate())
    }

    private int sendMessage(String message) {
        String url = "https://api.telegram.org/bot${this.token}/sendMessage"

        def params = new JsonBuilder([
                chat_id   : this.chatId,
                text      : message,
                parse_mode: 'Markdown'
        ]).toString()

        def responseContent = sendPostRequest(url, params)

        def jsonResponse = new JsonSlurper().parseText(responseContent)
        return jsonResponse.result.message_id
    }

    // Редактирование сообщения
    private void editMessage(String message) {
        String url = "https://api.telegram.org/bot${this.token}/editMessageText"

        def params = new JsonBuilder([
                chat_id   : this.chatId,
                message_id: this.messageId,
                text      : message,
                parse_mode: 'Markdown'
        ]).toString()

        sendPostRequest(url, params)
    }

    // Метод отправки POST-запроса
    // Метод отправки POST-запроса
    private static String sendPostRequest(String urlString, String params) {
        URL url = new URL(urlString)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection()
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setDoOutput(true)

        try (OutputStream output = connection.getOutputStream(); Writer writer = new OutputStreamWriter(output, "UTF-8")) {
            writer.write(params)
        }

        try (InputStream input = connection.getInputStream(); Reader reader = new InputStreamReader(input, "UTF-8")) {
            return reader.text
        }
    }
}
