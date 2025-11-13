package com.bot.subscriptionsCheckApp.Config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@Getter
@Setter
@ConfigurationProperties(prefix = "youtube")
public class YoutubeProperties
{
    String link;
    String name;
    List<YoutubeProperties> channels;
}
