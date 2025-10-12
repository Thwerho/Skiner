package com.bot.subscriptionsCheckApp;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.TelegramBeansConfig;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.bot")
@EnableConfigurationProperties({BotProperties.class, VkProperties.class})
public class ContestManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContestManagerApplication.class, args);
	}

}
