package com.bot.subscriptionsCheckApp.Telegram;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import com.bot.subscriptionsCheckApp.DTO.ChannelConfig;
import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import com.bot.subscriptionsCheckApp.Service.*;
import com.bot.subscriptionsCheckApp.listener.JoinRequestListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ContestJoinBot extends TelegramLongPollingBot
{
    private final BotProperties props;
    private final VkProperties vkProps;
    private final VkService vkService;
    private final TgService tgService;
    private final ContestService contestService;

    private final JoinRequestListener joinRequestListener; // внедрение обработчика
    private final JoinRequestService joinRequestService; // внедрение сервиса обработчика

    private final SubsCheckService subsCheckService; // сервис проверки на отписку


    @PostConstruct
    public void init()
    {
        joinRequestService.setBot(this);
    }

    @Override
    public void onUpdateReceived(Update update)
    {

        joinRequestListener.onUpdate(update); //включаем обработку заявок

        // обработка сообщений в бота
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();
            String text = update.getMessage().getText();
            String vkId;

            List<String> greetings = Arrays.asList("/start", "привет", "ку",
                    "как дела?", "здорово", "дарова", "здарова", "здравствуйте");

            if (greetings.stream().anyMatch(message -> text.matches(".*" + message + ".*"))
                    || text.matches("/check_subscriptions")) {
                reply(chatId, "Привет! Отправь ссылку на свою страницу в ВК.\n" +
                        "<b>Пример: vk.com/ВАШ_ЮЗЕРНЕЙМ\n" +
                        "Пример: https://vk.com/ВАШ_ЮЗЕРНЕЙМ</b>\n");
            } else if (text.matches(".*vk.com/.*")) {
                vkId = parseVkId(text);
                checkSubscriptions(chatId, userId, vkId);
            } else if (text.matches("\\d+")) { // поправил на "+", иначе срабатывало только на одну цифру
                vkId = text;
                checkSubscriptions(chatId, userId, vkId);
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
        }
    }


    /**
     * Проверяет подписки VK и Telegram
     */
    private void checkSubscriptions(Long chatId, Long userId, String vkId) {
        List<GroupConfig> missingVk = new ArrayList<>();
        List<ChannelConfig> missingTg = new ArrayList<>();

        // Проверка VK групп
        Map<String, Boolean> memberships = vkService.areMembers(
                vkProps.getGroups().stream().map(GroupConfig::getId).toList(),
                vkId
        );
        for (GroupConfig group : vkProps.getGroups()) {
            if (!memberships.getOrDefault(group.getId(), false)) {
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
                String cleanId = c.getName_id().replace("@", "");
                sb.append("<i><a href=\"https://t.me/+9VBfvYszQRoxNDMy").append("\">")
                        .append(c.getName()).append("</a></i>\n");
            }
            sb.append("\n");
        }

        sb.append("Чтобы проверить свои подписки, отправьте ссылку на свою страницу в ВК ещё раз \uD83D\uDCE9");

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
            e.printStackTrace();
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
