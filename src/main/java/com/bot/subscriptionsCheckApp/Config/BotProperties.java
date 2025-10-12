package com.bot.subscriptionsCheckApp.Config;

import com.bot.subscriptionsCheckApp.DTO.ChannelConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@Data
@ConfigurationProperties(prefix = "bot")
public class BotProperties
{
    private String username;
    private String token;
    private String webhookUrl;
    private List<ChannelConfig> telegramChannels;
}
