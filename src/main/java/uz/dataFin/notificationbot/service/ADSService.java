package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.Advertising;
import uz.dataFin.notificationbot.repository.AdvertisingRepository;
import uz.dataFin.notificationbot.repository.UserRepository;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ADSService {
    private final AdvertisingRepository advertisingRepository;
    private final UserRepository userRepository;
    private final BotService botService;

    public void saveAdsById(String chatId, String caption, String filedId, Boolean isPhoto){
        Optional<Advertising> advertising = advertisingRepository.findByClientID(chatId);
        if (advertising.isPresent()){
            advertising.get().setFilePath(filedId);
            advertising.get().setCaption(caption);
            advertising.get().setIsPhoto(isPhoto);
            advertisingRepository.save(advertising.get());
        }else {
            Advertising advertising1 = Advertising.builder()
                    .clientID(chatId)
                    .caption(caption)
                    .isPhoto(isPhoto)
                    .filePath(filedId).build();
            advertisingRepository.save(advertising1);
        }
    }

    public Optional<Advertising> findByClientID(String chatId) {
        return advertisingRepository.findByClientID(chatId);
    }
}
