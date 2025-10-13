package com.bot.subscriptionsCheckApp.Service;

import com.bot.subscriptionsCheckApp.Telegram.ContestJoinBot;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ApproveChatJoinRequest;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class JoinRequestService
{
    private TelegramLongPollingBot bot; // внедрение TelegramLongPollingBot -> ContestJoinBot
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void setBot(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void handleJoinRequest(Update update)
    {
        var req = update.getChatJoinRequest();
        var chatId = req.getChat().getId();
        var user = req.getUser();

        System.out.printf("Новая заявка от @%s — одобрю через 30 секунд...%n",
                user.getUserName());

        // Планируем задачу через 30 секунд
        scheduler.schedule(() -> {
            try {
                bot.execute(new ApproveChatJoinRequest(chatId.toString(), user.getId()));
                System.out.printf("Заявка от @%s одобрена%n", user.getUserName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 30, TimeUnit.SECONDS);
    }
}
