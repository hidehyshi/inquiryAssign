package org.example;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.example.entity.Member;

public class InquiryAssigner implements RequestHandler<Object, String> {
    @Override
    public String handleRequest(Object input, Context context) {
        System.out.println("問い合わせ担当者自動割当処理を開始");
        // サイクル開始日を環境変数から取得
        // 例: START_DATE=2023/10/4
        String startDateStr = System.getenv("START_DATE");
        if (startDateStr == null || startDateStr.isBlank()) {
            System.out.println("START_DATE環境変数が設定されていません");
            return "START_DATE環境変数が設定されていません";
        }
        String webhookUrl = System.getenv("WEBHOOK_URL");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            System.out.println("WEBHOOK_URL環境変数が設定されていません");
            return "WEBHOOK_URL環境変数が設定されていません";
        }
        String membersEnv = System.getenv("MEMBERS");
        if (membersEnv == null || membersEnv.isBlank()) {
            System.out.println("MEMBERS環境変数が設定されていません");
            return "MEMBERS環境変数が設定されていません";
        }
        String groupMention = System.getenv("GROUP_MENTION");
        if (groupMention == null) {
            groupMention = "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        LocalDate startDate = LocalDate.parse(startDateStr, formatter);

        List<Member> members = createMember(membersEnv);
        Member member = assign(startDate, members);
        System.out.println("問い合わせ担当者は: " + member.name());
        try {
            String message = "問い合わせ担当者は " + member.toString() + " です。";
            if (!groupMention.isBlank()) {
                message = groupMention + " 問い合わせ担当者は " + member.toString() + " です。";
            }
            sendMessage(message, webhookUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("問い合わせ担当者自動割当処理を終了");
        return "Executed successfully!";
    }

    /**
     * Slackにメッセージを送信する
     * @param message 送信するメッセージ
     * @param webhook SlackのWebhook URL
     * @throws Exception Slackへの送信に失敗した場合
     * @throws IllegalArgumentException WEBHOOK_URL環境変数が設定されていない場合
     */
    private static void sendMessage(String message, String webhook) throws Exception {
        var client = HttpClients.createDefault();
        var post = new HttpPost(webhook);

        // JSON形式でSlackに送信
        String jsonPayload = "{ \"text\": \"" + message + "\" }";
        post.setEntity(new StringEntity(jsonPayload, "UTF-8"));
        post.setHeader("Content-type", "application/json");

        var response = client.execute(post);
        response.close();
    }

    /**
     * 今日の問い合わせ担当者を返す
     * @param startDate サイクル開始日（最初の水曜日など基準日）
     * @param members 担当者のリスト
     * @return 今日の担当者オブジェクト
     */
    private static Member assign(LocalDate startDate, List<Member> members) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("担当者リストが空です");
        }
        if (startDate.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            throw new IllegalArgumentException("開始日は水曜日にしてください");
        }

        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(startDate, today);
        long weeks = days / 7; // 水曜基準で週数カウント
        int index = (int) (weeks % members.size());

        return members.get(index);
    }

    /**
     * 担当者リストを生成
     * @param membersEnv 環境変数から取得した担当者リスト
     *                   形式は "名前:メンション,名前:メンション"
     * @return 担当者のリスト
     * @throws IllegalArgumentException 環境変数が設定されていない場合
     */
    private static List<Member> createMember(String membersEnv) {
        if (membersEnv == null || membersEnv.isBlank()) {
            throw new IllegalArgumentException("担当者リストが空です");
        }
        return Arrays.stream(membersEnv.split(","))
                .map(s -> {
                    String[] parts = s.split(":");
                    if (parts.length != 2) throw new IllegalArgumentException("MEMBERSの形式が不正です: " + s);
                    return new Member(parts[0], parts[1]);
                })
                .collect(Collectors.toList());
    }
}