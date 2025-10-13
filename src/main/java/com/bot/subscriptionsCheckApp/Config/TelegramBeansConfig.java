package com.bot.subscriptionsCheckApp.Config;

import com.bot.subscriptionsCheckApp.Service.ContestService;
import com.bot.subscriptionsCheckApp.Service.JoinRequestService;
import com.bot.subscriptionsCheckApp.Service.TgService;
import com.bot.subscriptionsCheckApp.Service.VkService;
import com.bot.subscriptionsCheckApp.Telegram.ContestJoinBot;
import com.bot.subscriptionsCheckApp.listener.JoinRequestListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class TelegramBeansConfig {

    private final ContestJoinBot contestJoinBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        return botsApi;
    }
}
