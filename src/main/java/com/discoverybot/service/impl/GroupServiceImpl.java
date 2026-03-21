package com.discoverybot.service.impl;

import com.discoverybot.model.Group;
import com.discoverybot.model.Role;
import com.discoverybot.model.User;
import com.discoverybot.model.UserGroup;
import com.discoverybot.repository.GroupRepository;
import com.discoverybot.repository.UserGroupRepository;
import com.discoverybot.service.GroupService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GroupServiceImpl implements GroupService {

    GroupRepository groupRepository;
    UserGroupRepository userGroupRepository;

    @Override
    public Group getOrCreate(Chat telegramChat) {
        return groupRepository.findByTelegramGroupId(telegramChat.getId())
                .orElseGet(() -> {
                    Group group = new Group(telegramChat.getId(), telegramChat.getTitle());
                    groupRepository.save(group);
                    log.info("Registered new group: {} ({})", telegramChat.getTitle(), telegramChat.getId());
                    return group;
                });
    }

    @Override
    public void registerUserInGroup(User user, Group group) {
        if (userGroupRepository.findByUserAndGroup(user, group).isPresent()) {
            return;
        }

        boolean isFirstMember = !userGroupRepository.existsByGroup(group);
        Role role = isFirstMember ? Role.ADMIN : Role.MEMBER;

        UserGroup userGroup = new UserGroup(user, group, role);
        userGroupRepository.save(userGroup);
        log.info("Registered user {} in group {} as {}", user.getName(), group.getName(), role);
    }

    @Override
    public Role getUserRole(User user, Group group) {
        return userGroupRepository.findByUserAndGroup(user, group)
                .map(UserGroup::getRole)
                .orElse(Role.MEMBER);
    }
}
