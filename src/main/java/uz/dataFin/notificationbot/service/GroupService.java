package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.Groups;
import uz.dataFin.notificationbot.model.Report;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.repository.GroupRepository;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final UtilService utilService;
    private final FeignService feignService;
    private final GroupRepository groupRepository;


    public Map<Boolean, Groups> saveNewGroup(Update update) {
        String chatIdFromUpdate = utilService.getChatIdFromUpdate(update);
        String groupId = update.getMyChatMember().getChat().getId().toString();
        Optional<Groups> group = groupRepository.findByGroupId(groupId);
        if (group.isPresent()) {
            Groups groups = group.get();
            groups.setGroupId(groupId);
            groups.setGroupName(update.getMyChatMember().getChat().getTitle());
            groups.setUserName(update.getMyChatMember().getChat().getUserName());
            groups.setStatus(update.getMyChatMember().getNewChatMember().getStatus());
            groups.setInvitationLink(update.getMyChatMember().getChat().getInviteLink());
            groups.setAddedId(chatIdFromUpdate);
            groupRepository.save(groups);
            Map<Boolean, Groups> savedGroups = new HashMap<>();
            savedGroups.put(true, groups);
            return savedGroups;
        }else {
            Groups groups = Groups.builder()
                    .groupId(groupId)
                    .groupName(update.getMyChatMember().getChat().getTitle())
                    .userName(update.getMyChatMember().getChat().getUserName())
                    .addedId(chatIdFromUpdate)
                    .status(update.getMyChatMember().getNewChatMember().getStatus())
                    .invitationLink(update.getMyChatMember().getChat().getInviteLink()).build();
            groupRepository.save(groups);
            Map<Boolean, Groups> savedGroups = new HashMap<>();
            savedGroups.put(true, groups);
            return savedGroups;
        }
    }

    public void sendInfoGroup(Map<Boolean, Groups> groupsMap, Update update) {
        if (!groupsMap.isEmpty()){
            feignService.sendInfoGroup(groupsMap, update);
        }
    }

    public List<Groups> getAllGroups() {
        return groupRepository.findAll();
    }
}

