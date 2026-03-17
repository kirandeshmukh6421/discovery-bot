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

/**
 * Handles group identity resolution and user-group membership.
 *
 * Groups are auto-registered the first time the bot receives a message in them.
 * Membership (user_groups) is created on first contact per user per group.
 * The first person to message in a group receives ADMIN role; everyone else gets MEMBER.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GroupServiceImpl implements GroupService {

    GroupRepository groupRepository;
    UserGroupRepository userGroupRepository;

    /**
     * Returns the existing group for the given Telegram chat, or creates one if first contact.
     * Works for both group chats and private DM chats (Telegram gives each a unique chat ID).
     *
     * @param telegramChat The Telegram Chat object from the incoming update
     * @return Persisted Group entity
     */
    @Override
    public Group getOrCreate(Chat telegramChat) {
        // Lookup by Telegram's numeric chat ID (negative for groups, positive for DMs)
        return groupRepository.findByTelegramGroupId(telegramChat.getId())
                .orElseGet(() -> {
                    Group group = new Group(telegramChat.getId(), telegramChat.getTitle());
                    groupRepository.save(group);
                    log.info("Registered new group: {} ({})", telegramChat.getTitle(), telegramChat.getId());
                    return group;
                });
    }

    /**
     * Ensures a user-group membership record exists for the given user and group.
     * Safe to call on every message — exits early if membership already exists.
     *
     * Role assignment:
     * - First person to message in a group → ADMIN
     * - All subsequent users → MEMBER
     *
     * @param user  The resolved User entity
     * @param group The resolved Group entity
     */
    @Override
    public void registerUserInGroup(User user, Group group) {
        // Already a member — nothing to do
        if (userGroupRepository.findByUserAndGroup(user, group).isPresent()) {
            return;
        }

        // If no members exist yet for this group, this user is the first — make them ADMIN
        boolean isFirstMember = !userGroupRepository.existsByGroup(group);
        Role role = isFirstMember ? Role.ADMIN : Role.MEMBER;

        UserGroup userGroup = new UserGroup(user, group, role);
        userGroupRepository.save(userGroup);
        log.info("Registered user {} in group {} as {}", user.getName(), group.getName(), role);
    }

    /**
     * Returns the role of a user in a specific group.
     * Falls back to MEMBER if no membership record is found (defensive default).
     *
     * @param user  The resolved User entity
     * @param group The resolved Group entity
     * @return ADMIN or MEMBER
     */
    @Override
    public Role getUserRole(User user, Group group) {
        return userGroupRepository.findByUserAndGroup(user, group)
                .map(UserGroup::getRole)
                .orElse(Role.MEMBER);
    }
}
