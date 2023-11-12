package uz.dataFin.notificationbot.utils;

public interface Security {
    String BOT_TOKEN = "6055845521:AAEg0ArIHuOo8TNsytdEDgfTK-s4Q5s4VfI";
//    String BOT_TOKEN = "1095403197:AAE5nZ1-j9q3GvmTGG0XIOSf2QWOKod3n7c";

    String GLOBAL_URL = "https://benazir.ssglink.uz/";
//    String GLOBAL_URL = "https://6407-213-230-100-246.ngrok-free.app/";

    String TELEGRAM_URL = "https://api.telegram.org/";

    String MY_URL = "api/telegram/";

    String BOT = "bot";

    String REQUEST_FULL_PATH = TELEGRAM_URL + BOT + BOT_TOKEN;
}
