package com.bot.subscriptionsCheckApp.Service;

import com.bot.subscriptionsCheckApp.Models.ContestUser;
import com.bot.subscriptionsCheckApp.Repos.ContestUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestService {
    private final ContestUserRepository repo;

    public boolean findByTgId(Long tgId)
    {
        if (repo.findByTelegramId(tgId).isPresent())
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    public boolean addParticipant(Long tgId, String username, String vkId) {
        if (repo.findByTelegramId(tgId).isPresent()) {
            return false; // уже участвует
        }
        ContestUser user = ContestUser.builder()
                .telegramId(tgId)
                .telegramUsername(username)
                .vk_id(vkId) // теперь строка
                .isParticipates(true)
                .time_joined(LocalDateTime.now())
                .build();
        repo.save(user);
        return true;
    }

    public boolean addUser(Long tgId, String username, String vkId)
    {
        try {
            if (repo.findByTelegramId(tgId).isPresent()) // если пользователь уже есть - добавляем новый вк или меняем старый
            {
                ContestUser user = repo.findByTelegramId(tgId).get();
                user.setVk_id(vkId);
                repo.save(user);
            }
            else // если пользователя еще нет (никогда не добавлял вк), то сохраняем его как нового пользователя
            {
                ContestUser user = ContestUser.builder()
                        .telegramId(tgId)
                        .telegramUsername(username)
                        .vk_id(vkId)
                        .time_joined(LocalDateTime.now()) // обязательно для нового пользователя
                        .isParticipates(false)
                        .build();
                repo.save(user);
            }

            return true;
        }
        catch (DataIntegrityViolationException e)
        {
            log.error(e.getMessage());
            return false;
        }
    }

    public void deleteParticipantById(Long tgId, String username, String vkId) {
            ContestUser user = ContestUser.builder()
                    .telegramId(tgId)
                    .telegramUsername(username)
                    .vk_id(vkId)
                    .time_joined(LocalDateTime.now())
                    .build();
            repo.deleteById(user.getId());

        }

    public void deleteParticipant(ContestUser user)
    {
        user.setParticipates(false);
        repo.save(user);
    }

}
