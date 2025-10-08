package com.bot.subscriptionsCheckApp.Config;

import com.bot.subscriptionsCheckApp.DTO.GroupConfig;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@Getter
@Setter
@ConfigurationProperties(prefix = "vk")
public class VkProperties
{
    private String serviceToken;
    private String apiVersion;
    private List<GroupConfig> groups;
}
