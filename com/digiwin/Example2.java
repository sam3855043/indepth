package com.digiwin;
import com.digiwin.PublicKeyFetcher;

/**
 * 使用說明:
 * 1. 確保 JAR 檔案都在當前目錄
 * 
 * 編譯和執行:
 * cd /u1/usr/tiptop/rsa_test
 * java -cp ".:lib/*" com.digiwin.Example2
 */
public class Example2 {
    public static void main(String[] args) {
        try {
            String serverUrl = "https://iam.digiwincloud.com.cn";
            String aiServerUrl = "https://kai-skc.apps.digiwincloud.com.cn/assistant/api/dvmomap6orwubf64";
            // AI 問答功能示範
            String appToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImthaS13aXMiLCJzaWQiOjB9.oEndATum-IdLywv_UGbqRziK7PY1TEMI_BY8wDy-CY8";
            PublicKeyFetcher fetcher = new PublicKeyFetcher(appToken);
            fetcher.initialize(serverUrl);
            fetcher.setAIServerUrl(aiServerUrl);
            
            // 登入取得 token
            String token = fetcher.login("samuel-chuang@digiwin.com", "God1God@", serverUrl);
            
            // AI 問答
            String[] questions = {
                "幫我介紹一下這家公司 digiwin",
                "公司主要產品有哪些?",
                "公司的優勢在哪裡?"
            };
            
            for (String question : questions) {
                System.out.println("\n問題: " + question);
                String answer = fetcher.askQuestion(question,aiServerUrl);
                System.out.println("AI回答: " + answer);
                Thread.sleep(1000); // 避免請求過快
            }
            
        } catch (Exception e) {
            System.err.println("錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
