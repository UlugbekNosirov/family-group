package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.DateDTO;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.repository.FileRepository;
import uz.dataFin.notificationbot.repository.MarketRepository;
import uz.dataFin.notificationbot.repository.UserRepository;
import uz.dataFin.notificationbot.utils.BotState;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    public final MarketService marketService;
    private final FileRepository fileRepository;
    private final static String words ="abdefghijklmnopqrstuvxyzwcйцукенгшўзхъфқвапролджэячсмитьбюёҳғыщ1234567890.,/*-+_:!?@#$%^&()'\"[]{}|<>'\'№~` ";

    public UserDTO checkAndGet(Update update) {
        Message message = update.hasMessage() ? update.getMessage() : update.hasCallbackQuery() ? update.getCallbackQuery().getMessage() : null;
        if (message == null) return null;
        Optional<Users> optionalUser = userRepository.findByChatId(message.getChatId().toString());
        Optional<DateDTO> dateDTO = fileRepository.findDateDTOByClientId(message.getChatId().toString());
        if (optionalUser.isPresent() && dateDTO.isPresent() && optionalUser.get().getPhone() != null)
            return new UserDTO(optionalUser.get().getState());
        else {
            if (dateDTO.isEmpty()) {
                LocalDate currentDate = LocalDate.now();
                currentDate.getMonth();
                LocalDate lastDayOfMonthDate = currentDate.withDayOfMonth(
                        currentDate.getMonth().length(currentDate.isLeapYear()));
                DateDTO DTO = new DateDTO(update.getMessage().getFrom().getFirstName(), update.getMessage().getChatId().toString(), 1, "REPORT", LocalDate.now().toString(), lastDayOfMonthDate.toString(), "", "xlsx");
                fileRepository.save(DTO);
            }
            if (optionalUser.isEmpty()) {
                saveChosenMarket(message, 1, update.getMessage().getChatId().toString());
            }
        }
        return new UserDTO(BotState.SEND_PHONE);
    }

    public void saveChosenMarket(Message message, Integer data, String chatId) {
        var from = message.getFrom();
        userRepository.save(new Users(message.getChatId().toString(), from.getFirstName(),
                from.getLastName(), from.getUserName(), BotState.SEND_PHONE));
        Market market = marketService.getMarket(new Long(data));
        Users user = userRepository.findByChatId(chatId).get();
        List<Market> marketList = user.getMarketList();
        Long userMarketId = marketRepository.getUserMarketId(user.getId(), market.getId());
        if (userMarketId==null) {
            marketList.add(market);
            user.setMarketList(marketList);
            userRepository.save(user);
        }
    }

    public static boolean containsSpecialCharacters(String text) {
        String lowerCase = text.toLowerCase();
        for (char c : lowerCase.toCharArray()) {
            if (words.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    public String getRole(Long chatId){
        Optional<Users> roleByChatId = userRepository.findRoleByChatId(chatId.toString());
        return roleByChatId.map(Users::getRole).orElse(null);
    }

    public void saveRole(String chatId, String role) {
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);
        if (optionalUser.isPresent()) {
            Users user = optionalUser.get();
            user.setRole(role);
            userRepository.save(user);
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

    public Boolean getPhone(Update update){
        Optional<Users> optionalUser = userRepository.findByChatId(update.getMessage().getChatId().toString());
        if (optionalUser.isPresent()){
            String phone = optionalUser.get().getPhone();
            return phone != null;
        }
        return false;
    }

    public BotState getState(Update update){
        Optional<Users> optionalUser = userRepository.findByChatId(update.getMessage().getChatId().toString());
        if (optionalUser.isPresent()){
            BotState state = optionalUser.get().getState();
            return state;
        }
        return null;
    }

    public void updateUserState(String chatId,BotState state, String contact){
        Users user = userRepository.findByChatId(chatId).get();
        user.setState(state);
        user.setPhone(contact);
        userRepository.save(user);
    }

    public void saveUserName(Update update) {
        Optional<Users> user = userRepository.findByChatId(update.getMessage().getChatId().toString());
        if (user.isPresent()){
            user.get().setFirstname(update.getMessage().getText());
            user.get().setState(BotState.SEND_PHONE);
        }
    }

    public String getName(Update update) {
        Optional<Users> user = userRepository.findByChatId(update.getMessage().getChatId().toString());
        return user.get().getFirstname();
    }
}
