#!/usr/bin/env groovy
package com.telebot


@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
import groovyx.net.http.RESTClient
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


class TelegramBot {
    int chatId
    int messageId
    String token
    String url
    RESTClient client

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
    String branchName = null

    int stepIndex = 0
    def stepTimer = null


    private String renderTemplate() {
        String tmp = messageTemplate.formatted(this.title, this.projectName, this.buildNumber, this.buildNumber, this.buildUrl,
                this.repoName, this.repoUrl, this.buildTimestamp, this.buildStatus, this.steps.collect { it.render() }.join("\n"))
        if (extraInfoReady) {
            tmp += messageTemplateExtra.formatted(this.author, this.authorUrl, this.branchName, this.commitName, this.commitUrl, this.duration.round(2),
                    this.cpuUsage, this.memoryUsage, this.artifactName, this.artifactSize, this.artifactUrl)
        }
        return tmp
    }

    public void updateInfo(def bi) {
        // Название проекта
        this.projectName = bi.projectName

        // Номер сборки
        this.buildNumber = bi.buildNumber

        // Ссылка на сборку
        this.buildUrl = bi.buildUrl

        // Репозиторий
        this.repoUrl = bi.repoUrl
        this.repoName = bi.repoName

        // Начало сборки
        this.buildTimestamp = bi.buildTimestamp

        // Автор изменений
        this.author = bi.author
        this.authorUrl = bi.authorUrl

        // Ветка
        this.branchName = bi.branchName

        // Инициирующий коммит
        this.commitName = bi.commitName
        this.commitUrl = bi.commitUrl
    }

    public void updateInfoExtra() {

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
        this.url = "https://api.telegram.org/bot${this.token}/"
        this.client = new RESTClient(this.url)
        this.messageId = sendMessage(renderTemplate())
        
        editMessage("asdfdasfdas")
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

    // Отправка сообщения
    private int sendMessage(String message) {
        def params = [
            chat_id: this.chatId, 
            text: message, 
            parse_mode: 'Markdown'
        ]
        
        def response = this.client.post(
                path: 'sendMessage',
                body: params,
                requestContentType: 'application/json'
        )

        if (response.status == 200 && response.data.ok) {
            return response.data.result.message_id
        } else {
            throw new RuntimeException("Failed to send message: ${response.data}")
        }
    }


    // Редактирование сообщения
    private void editMessage(String message) {
        def params = [
            chat_id: this.chatId,
            message_id: this.messageId,
            text: message, 
            parse_mode: 'Markdown'
        ]
        
        def response = this.client.post(
                path: 'editMessageText',
                body: params,
                requestContentType: 'application/json'
        )

        if (response.status == 200 && response.data.ok) {
            return response.data.result.message_id
        } else {
            throw new RuntimeException("Failed to send message: ${response.data}")
        }
    }
}
