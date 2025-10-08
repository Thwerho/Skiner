package com.bot.subscriptionsCheckApp;

import com.bot.subscriptionsCheckApp.Config.BotProperties;
import com.bot.subscriptionsCheckApp.Config.VkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BotProperties.class, VkProperties.class})
public class SubscriptionsCheckAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubscriptionsCheckAppApplication.class, args);
	}

}
