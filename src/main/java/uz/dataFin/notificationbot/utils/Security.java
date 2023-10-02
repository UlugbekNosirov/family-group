package uz.dataFin.notificationbot.utils;

public interface Security {
    String BOT_TOKEN = "5996854486:AAHKZepV7funiJ90Vja6AoR6HNitzWnfZbo";
//    String BOT_TOKEN = "1095403197:AAE5nZ1-j9q3GvmTGG0XIOSf2QWOKod3n7c";

    String GLOBAL_URL = "https://testbot.ssglink.uz/";
//    String GLOBAL_URL = "https://81a7-213-230-118-143.ngrok-free.app/";

    String TELEGRAM_URL = "https://api.telegram.org/";

    String MY_URL = "api/telegram/";

    String BOT = "bot";

    String REQUEST_FULL_PATH = TELEGRAM_URL + BOT + BOT_TOKEN;
}
