package com.digiwin;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class PublicKeyFetcher {
    private String appToken;
    private PublicKey serverPublicKey;
    private java.security.KeyPair clientKeyPair;
    private String aesKey;
    private String ivString;
    private String token;

    public PublicKeyFetcher(String appToken) {
        this.appToken = appToken;
        this.ivString = "ghUb#er57HBh(u%g";  // 預設 IV
    }

    /**
     * 初始化加密相關的金鑰
     */
    public void initialize() throws Exception {
        // 取得服務器公鑰
        String serverPublicKeyStr = fetchServerPublicKey(appToken);
        this.serverPublicKey = getPublicKeyFromBase64(serverPublicKeyStr);
        
        // 生成客戶端金鑰對
        java.security.KeyPairGenerator keyPairGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);
        this.clientKeyPair = keyPairGen.generateKeyPair();
    }

    /**
     * 執行登入流程
     */
    public String login(String userId, String password) throws Exception {
        if (serverPublicKey == null || clientKeyPair == null) {
            throw new IllegalStateException("Please call initialize() first");
        }

        // 加密客戶端公鑰
        String base64ClientPublicKey = Base64.getEncoder().encodeToString(clientKeyPair.getPublic().getEncoded());
        byte[] clientPublicKeyStringBytes = base64ClientPublicKey.getBytes("UTF-8");
        String encryptedAESKey = encryptAESKeyWithRSA(clientPublicKeyStringBytes, serverPublicKey);

        // 獲取 AES 金鑰
        String encryptedAesKeyFromServer = fetchAesKeyFromServer(appToken, encryptedAESKey);
        
        // 解密 AES 金鑰
        byte[] decryptedAesKeyBytes = decryptWithPrivateKey(Base64.getDecoder().decode(encryptedAesKeyFromServer));
        this.aesKey = Base64.getEncoder().encodeToString(decryptedAesKeyBytes);

        // 加密密碼
        String encryptedPassword = toggleEncrypt(password, this.aesKey, this.ivString);
        
        // 執行登入
        this.token = loginAndGetToken(appToken, userId, encryptedPassword, encryptedAESKey);
        return this.token;
    }

    /**
     * 向 AI 發送問題
     */
    public String askQuestion(String question) throws Exception {
        if (token == null) {
            throw new IllegalStateException("Please login first");
        }
        return askAI(token, question);
    }

    /**
     * 設置自定義 IV
     */
    public void setIV(String iv) {
        if (iv.length() != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes long");
        }
        this.ivString = iv;
    }

    // 步驟 1：取得伺服器 RSA 公鑰 (Base64)
    public static String fetchServerPublicKey(String appToken) throws Exception {
        URL url = new URL("https://iam.digiwincloud.com.cn/api/iam/v2/identity/publickey");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("digi-middleware-auth-app", appToken);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        JSONObject json = new JSONObject(response.toString());
        return json.getString("publicKey");
    }

    // 步驟 2：將 Base64 RSA 公鑰轉成 PublicKey 物件
    public static PublicKey getPublicKeyFromBase64(String base64Key) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    // 步驟 3：產生 AES 金鑰（128-bit）和 16-byte IV
    public static String[] generateAESKeyAndIV() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128, new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();
        byte[] aesKey = secretKey.getEncoded();

        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        return new String[] {
            Base64.getEncoder().encodeToString(aesKey),
            Base64.getEncoder().encodeToString(iv)
        };
    }

    // 步驟 4：用伺服器公鑰加密 AES 金鑰 (改為接受 byte[])
    public static String encryptAESKeyWithRSA(byte[] data, PublicKey serverPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encrypted = cipher.doFinal(data);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // 步驟 5：使用 AES 金鑰加密密碼（與 C# 的 ToggleEncrypt 對應），改用 AES-CBC 並接收 IV
    public static String toggleEncrypt(String plainText, String base64AESKey, String ivString) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(base64AESKey);
        byte[] ivBytes = ivString.getBytes("UTF-8"); // 必須為 16 bytes

        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 分段解密：用私鑰解密 RSA 加密內容
    public static String decryptByPrivateKey(String base64Encrypted, java.security.PrivateKey privateKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(base64Encrypted);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        int inputLen = encryptedBytes.length;
        int offSet = 0;
        int maxDecryptBlock = 128; // 1024 bits = 128 bytes
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        while (inputLen - offSet > 0) {
            byte[] cache;
            if (inputLen - offSet > maxDecryptBlock) {
                cache = cipher.doFinal(encryptedBytes, offSet, maxDecryptBlock);
            } else {
                cache = cipher.doFinal(encryptedBytes, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            offSet += maxDecryptBlock;
        }

        byte[] decryptedData = out.toByteArray();
        out.close();
        return new String(decryptedData, "UTF-8");
    }

    // 測試主程式
    public static void main(String[] args) {
        try {
            String appToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6ImthaS13aXMiLCJzaWQiOjB9.oEndATum-IdLywv_UGbqRziK7PY1TEMI_BY8wDy-CY8"; // ⚠️ 替換成你的實際 token

            PublicKeyFetcher fetcher = new PublicKeyFetcher(appToken);
            fetcher.initialize();

            String userId = "wangqyb@digiwin.com";
            String password = "@Wang0705";
            String token = fetcher.login(userId, password);

            System.out.println("登入取得的 Token: " + token);

            String selectedText = "幫我介紹一下這家公司"; // ⚠️ 可改為實際輸入
            String aiReply = fetcher.askQuestion(selectedText);
            System.out.println("✅ AI 回覆:\n" + aiReply);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 從 server 取得加密的 AES key
    public static String fetchAesKeyFromServer(String appToken, String encryptedClientPublicKey) throws Exception {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("clientEncryptPublicKey", encryptedClientPublicKey);
        System.out.println("送出 JSON:\n" + jsonBody.toString(2)); // pretty print with indent

        URL aesUrl = new URL("https://iam.digiwincloud.com.cn/api/iam/v2/identity/aeskey");
        HttpURLConnection aesConn = (HttpURLConnection) aesUrl.openConnection();
        aesConn.setRequestMethod("POST");
        aesConn.setRequestProperty("Content-Type", "application/json");
        aesConn.setRequestProperty("digi-middleware-auth-app", appToken);
        aesConn.setDoOutput(true);

        aesConn.getOutputStream().write(jsonBody.toString().getBytes("UTF-8"));
        aesConn.getOutputStream().flush();
        aesConn.getOutputStream().close();

        BufferedReader aesIn = new BufferedReader(new InputStreamReader(aesConn.getInputStream()));
        StringBuilder aesResponse = new StringBuilder();
        String line2;
        while ((line2 = aesIn.readLine()) != null) {
            aesResponse.append(line2);
        }
        aesIn.close();

        // 顯示伺服器回應
        System.out.println("伺服器回應:\n" + aesResponse.toString());

        // 從伺服器回應中取得 encryptAesKey
        JSONObject aesJson = new JSONObject(aesResponse.toString());
        return aesJson.getString("encryptAesKey");
    }

    // 發送登入請求取得 token
    public static String loginAndGetToken(String appToken, String userId, String encryptedPassword, String encryptedClientPublicKey) throws Exception {
        JSONObject loginBody = new JSONObject();
        loginBody.put("identityType", "token");
        loginBody.put("userId", userId);
        loginBody.put("passwordHash", encryptedPassword);
        loginBody.put("clientEncryptPublicKey", encryptedClientPublicKey);

        System.out.println("Login Request JSON:\n" + loginBody.toString(2));

        URL loginUrl = new URL("https://iam.digiwincloud.com.cn/api/iam/v2/identity/login");
        HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json");
        loginConn.setRequestProperty("digi-middleware-auth-app", appToken);
        loginConn.setDoOutput(true);

        loginConn.getOutputStream().write(loginBody.toString().getBytes("UTF-8"));
        loginConn.getOutputStream().flush();
        loginConn.getOutputStream().close();

        // 取得 token 回應
        BufferedReader loginIn = new BufferedReader(new InputStreamReader(loginConn.getInputStream()));
        StringBuilder loginResponse = new StringBuilder();
        String line3;
        while ((line3 = loginIn.readLine()) != null) {
            loginResponse.append(line3);
        }
        loginIn.close();

        System.out.println("Login Response JSON:\n" + loginResponse.toString());

        JSONObject loginJson = new JSONObject(loginResponse.toString());
        return loginJson.getString("token");
    }

    // 使用 token 向 AI 發問
    public static String askAI(String token, String question) throws Exception {
        JSONObject aiRequest = new JSONObject();
        aiRequest.put("question", question);

        URL indepthUrl = new URL("https://kai-skc.apps.digiwincloud.com.cn/assistant/api/ljftmag8vhx177be");
        HttpURLConnection indepthConn = (HttpURLConnection) indepthUrl.openConnection();
        indepthConn.setRequestMethod("POST");
        indepthConn.setRequestProperty("Content-Type", "application/json");
        indepthConn.setRequestProperty("token", token);
        indepthConn.setRequestProperty("Accept-Language", "zh-CN");
        indepthConn.setDoOutput(true);

        indepthConn.getOutputStream().write(aiRequest.toString().getBytes("UTF-8"));
        indepthConn.getOutputStream().flush();
        indepthConn.getOutputStream().close();

        BufferedReader indepthIn = new BufferedReader(new InputStreamReader(indepthConn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder fullMessage = new StringBuilder();
        String line4;
        System.out.println("📡 AI Streaming Response:");
        while ((line4 = indepthIn.readLine()) != null) {
            if (!line4.trim().isEmpty()) {
                if (line4.trim().startsWith("data:")) {
                    String jsonString = line4.trim().substring("data:".length());
                    try {
                        JSONObject part = new JSONObject(jsonString);
                        String msg = part.optString("message", "");
                        fullMessage.append(msg);
                    } catch (Exception ex) {
                        System.out.println("⚠️ 無法解析片段 JSON 內容: " + jsonString);
                    }
                }
            }
        }
        indepthIn.close();
        return fullMessage.toString();
    }

    private byte[] decryptWithPrivateKey(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, clientKeyPair.getPrivate());
        return cipher.doFinal(encrypted);
    }
}
/* 
 * 
 * javac -cp ".:json-20240303.jar" PublicKeyEncryptorUsingJSON.java
 * java -cp ".:json-20240303.jar" PublicKeyEncryptorUsingJSON
 * 
*/