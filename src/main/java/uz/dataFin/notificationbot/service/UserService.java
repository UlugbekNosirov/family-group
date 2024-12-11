package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.model.dtos.ClientDTO;
import uz.dataFin.notificationbot.model.dtos.UserDTO;
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
    private final ReportRepository reportRepository;
    private final UtilService utilService;

    public UserDTO checkAndGet(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        if ((update.hasMessage() && Objects.isNull(update.getMessage())))
            return new UserDTO(BotState.UPDATE_IS_NULL);
        Optional<Users> optionalUser = userRepository.findByChatId(chatId);

        if (optionalUser.isPresent() && optionalUser.get().getPhone() != null) {
            return new UserDTO(optionalUser.get().getState());
        }

        Optional<Report> report = reportRepository.findByClientId(chatId);

        if (report.isEmpty()) {
            LocalDate currentDate = LocalDate.now();
            currentDate.getMonth();
            LocalDate lastDayOfMonthDate = currentDate.withDayOfMonth(
                    currentDate.getMonth().length(currentDate.isLeapYear()));
            try {
                reportRepository.save(new Report(chatId,
                        "",
                        "",
                        "",
                        "",
                        currentDate.toString(),
                        lastDayOfMonthDate.toString(),
                        "",
                        "",
                        "",
                        "xlsx",
                        0,
                        "",
                        "",
                        1));
            }catch (Exception e){
                System.out.println("\nError: \n\n"+e+"\n\nGet and check da error. Report repository saving...\n\n");
            }
        }
        if (optionalUser.isEmpty()) {
            saveUserByToken(update, 1);
        }
        return new UserDTO(userRepository.findByChatId(chatId).get().getState());
    }

    public String getRoleInURL(String chatId){
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ClientDTO clientDTO = new ClientDTO();
            clientDTO.setClientID(chatId);
            HttpEntity<ClientDTO> entity = new HttpEntity<>(clientDTO, headers);

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
            System.out.println(e+"Role aniqlashda xatolik yuz berdi! userService.getRoleINURL");
        }
        return "Nobody";
    }

    public void saveUserByToken(Update update, Integer data) {
        String chatId = utilService.getChatIdFromUpdate(update);
        Message message = (update.hasMessage())?update.getMessage(): update.getCallbackQuery().getMessage();
        String role = getRoleInURL(chatId);

        userRepository.save(new Users(message.getChatId().toString(),
                message.getFrom().getFirstName(),
                message.getFrom().getLastName(),
                message.getFrom().getUserName(),
                role,
                BotState.SEND_PHONE,
                "kril"));

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

    public List<Users> getAll() {
        return userRepository.findAllByActive();
    }
}
