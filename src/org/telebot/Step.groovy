package org.telebot

class Step {
    String name
    int status = 0
    float duration = 0

    private List<String> renderStatus() {
        switch (this.status) {
            case -1: // error
                return ["❌", "Ошибка"]
            case 0: // waiting
                return ["🕒", "Ожидание"]
            case 1: // not started
                return ["⏳", "Выполняется"]
            case 2: // success
                return ["✅", "Успех"]
            default:
                return ["❓", "Неизвестно"]
        }
    }


    String render() {
        def status = this.renderStatus()
        String duration = this.duration == 0 ? "" : " (${this.duration.round(2)} сек)"

        return "${status[0]} | ${this.name} - ${status[1]}" + duration
    }
}
