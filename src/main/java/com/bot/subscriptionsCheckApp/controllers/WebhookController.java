package com.bot.subscriptionsCheckApp.controllers;

import com.bot.subscriptionsCheckApp.Telegram.ContestJoinBot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
public class WebhookController
{

    private final ContestJoinBot contestJoinBot;

    @PostMapping("webhook")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update)
    {
        return contestJoinBot.onWebhookUpdateReceived(update);
    }
}
