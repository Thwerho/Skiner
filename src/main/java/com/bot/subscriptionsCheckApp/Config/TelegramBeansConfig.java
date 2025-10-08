package com.bot.subscriptionsCheckApp.Config;

import com.bot.subscriptionsCheckApp.Telegram.ContestBot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class TelegramBeansConfig {

    private final BotProperties botProperties;
    private final VkProperties vkProperties;
    private final com.bot.subscriptionsCheckApp.Service.VkService vkService;
    private final com.bot.subscriptionsCheckApp.Service.TgService tgService;
    private final com.bot.subscriptionsCheckApp.Service.ContestService contestService;

    @Bean
    public ContestBot contestBot() {
        return new ContestBot(
                botProperties,
                vkProperties,
                vkService,
                tgService,
                contestService
        );
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(ContestBot contestBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(contestBot);
        return botsApi;
    }
}
