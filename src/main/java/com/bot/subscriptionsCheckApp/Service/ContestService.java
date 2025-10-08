package com.bot.subscriptionsCheckApp.Service;

import com.bot.subscriptionsCheckApp.Models.ContestUser;
import com.bot.subscriptionsCheckApp.Repos.ContestUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContestService {
    private final ContestUserRepository repo;

    public boolean addParticipant(Long tgId, String username, String vkId) {
        if (repo.findByTelegramId(tgId).isPresent()) {
            return false; // уже участвует
        }
        ContestUser user = ContestUser.builder()
                .telegramId(tgId)
                .telegramUsername(username)
                .vk_id(vkId) // теперь строка
                .time_joined(LocalDateTime.now())
                .build();
        repo.save(user);
        return true;
    }
}
