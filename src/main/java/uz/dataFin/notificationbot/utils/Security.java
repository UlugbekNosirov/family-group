package uz.dataFin.notificationbot.utils;

public interface Security {
    String BOT_TOKEN = "6980716361:AAEa8Qva80Qb8-213TJCK4NI0LXONoQN6Vw";
//    String BOT_TOKEN = "1095403197:AAE5nZ1-j9q3GvmTGG0XIOSf2QWOKod3n7c";

    String GLOBAL_URL = "https://datafin5.ssglink.uz/";
//        String GLOBAL_URL = "https://1a37-84-54-84-153.ngrok-free.app/";
    String  LOGIN = "Админстратор";
    String  PASSWORD = "2275157";
    String TELEGRAM_URL = "https://api.telegram.org/";

    String MY_URL = "api/telegram/";

    String BOT = "bot";

    String REQUEST_FULL_PATH = TELEGRAM_URL + BOT + BOT_TOKEN;
}
