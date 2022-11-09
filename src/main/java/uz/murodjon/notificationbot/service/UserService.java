package uz.murodjon.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.murodjon.notificationbot.model.Market;
import uz.murodjon.notificationbot.model.Users;
import uz.murodjon.notificationbot.payload.UserDTO;
import uz.murodjon.notificationbot.repository.MarketRepository;
import uz.murodjon.notificationbot.repository.UserRepository;
import uz.murodjon.notificationbot.utils.BotState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    public UserDTO checkAndGet(Update update) {
        Message message = update.hasMessage() ? update.getMessage() : update.hasCallbackQuery() ? update.getCallbackQuery().getMessage() : null;
        if (message == null) return null;
        Optional<Users> optionalUser = userRepository.findByChatId(message.getChatId().toString());
        if (optionalUser.isPresent())
            return new UserDTO(optionalUser.get().getState());
        else {
            var from = message.getFrom();
            userRepository.save(new Users(message.getChatId().toString(),from.getFirstName(),
                    from.getLastName(), from.getUserName(), BotState.CHOOSE_MARKET));
            return new UserDTO(BotState.CHOOSE_MARKET);
        }

    }

    public void saveData(String chatId, UserDTO userDTO) {
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);
        if (optionalUser.isPresent()) {
            Users user = optionalUser.get();
            user.setState(userDTO.getState());
            userRepository.save(user);
        }
    }

    public Users getByChatId(String chatId) {
        return userRepository.findByChatId(chatId).orElse(null);
    }

    public void saveMarket(Market market, String chatId){
        Users user = userRepository.findByChatId(chatId).get();
        List<Market> marketList = user.getMarketList();
        Long userMarketId = marketRepository.getUserMarketId(user.getId(), market.getId());
        if (userMarketId==null) {
            marketList.add(market);
            user.setMarketList(marketList);
            userRepository.save(user);
        }
    }
    public void updateUserState(String chatId,BotState state){
        Users user = userRepository.findByChatId(chatId).get();
        user.setState(state);
        userRepository.save(user);
    }

    public void saveUser(Users user){
        userRepository.save(user);
    }

}
