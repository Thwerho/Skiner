package com.bot.subscriptionsCheckApp.Config;

import com.bot.subscriptionsCheckApp.Service.ContestService;
import com.bot.subscriptionsCheckApp.Service.JoinRequestService;
import com.bot.subscriptionsCheckApp.Service.TgService;
import com.bot.subscriptionsCheckApp.Service.VkService;
import com.bot.subscriptionsCheckApp.Telegram.ContestJoinBot;
import com.bot.subscriptionsCheckApp.listener.JoinRequestListener;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
@RequiredArgsConstructor
public class TelegramBeansConfig {

    private final VkProperties vkProperties;
    private final VkService vkService;
    private final TgService tgService;
    private final ContestService contestService;
    private final JoinRequestListener joinRequestListener;
    private final JoinRequestService joinRequestService;
    private final BotProperties botProperties;

    @Bean
    public SetWebhook setWebhookInstance()
    {
        return SetWebhook.builder().url(botProperties.getWebhookUrl()).build();
    }

    @Bean
    public ContestJoinBot contestJoinBot(SetWebhook setWebhook) throws TelegramApiException {
        ContestJoinBot contestJoinBot = new ContestJoinBot(
                botProperties,
                vkProperties,
                vkService,
                tgService,
                contestService,
                joinRequestListener,
                joinRequestService
        );
        contestJoinBot.setWebhook(setWebhook);
        return contestJoinBot;
    }
}
