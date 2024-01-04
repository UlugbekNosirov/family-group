package uz.dataFin.notificationbot.utils;

public interface Security {
    String BOT_TOKEN = "5046468356:AAF0NgmaMRa51FClxD8Uw40YS7xapo9EB_8";
//    String BOT_TOKEN = "1095403197:AAE5nZ1-j9q3GvmTGG0XIOSf2QWOKod3n7c";

//    String GLOBAL_URL = "https://datafin2.ssglink.uz/";
    String GLOBAL_URL = "https://dde6-213-230-100-26.ngrok-free.app/";
    String  LOGIN = "Админстратор";
    String  PASSWORD = "2275157";
    String TELEGRAM_URL = "https://api.telegram.org/";

    String MY_URL = "api/telegram/";

    String BOT = "bot";

    String REQUEST_FULL_PATH = TELEGRAM_URL + BOT + BOT_TOKEN;
}
