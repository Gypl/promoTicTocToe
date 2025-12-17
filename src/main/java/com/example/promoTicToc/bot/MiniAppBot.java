//package com.example.promoTicToc.bot;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.telegram.abilitybots.api.bot.AbilityBot;
//import org.telegram.abilitybots.api.objects.Ability;
//import org.telegram.abilitybots.api.objects.Reply;
//import org.telegram.abilitybots.api.sender.SilentSender;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
//
//
//import java.util.List;
//import java.util.UUID;
//
//
//import static org.telegram.abilitybots.api.objects.Flag.TEXT;
//import static org.telegram.abilitybots.api.objects.Locality.USER;
//
////@Component
//public class MiniAppBot extends AbilityBot {
//
//
//    @Value("https://tic-yadrejj.amvera.io/")
//    private String miniAppUrl;
//
//
//    protected MiniAppBot(
//            @Value("8498893712:AAELoTBs8YMYNdzpok0CUPuixQ1PFBfPqsw") String token,
//            @Value("PromoTicToc456788656_bot") String username) {
//        super(token, username);
//    }
//
//
//    @Override
//    public long creatorId() {
//        return 0; // не используется
//    }
//
//
//    // ===== /start =====
//    public Ability start() {
//        return Ability.builder()
//                .name("start")
//                .info("Start command")
//                .locality(USER)
//                .input(0)
//                .action(ctx -> {
//                    SendMessage msg = new SendMessage();
//                    msg.setChatId(ctx.chatId());
//                    msg.setText("Отдохните и получите подарок");
//                    msg.setReplyMarkup(participateKeyboard());
//                    silent.execute(msg);
//                })
//                .build();
//    }
//
//
//    // ===== Callback / WebApp events =====
//    public Reply gameEventsReply() {
//        return Reply.of(
//                (bot, upd) -> handleGameEvent(upd),
//                upd -> upd.hasMessage() && upd.getMessage().getWebAppData() != null
//        );
//    }
//
//
//    private void handleGameEvent(Update update) {
//        String data = update.getMessage().getWebAppData().getData();
//        Long chatId = update.getMessage().getChatId();
//
//
//// Пример: frontend Mini App присылает JSON
//// {"result":"WIN"} или {"result":"LOSE"}
//        if (data.contains("WIN")) {
//            String promo = generatePromo();
//            sendWinMessage(chatId, promo);
//        } else {
//            sendLoseMessage(chatId);
//        }
//    }
//
//
//    private void sendWinMessage(Long chatId, String promo) {
//        SendMessage msg = new SendMessage();
//        msg.setChatId(chatId);
//        msg.setText("🎉 Победа! Ваш промокод: " + promo);
//        msg.setReplyMarkup(restartKeyboard());
//        silent.execute(msg);
//    }
//
//
//    private void sendLoseMessage(Long chatId) {
//        SendMessage msg = new SendMessage();
//        msg.setChatId(chatId);
//        msg.setText("😢 Вы проиграли. Попробуйте снова!");
//        msg.setReplyMarkup(restartKeyboard());
//        silent.execute(msg);
//    }
//
//
//    private InlineKeyboardMarkup participateKeyboard() {
//        InlineKeyboardButton btn = InlineKeyboardButton.builder()
//                .text("Участвовать")
//                .webApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniAppUrl))
//                .build();
//        return new InlineKeyboardMarkup(List.of(List.of(btn)));
//    }
//
//
//    private InlineKeyboardMarkup restartKeyboard() {
//        InlineKeyboardButton btn = InlineKeyboardButton.builder()
//                .text("Играть заново")
//                .webApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniAppUrl))
//                .build();
//        return new InlineKeyboardMarkup(List.of(List.of(btn)));
//    }
//
//
//    private String generatePromo() {
//        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }
//
//
//    @Override
//    public SilentSender silent() {
//        return super.silent();
//    }
//}