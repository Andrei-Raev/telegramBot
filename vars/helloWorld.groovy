package vars

import org.telebot.TelegramBot

def call(Map config = [:]) {
    TelegramBot telegramBot = new TelegramBot(env.TELEGRAM_CHAT_ID, env.TELEGRAM_TOKEN)

    sh "echo Hello ${config.name}. Today is ${config.dayOfWeek}."
}
