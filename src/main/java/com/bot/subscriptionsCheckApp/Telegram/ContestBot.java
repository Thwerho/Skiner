package com.bot.subscriptionsCheckApp.Telegram;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import com.bot.subscriptionsCheckApp.DTO.ChannelConfig;
import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import com.bot.subscriptionsCheckApp.Service.ContestService;
import com.bot.subscriptionsCheckApp.Service.TgService;
import com.bot.subscriptionsCheckApp.Service.VkService;
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
public class ContestBot extends TelegramLongPollingBot {
    private final BotProperties props;
    private final VkProperties vkProps;
    private final VkService vkService;
    private final TgService tgService;
    private final ContestService contestService;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();
            String username = update.getMessage().getFrom().getUserName();
            String text = update.getMessage().getText();
            String vkId;

            List<String> greetings = Arrays.asList("/start", "привет", "ку",
                    "как дела?", "здорово", "дарова", "здарова", "здравствуйте");

            if (greetings.stream().anyMatch(message -> text.matches(".*" + message + ".*"))) {
                reply(chatId, "Привет! Отправь свой VK ID или ссылку.");
            } else if (text.matches(".*vk.com/.*")) {
                vkId = parseVkId(text);
                checkSubscriptions(chatId, userId, vkId);
            } else if (text.matches("\\d+")) { // поправил на "+", иначе срабатывало только на одну цифру
                vkId = text;
                checkSubscriptions(chatId, userId, vkId);
            }
        }

        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Long userId = update.getCallbackQuery().getFrom().getId();
            String username = update.getCallbackQuery().getFrom().getUserName();

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
        StringBuilder sb = new StringBuilder("Подпишись на все группы/каналы ❌\n\n");

        if (!missingVk.isEmpty()) {
            sb.append("❗Не подписан на VK:\n");
            for (GroupConfig g : missingVk) {
                sb.append("➡️ <a href=\"https://vk.com/")
                        .append(g.getId()).append("\">")
                        .append(g.getName()).append("</a>\n");
            }
            sb.append("\n");
        }

        if (!missingTg.isEmpty()) {
            sb.append("❗Не подписан на Telegram:\n");
            for (ChannelConfig c : missingTg) {
                String cleanId = c.getId().replace("@", "");
                sb.append("➡️ <a href=\"https://t.me/")
                        .append(cleanId).append("\">")
                        .append(c.getName()).append("</a>\n");
            }
            sb.append("\n");
        }

        sb.append("Чтобы проверить подписки, просто пришли ссылку на свой аккаунт в ВК еще раз.");

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

    /**
     * Парсинг VK ID (число или username из ссылки)
     */
    private String parseVkId(String input) {
        if (input.matches("\\d+")) return input; // если ввели число
        return input.replaceAll(".*vk.com/", "").replaceAll("[^a-zA-Z0-9_]", "");
    }
}
