package uz.murodjon.notificationbot.utils;

public interface Security {
    String BOT_TOKEN = "5766804065:AAEwldkTWjTIccmoOZs9FWOM-_06S_sggXQ";
//    String BOT_TOKEN = "1095403197:AAE5nZ1-j9q3GvmTGG0XIOSf2QWOKod3n7c";

    String GLOBAL_URL = "https://notificationbot.ssglink.uz/";
//    String GLOBAL_URL = "https://98d2-95-214-210-98.eu.ngrok.io/";

    String TELEGRAM_URL = "https://api.telegram.org/";

    String MY_URL = "api/telegram/";

    String BOT = "bot";

    String REQUEST_FULL_PATH = TELEGRAM_URL + BOT + BOT_TOKEN;
}
