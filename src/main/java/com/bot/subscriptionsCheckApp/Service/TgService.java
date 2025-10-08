package com.bot.subscriptionsCheckApp.Service;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TgService extends DefaultAbsSender
{
    private final BotProperties properties;

    public TgService(BotProperties properties) {
        super(new DefaultBotOptions());
        this.properties = properties;
    }

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    public boolean isMember(Long telegramUserId, String channelId) {
        try {
            GetChatMember req = new GetChatMember(channelId, telegramUserId);
            ChatMember member = execute(req);

            return (member instanceof ChatMemberOwner) ||
                    (member instanceof ChatMemberAdministrator) ||
                    (member instanceof ChatMemberMember);
        } catch (TelegramApiException e) {
            System.out.println(e);
            return false;
        }
    }
}
