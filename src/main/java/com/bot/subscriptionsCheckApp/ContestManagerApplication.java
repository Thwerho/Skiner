package com.bot.subscriptionsCheckApp;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.TelegramBeansConfig;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.bot")
@EnableConfigurationProperties({BotProperties.class, VkProperties.class})
@EnableScheduling
public class ContestManagerApplication {

	public static void main(String[] args)
    {
		SpringApplication.run(ContestManagerApplication.class, args);
	}

}
