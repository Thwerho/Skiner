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
        log.info("Subscriptions check...");

        try
        {
            List<ContestUser> users = repo.findAll();

            for(ContestUser user : users)
            {
                log.info("User check: {}", user.getTelegramId());


                checkTgSubscriptions(user);

                checkVkSubscriptions(user);

            }

        } catch (Exception e) {
            log.error("Error while checking subscriptions: \n", e);
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

                    log.info("Tg API: user {} deleted, not subscriber to telegram: {} .\n",
                            user.getTelegramId(), channel.getName_id());
                }
                else
                {
                    log.info("Tg API: check is completed.");
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Error while checking subscriptions of user {}: {}", user.getTelegramId(), e.getMessage());
        }
    }

    private void checkVkSubscriptions(ContestUser user)
    {
        for (GroupConfig group : vkProps.getGroups()) // проходимся по всем группам из application.yaml
        {

            if (vkService.areMembers(group, user.getVk_id()))
            {
                contestService.deleteParticipant(user);
                log.info("VK API: user {} deleted, not subscribed to vk: {}.\n", user.getVk_id(), group.getName());
            }
            else
            {
                log.info("VK API: check is completed.");
            }
        }

    }

}