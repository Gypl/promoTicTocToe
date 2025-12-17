package com.example.promoTicToc.config;

import com.example.promoTicToc.bot.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Configuration
//@Slf4j
public class TelegramConfig {

    @Bean
    public TelegramBot telegramBot(@Value("telegram.bot.username") String botName,
                                      @Value("telegram.bot.token") String botToken) {
        TelegramBot telegramBot = new TelegramBot(botName, botToken);
        try {
            var telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
//            log.error("Exception during registration telegram api: {}", e.getMessage());
        }
        return telegramBot;
    }
}