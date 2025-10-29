package com.bot.subscriptionsCheckApp.Telegram;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import com.bot.subscriptionsCheckApp.DTO.ChannelConfig;
import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import com.bot.subscriptionsCheckApp.Repos.ContestUserRepository;
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
import java.util.Arrays;
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
            String vkId;

            List<String> greetings = Arrays.asList("/start", "привет", "ку",
                    "как дела?", "здорово", "дарова", "здарова", "здравствуйте");

            if (greetings.stream().anyMatch(message -> text.matches(".*" + message + ".*")))
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

            else if (text.matches("/check_subscriptions"))
            {
                if(contestService.findByTgId(chatId))
                {
                    sendButton(chatId, "Чтобы проверить подписки, нажмите кнопку ниже.\n",
                            "Проверить подписки", "check_subscriptions");
                }
                else
                {
                    sendButton(chatId, "Чтобы проверить подписки, привяжите свою страницу в ВК.<b>Для этого нажмите кнопку\n</b>\n",
                            "Проверить подписки", "check_subscriptions");
                }
            }

            else if (text.matches("/add_vk"))
            {
                reply(chatId, "Для того, чтобы привязать страницу в VK, оставьте ссылку на нее в сообщении ниже по примеру:\n\n" +
                        "Пример: https://vk.com/ВАШ_ЮЗЕРНЕЙМ_VK\n" +
                        "Или\n" +
                        "Пример: vk.com/ВАШ_ЮЗЕРНЕЙМ_VK\n");
            }

            else if (text.matches(".*vk.com/.*")) {
                vkId = parseVkId(text);
                sendButton(chatId, "Чтобы привязать свой VK нажмите на кнопку ниже:", "Привязать VK", "addUser_" + vkId);
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
                boolean ok = contestService.addUser(userId, username, vkId);
                reply(chatId, ok ? "Страница в VK успешно привязана 🎉" : "Ошибка, обратитесь к @mollen44 в Telegram, чтобы привязать страницу.");
            }

            if (data.startsWith("check_subscriptions"))
            {

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
            sendButton(chatId, "Все подписки есть ✅", "Участвовать в конкурсе", "join_" + vkId);
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

        sb.append("Чтобы проверить свои подписки, нажмите кнопку в меню \uD83D\uDCE9");

        reply(chatId, sb.toString());
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
        return input.replaceAll(".*vk.com/", "").replaceAll("[^a-zA-Z0-9_]", "");
    }


}
