package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.repository.*;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    public final MarketService marketService;
    private final ReportRepository reportRepository;
    private final FileRepository fileRepository;
    private final UtilService utilService;
    private final SearchRepository searchRepository;

    public UserDTO checkAndGet(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);
        Optional<DateDTO> dateDTO = fileRepository.findDateDTOByClientId(chatId);
        if (optionalUser.isPresent() && dateDTO.isPresent() && optionalUser.get().getPhone() != null) {
            return new UserDTO(optionalUser.get().getState());
        }
        if (dateDTO.isEmpty()) {
            LocalDate currentDate = LocalDate.now();
            currentDate.getMonth();
            LocalDate lastDayOfMonthDate = currentDate.withDayOfMonth(
                    currentDate.getMonth().length(currentDate.isLeapYear()));
            DateDTO DTO = new DateDTO(update.getMessage().getFrom().getFirstName(), update.getMessage().getChatId().toString(), chatId, 1, "REPORT", LocalDate.now().toString(), lastDayOfMonthDate.toString(), "", "pdf");
            fileRepository.save(DTO);
            reportRepository.save(new ReportDTO(chatId, "","", "","","","", "","", "xlsx", 0, "",""));
        }
        if (optionalUser.isEmpty()) {
            saveChosenMarket(update, 1);
            searchRepository.save(SearchDTO.builder().search("").chatId(chatId).branchID("").page(1).pageSize(10).build());
        }
        return new UserDTO(userRepository.findByChatId(chatId).get().getState());
    }

    public String getRoleInURL(String chatId){
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            Client client = new Client();
            client.setClientID(chatId);
            HttpEntity<Client> entity = new HttpEntity<>(client, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/check", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                String text = new String(responseBody, StandardCharsets.UTF_8);
                saveRole(chatId, text);
                return text;
            }
        }catch (Exception e){
            System.out.println("Role aniqlashda xatolik yuz berdi! userService.getRoleINURL");
        }
        return "Nobody";
    }

    public void saveChosenMarket(Update update, Integer data) {
        String chatId = utilService.getChatIdFromUpdate(update);
        Message message = (update.hasMessage())?update.getMessage(): update.getCallbackQuery().getMessage();
        String role = getRoleInURL(chatId);
        Long alkan = 1L;
        try {
            alkan = marketRepository.getMarketIdByUsername("ALKAN");
            if (Objects.isNull(alkan)) marketRepository.save(new Market("ALKAN GROUP", "ALKAN", "ALKAN"));
        }catch (Exception e){
            System.out.println("MARKET create error");
        }
        userRepository.save(new Users(message.getChatId().toString(), message.getFrom().getFirstName(),
                message.getFrom().getLastName(), message.getFrom().getUserName(), role, BotState.SEND_PHONE, "kril"));
        Market market = marketService.getMarket(alkan);
        Optional<Users> usersOptional = userRepository.findByChatId(chatId);
        if (usersOptional.isPresent()){
            Users user = usersOptional.get();
            List<Market> marketList = user.getMarketList();
            Long userMarketId = marketRepository.getUserMarketId(user.getId(), market.getId());
            if (userMarketId==null) {
                marketList.add(market);
                user.setMarketList(marketList);
                userRepository.save(user);
            }
        }
    }


    public String getRole(String chatId){
        Optional<Users> roleByChatId = userRepository.findRoleByChatId(chatId);
        if (roleByChatId.isPresent()){
            return roleByChatId.get().getRole();
        }
        saveRole(chatId, "Nobody");
        return "Nobody";
    }

    public void saveRole(String chatId, String role) {
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);
        if (optionalUser.isPresent()) {
            Users user = optionalUser.get();
            user.setRole(role);
            userRepository.save(user);
        }
    }

    public void saveLanguage(String chatId, String language) {
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);
        if (optionalUser.isPresent()) {
            Users user = optionalUser.get();
            user.setLanguage(language);
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

    public Boolean getPhone(String chatId){
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);
        if (optionalUser.isPresent()){
            String phone = optionalUser.get().getPhone();
            return Objects.nonNull(phone);
        }
        return false;
    }

    public void updateUserState(String chatId,BotState state){
        Optional<Users> user = userRepository.findByChatId(chatId);
        if (user.isPresent()) {
            user.get().setState(state);
            userRepository.save(user.get());
        }
    }

    public void saveUserName(Message message, String chatId) {
        Optional<Users> user = userRepository.findByChatId(chatId);
        if (user.isPresent()){
            user.get().setFirstname(message.getText());
            userRepository.save(user.get());
        }
    }

    public String getName(String chatId) {
        Optional<Users> user = userRepository.findByChatId(chatId);
        if (user.isPresent())
            return user.get().getFirstname();
        return "No name!";
    }

    public BotState getState(String chatId) {
        Optional<Users> user = userRepository.findByChatId(chatId);
        return user.map(Users::getState).orElse(null);
    }

    public boolean isRegistered(String chatId) {
        if (!getPhone(chatId)){
            return false;
        }
        if (utilService.containsSpecialCharacters(getName(chatId))) {
            return false;
        }
        return true;
    }

    public BotState startCondition(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        Optional<Users> user = userRepository.findByChatId(chatId);
        if (user.isPresent()){
            if (getState(chatId) == BotState.SEND_NAME)
                saveUserName(update.getMessage(), chatId);
            if (utilService.containsSpecialCharacters(getName(chatId)))
                return BotState.SEND_NAME;
            if (!getPhone(chatId))
                return BotState.GET_CONTACT;
            return BotState.SUCCESS;
        }else {
            saveChosenMarket(update, 1);
            startCondition(update);
        }
        return BotState.SUCCESS;
    }

    public void saveContact(Message message, String chatId) {
        Optional<Users> user = userRepository.findByChatId(chatId);
        if (user.isPresent()) {
            try {
                user.get().setPhone(message.getContact().getPhoneNumber());
                userRepository.save(user.get());
            }catch (Exception e){
                System.out.println("\n\nMessage may be null\n\n");
            }
        }
    }

}
