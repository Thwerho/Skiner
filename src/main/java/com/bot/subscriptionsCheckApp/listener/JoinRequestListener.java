package com.bot.subscriptionsCheckApp.listener;

import com.bot.subscriptionsCheckApp.Service.JoinRequestService;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.springframework.stereotype.Component;


//ловим обновление и передаем его JoinRequestService.java
@Component
@RequiredArgsConstructor
public class JoinRequestListener
{
    private final JoinRequestService joinRequestService; // внедрение сервиса обработчика

    public void onUpdate(Update update)
    {
        if (update.hasChatJoinRequest())
        {
            joinRequestService.handleJoinRequest(update);
        }
    }
}
