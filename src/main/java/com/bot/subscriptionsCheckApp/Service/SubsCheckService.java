package com.bot.subscriptionsCheckApp.Service;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import com.bot.subscriptionsCheckApp.DTO.ChannelConfig;
import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import com.bot.subscriptionsCheckApp.Models.ContestUser;
import com.bot.subscriptionsCheckApp.Repos.ContestUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class SubsCheckService
{
    private final ContestService contestService;
    private final ContestUserRepository repo;
    private final TgService tgService;
    private final VkService vkService;
    private final BotProperties props;
    private final VkProperties vkProps;


    @Scheduled(fixedRate = 60000)
    public void handleGroupLeave()
    {
        log.info("Проверка подписок в бд...");

        try
        {
            List<ContestUser> users = repo.findAll();

            for(ContestUser user : users)
            {
                log.info("Проверка пользователя: {}", user.getTelegramId());


                checkTgSubscriptions(user);

                checkVkSubscriptions(user);

            }

        } catch (Exception e) {
            log.error("Ошибка при выполнении проверки подписок: \n", e);
        }

        System.out.println("_____________________________" +
                "____________________________________________________________________");

    }


    private void checkTgSubscriptions(ContestUser user)
    {
        try
        {
            for (ChannelConfig channel : props.getTelegramChannels())
            {
                if (!tgService.isMember(user.getTelegramId(), channel.getId())) // если пользователь не состоит в канале
                {
                    contestService.deleteParticipant(user); // удаление из бд

                    log.info("Tg API: пользователь {} удален, отписан от telegram: {} .\n",
                            user.getTelegramId(), channel.getName_id());
                }
            }
            log.info("Tg API: проверка пройдена успешно.");
        }
        catch (Exception e)
        {
            log.warn("Ошибка при проверке tg для пользователя {}: {}", user.getTelegramId(), e.getMessage());
        }
    }

    private void checkVkSubscriptions(ContestUser user)
    {
        for (GroupConfig group : vkProps.getGroups()) // проходимся по всем группам из application.yaml
        {

            if (vkService.areMembers(group, user.getVk_id()))
            {
                contestService.deleteParticipant(user);
                log.info("VK API: Пользователь {} удален, отписан от vk: {}.\n", user.getVk_id(), group.getName());
            }
        }

        log.info("VK API: проверка пройдена успешно.");
    }

}