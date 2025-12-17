package com.example.promoTicToc.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

//@Slf4j
@RestController
public class TelegramBot extends TelegramLongPollingBot {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("telegram.miniapp.url")
    private String miniAppUrl;
    private final String botName;

    public TelegramBot(String botName, String botToken) {
        super(botToken);
        this.botName = botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // 2. ОБРАБОТКА ДАННЫХ ИЗ WEB APP
        if (update.getMessage().getWebAppData() != null) {
            try {
                handleGameEvent(update);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            var chatId = message.getChatId();
//            log.info("Message received: {}", message.getChatId());
            var messageText = message.getText();
            try {
                SendMessage msg = new SendMessage();
                msg.setChatId(chatId.toString());
                msg.setText("Отдохните и получите подарок. Нажмите кнопку ИГРАТЬ");
                execute(msg);

            } catch (TelegramApiException e) {
                System.out.println(e.getMessage());
//                log.error("Exception during processing telegram api: {}", e.getMessage());
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    @PostMapping("/api/web-app-callback")
    public void handleWebApp(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        String initData = (String) body.get("initData");

        String botToken = ("$BOT_TOKEN");

        if (isInitDataValid(initData, botToken)) {
            // 2. Извлекаем userId из поля 'user'
            String userId = extractUserId(initData);

            // 3. Отправляем сообщение через бота
            SendMessage message = new SendMessage();
            message.setChatId(userId);
            message.setText("Спасибо! Ваш заказ принят.");
            try {
                execute(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("Data integrity check failed");
        }
    }

    // 1. Хеш-проверка (HMAC-SHA256)
    private boolean isInitDataValid(String initData, String botToken) {
        try {
            // Парсим строку в Map и декодируем значения
            Map<String, String> params = Arrays.stream(initData.split("&"))
                    .map(p -> p.split("=", 2))
                    .collect(Collectors.toMap(
                            s -> s[0],
                            s -> URLDecoder.decode(s[1], StandardCharsets.UTF_8)
                    ));

            String hash = params.remove("hash");

            // Сортируем ключи по алфавиту и собираем строку для проверки
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            // Вычисляем secret_key = HMAC_SHA256("WebAppData", botToken)
            byte[] secretKey = hmacSha256("WebAppData".getBytes(), botToken.getBytes());

            // Вычисляем финальный HMAC от dataCheckString
            byte[] calculatedHashBytes = hmacSha256(secretKey, dataCheckString.getBytes());
            String calculatedHash = bytesToHex(calculatedHashBytes);

            return calculatedHash.equals(hash);
        } catch (Exception e) {
            return false;
        }
    }

    // 2. Вспомогательный метод извлечения ID пользователя
    private String extractUserId(String initData) throws JsonProcessingException {
        String userJson = Arrays.stream(initData.split("&"))
                .filter(p -> p.startsWith("user="))
                .map(p -> URLDecoder.decode(p.split("=", 2)[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow();


        // Парсим JSON с помощью Jackson
        JsonNode root = objectMapper.readTree(userJson);
        return root.get("id").asText();
    }

    private byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
////////////////////////////////////////////////////////////////////////////////////


    private void handleWebAppData(Message message) {
        var chatId = message.getChatId();
        // Получаем строку, которую мы отправили из JS через Telegram.WebApp.sendData()
        String webAppData = message.getWebAppData().getData();

        try {
            SendMessage response = new SendMessage();
            response.setChatId(chatId.toString());

            // Логика реагирования в зависимости от данных
            if (webAppData.contains("WIN")) {
                response.setText("Вы выбрали подарок! Мы готовим его для вас.");
            } else if (webAppData.contains("discount")) {
                response.setText("Ваша скидка 10% применена.");
            } else {
                response.setText("Получены данные из Web App: " + webAppData);
            }

            execute(response);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка при обработке Web App: " + e.getMessage());
        }
    }


    // ===== Callback / WebApp events =====
    public Reply gameEventsReply() {
        return Reply.of(
                (bot, upd) -> {
                    try {
                        handleGameEvent(upd);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                },
                upd -> upd.hasMessage() && upd.getMessage().getWebAppData() != null
        );
    }


    private void handleGameEvent(Update update) throws TelegramApiException {
        String data = update.getMessage().getWebAppData().getData();
        Long chatId = update.getMessage().getChatId();
        System.out.println("handleGameEvent"+data);


// Пример: frontend Mini App присылает JSON
// {"result":"WIN"} или {"result":"LOSE"}
        if (data.contains("WIN")) {
            String promo = generatePromo();
            sendWinMessage(chatId, promo);
        } else {
            sendLoseMessage(chatId);
        }
    }


    private void sendWinMessage(Long chatId, String promo) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("🎉 Победа! Ваш промокод: " + promo);
        msg.setReplyMarkup(restartKeyboard());
        execute(msg);
    }


    private void sendLoseMessage(Long chatId) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("😢 Вы проиграли. Попробуйте снова!");
        msg.setReplyMarkup(restartKeyboard());
        execute(msg);
    }


    private InlineKeyboardMarkup participateKeyboard() {
        InlineKeyboardButton btn = InlineKeyboardButton.builder()
                .text("Участвовать")
                .webApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniAppUrl))
                .build();
        return new InlineKeyboardMarkup(List.of(List.of(btn)));
    }


    private InlineKeyboardMarkup restartKeyboard() {
        InlineKeyboardButton btn = InlineKeyboardButton.builder()
                .text("Играть заново")
                .webApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniAppUrl))
                .build();
        return new InlineKeyboardMarkup(List.of(List.of(btn)));
    }


    private String generatePromo() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }




    @Override
    public String getBotUsername() {
        return this.botName;
    }
}