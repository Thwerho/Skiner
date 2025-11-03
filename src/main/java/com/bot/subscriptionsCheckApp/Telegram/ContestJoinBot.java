package com.bot.subscriptionsCheckApp.Telegram;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import com.bot.subscriptionsCheckApp.DTO.ChannelConfig;
import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import com.bot.subscriptionsCheckApp.Service.*;
import com.bot.subscriptionsCheckApp.listener.JoinRequestListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContestJoinBot extends TelegramLongPollingBot
{
    private final BotProperties props;
    private final VkProperties vkProps;
    private final VkService vkService;
    private final TgService tgService;
    private final ContestService contestService;

    private final JoinRequestListener joinRequestListener; // внедрение обработчика
    private final JoinRequestService joinRequestService; // внедрение сервиса обработчика



    @PostConstruct
    public void init()
    {
        joinRequestService.setBot(this);
    }

    @Override
    public void onUpdateReceived(Update update)
    {

        joinRequestListener.onUpdate(update); //включаем обработку заявок
        StringBuilder sb = new StringBuilder();

        // обработка сообщений в бота
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();
            String text = update.getMessage().getText();
            String vkId = "";


            if ((text.matches("/start") || text.isEmpty() || text.matches(".*")) &&
                    !(text.matches("/media") || text.matches("/check_subscriptions")
                    ||text.matches("/add_vk") || text.matches(".*vk.com/.*") || text.matches("@.*")))
            {
                reply(chatId, "Привет! Для взаимодействия с ботом используйте кнопки в меню.\n");
            }


            else if (text.matches("/media"))
            {
                sb.append( "\uD83D\uDCCC Социальные сети Федерации хоккея г.о. Электросталь\n\n" +
                        "✔\uFE0F<b>Telegram</b>-канал: ");
                for(ChannelConfig channel : props.getTelegramChannels())
                {
                    sb.append("<b><i><a href=\"https://t.me/+9VBfvYszQRoxNDMy").append("\">")
                            .append(channel.getName()).append("</a></i></b>\n\n");
                }

                sb.append("✔\uFE0FСообщество в <b>VK</b>: ");
                for(GroupConfig group : vkProps.getGroups())
                {
                    sb.append("<b><i><a href=\"https://vk.com/")
                            .append(group.getId()).append("\">")
                            .append(group.getName()).append("</a></i></b>\n");
                }

                reply(chatId, sb.toString());
            }

            else if (text.matches("/add_vk"))
            {
                reply(chatId, "Для привязки VK, оставьте ссылку в сообщении ниже по одному из примеров:\n\n" +
                        "1\uFE0F⃣: https://vk.com/юзернейм\n\n" +
                        "2\uFE0F⃣: vk.com/юзернейм\n\n" +
                        "3\uFE0F⃣:@юзернейм");
            }

            else if (text.matches(".*vk.com/.*") || text.matches("@.*")) {
                vkId = parseVkId(text);
                sendButton(chatId, "Чтобы привязать свой VK нажмите на кнопку ниже:", "Привязать VK", "addUser_" + vkId);
            }

            if (text.matches("/check_subscriptions"))
            {
                if(contestService.findByTgId(userId)) {
                    if (contestService.isVkFilled(userId)) {
                        sendButton(chatId, "Чтобы проверить подписки, нажмите кнопку ниже.",
                                "Проверить подписки", "_checkSubscriptions");
                    }
                    else
                    {
                        reply(chatId, "Вы еще не привязали VK. Для привязки нажмите кнопку Привязать VK в меню.");
                    }
                }

            }
        }

        // обработка включения пользователя в БД
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Long userId = update.getCallbackQuery().getFrom().getId();

            String username;
            if(update.getCallbackQuery().getFrom().getUserName() == (null))
            {
                username = "null";
            }
            else
            {
                username = update.getCallbackQuery().getFrom().getUserName();
            }


            if (data.startsWith("join_")) {
                String vkId = data.substring(5); // теперь строка, а не Long.parseLong
                boolean ok = contestService.addParticipant(userId, username, vkId);
                reply(chatId, ok ? "Ты участвуешь в конкурсе 🎉" : "Ты уже участвуешь!");
            }

            if (data.startsWith("addUser_"))
            {
                String vkId = data.substring(8);
                System.out.println(vkId);

                if(checkSubscriptionsBool(userId, contestService.repo.findByTelegramId(userId).get().getVk_id()))
                {
                    boolean ok = contestService.addUser(userId, username, vkId);
                    reply(chatId, ok ? "Страница в VK успешно привязана \uD83E\uDD73" : "Ошибка, обратитесь к @mollen44 в Telegram, чтобы привязать страницу.");
                }
                else
                {
                    checkSubscriptions(chatId, userId, contestService.repo.findByTelegramId(userId).get().getVk_id());
                }
            }

            if (data.startsWith("_checkSubscriptions"))
            {
                checkSubscriptions(chatId, userId, contestService.repo.findByTelegramId(userId).get().getVk_id());
            }


        }
    }


    /**
     * Проверяет подписки VK и Telegram
     */
    private void checkSubscriptions(Long chatId, Long userId, String vkId) {
        List<GroupConfig> missingVk = new ArrayList<>();
        List<ChannelConfig> missingTg = new ArrayList<>();

        // Проверка VK групп
        for (GroupConfig group : vkProps.getGroups()) {
            if (!vkService.areMembers(group, vkId)) {
                missingVk.add(group);
            }
        }

        // Проверка Telegram каналов
        for (ChannelConfig channel : props.getTelegramChannels()) {
            if (!tgService.isMember(userId, channel.getId())) {
                missingTg.add(channel);
            }
        }

        // Все подписки есть
        if (missingVk.isEmpty() && missingTg.isEmpty()) {
            sendButton(chatId, "✅ все подписки есть", "Участвовать в конкурсе", "join_" + vkId);
            return;
        }

        // Формируем сообщение о недостающих подписках
        StringBuilder sb = new StringBuilder("\uD83D\uDCCC ВАЖНО — подписка на все сообщества и каналы\n\n");


        if (!missingVk.isEmpty()) {
            sb.append("✔\uFE0F Cообщество в <b>VK</b>:\n");
            for (GroupConfig g : missingVk) {
                sb.append("<i><a href=\"https://vk.com/")
                        .append(g.getId()).append("\">")
                        .append(g.getName()).append("</a></i>\n");
            }
            sb.append("\n");
        }

        if (!missingTg.isEmpty()) {
            sb.append("✔\uFE0F <b>Telegram</b>-канал:\n");
            for (ChannelConfig c : missingTg) {

                sb.append("<i><a href=\"https://t.me/+9VBfvYszQRoxNDMy").append("\">")
                        .append(c.getName()).append("</a></i>\n");
            }
            sb.append("\n");
        }

        sb.append("Для проверки подписок, нажмите кнопку в меню \uD83D\uDCE9");

        reply(chatId, sb.toString());
    }

    private void checkSubscriptionsOutput(Long chatId, Long tgId, String vkId)
    {

        if (missingTgChannels(tgId).isEmpty() && missingVkGroups(vkId).isEmpty())
        {
            sendButton(chatId, "✅ все подписки есть", "Участвовать в конкурсе", "join_" + vkId);
            return;
        }

        StringBuilder sb = new StringBuilder("\uD83D\uDCCC ВАЖНО — подписка на все сообщества и каналы\n\n");

        if (!missingVkGroups(vkId).isEmpty())
        {
            sb.append("✔\uFE0F Cообщество в <b>VK</b>:\n");
            for (GroupConfig g : missingVkGroups(vkId)) {
                sb.append("<i><a href=\"https://vk.com/")
                        .append(g.getId()).append("\">")
                        .append(g.getName()).append("</a></i>\n");
            }
            sb.append("\n");
        }

        if (!missingTgChannels(tgId).isEmpty())
        {
            sb.append("✔\uFE0F <b>Telegram</b>-канал:\n");
            for (ChannelConfig c : missingTgChannels(tgId)) {
                sb.append("<i><a href=\"https://t.me/+9VBfvYszQRoxNDMy").append("\">")
                        .append(c.getName()).append("</a></i>\n");
            }
            sb.append("\n");
        }

        sb.append("Для проверки подписок, нажмите кнопку в меню \uD83D\uDCE9");

        reply(chatId, sb.toString());
    }

    private List<ChannelConfig> missingTgChannels(Long userId)
    {
        List<ChannelConfig> missingChannels = new ArrayList<>();
         for (ChannelConfig channel : props.getTelegramChannels())
         {
             if(!tgService.isMember(userId, channel.getId()))
             {
                 missingChannels.add(channel);
             }
         }

         return missingChannels;
    }

    private List<GroupConfig> missingVkGroups(String vkId)
    {
        List<GroupConfig> missingGroups = new ArrayList<>();
        for(GroupConfig group : vkProps.getGroups())
        {
            if(!vkService.areMembers(group, vkId))
            {
                missingGroups.add(group);
            }
        }

        return missingGroups;
    }

    private boolean checkSubscriptionsBool(Long tgId, String vkId)
    {
        if (missingTgChannels(tgId).isEmpty() && missingVkGroups(vkId).isEmpty())
        {
            return true;
        }
        return false;
    }



    private void sendButton(Long chatId, String text, String btnText, String callback) {
        InlineKeyboardButton btn = InlineKeyboardButton.builder()
                .text(btnText)
                .callbackData(callback)
                .build();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(List.of(btn)));

        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(markup)
                .build();

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.info(e.getMessage());
        }
    }

    private void reply(Long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("HTML")
                .build();

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return props.getUsername();
    }

    @Override
    public String getBotToken() {
        return props.getToken();
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }


    /**
     * Парсинг VK ID (число или username из ссылки)
     */
    private String parseVkId(String input) {
        if (input.matches("\\d+")) return input; // если ввели число
        return input.replaceAll(".*vk.com/", "").replaceAll("[^a-zA-Z0-9_]", "").replaceAll("@", "");
    }


}
