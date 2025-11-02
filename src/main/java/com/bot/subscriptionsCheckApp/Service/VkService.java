package com.bot.subscriptionsCheckApp.Service;

import com.bot.subscriptionsCheckApp.Config.VkProperties;
import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class VkService {
    private final VkProperties properties;
    private final WebClient http = WebClient.create("https://api.vk.com/method");

    /**
     * Проверка подписки пользователя на группу
     */
    public Boolean areMembers(GroupConfig g, String userId) {
        String numericUserId = resolveId(userId, "user");

        String numericGroupId = resolveId(g.getId(), "group");

        Map<?, ?> res = http.get()
                .uri(uriBuilder -> uriBuilder.path("/groups.isMember")
                        .queryParam("group_id", numericGroupId)
                        .queryParam("user_id", numericUserId)
                        .queryParam("access_token", properties.getServiceToken())
                        .queryParam("v", properties.getApiVersion())
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();


        boolean isMember = false;
        if (res != null && res.get("response") != null) {
            Object response = res.get("response");
            if (response instanceof Integer i)
            {
                isMember = (i == 1);
            }
            else if (response instanceof Map<?, ?> m)
            {
                isMember = "1".equals(String.valueOf(m.get("member")));
            }
        }

        return isMember;
    }

    /**
     * Преобразует screen_name (vk.com/xxx) в числовой ID
     * @param id - screen_name или число
     * @param expectedType "user" или "group"
     */
    public String resolveId(String id, String expectedType) {
        // если уже число → возвращаем как есть
        if (id.matches("\\d+")) return id;

        String screenName = id.replaceAll("https?://vk.com/", "")
                .replaceAll("vk.com/", "")
                .replaceAll("[^a-zA-Z0-9_]", "")
                .replaceAll("@", "");

        Map<?, ?> res = http.get()
                .uri(uriBuilder -> uriBuilder.path("/utils.resolveScreenName")
                        .queryParam("screen_name", screenName)
                        .queryParam("access_token", properties.getServiceToken())
                        .queryParam("v", properties.getApiVersion())
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (res != null && res.get("response") instanceof Map<?, ?> resp) {
            String type = (String) resp.get("type");
            Integer numericId = (Integer) resp.get("object_id");

            if (expectedType.equals(type) && numericId != null) {
                // для одной группы groups.isMember ждет положительный ID (или screen_name)
                return String.valueOf(numericId);
            }
        }

        return id; // fallback — вернём как есть
    }
}
