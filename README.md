

# 🔐 PublicKeyFetcher 說明文件

## 📌 功能簡介

`PublicKeyFetcher` 是一個用於與 Digiwin Cloud IAM 和 AI 伺服器互動的 Java 類別，實現了以下功能：

- 向 IAM 伺服器取得 RSA 公鑰
- 建立本地 RSA 金鑰對
- 透過伺服器提供的 RSA 公鑰加密本地金鑰
- 與伺服器交換 AES 金鑰後，用於加密密碼登入
- 登入成功後，使用 Token 向 AI 伺服器提問

---

## 🧱 類別結構

### 建構子

```java
PublicKeyFetcher(String appToken)
```

- `appToken`：應用程式識別用的 token，用於所有 API 認證。

### 方法總覽

| 方法名 | 說明 |
|--------|------|
| `initialize(String serverUrl)` | 初始化伺服器公鑰與本地 RSA 金鑰對 |
| `setAIServerUrl(String aiServerUrl)` | 設定 AI 伺服器的 URL |
| `login(String userId, String password, String serverUrl)` | 登入 IAM 並取得 Token |
| `askQuestion(String question, String aiServerUrl)` | 向 AI 伺服器發送問題，取得回應 |
| `setIV(String iv)` | 自定義 AES 加密時使用的 IV（必須為 16 字元） |

---

## 🔐 加密流程圖

```
[Client]
   |
   |--(GET)--> /identity/publickey  -----------------------------+
   |                        [Server returns RSA Public Key]      |
   +-------------------------------------------------------------+
   |
   |--(POST encrypted client RSA pub key)--> /identity/aeskey
   |                        [Server returns encrypted AES key]
   |
   |-- decrypt AES key with private key
   |
   |-- encrypt password using AES key and IV
   |
   |--(POST login payload)--> /identity/login
   |                        [Server returns token]
   |
   |--(POST question with token)--> AI Server
```

---

## 🧪 測試範例 Example

可於 `main()` 方法中進行測試：

```java
String appToken = "your-app-token";
String serverUrl = "https://iam.digiwincloud.com.cn";
String aiServerUrl = "https://kai-skc.apps.digiwincloud.com.cn/assistant/api/xxx";

PublicKeyFetcher fetcher = new PublicKeyFetcher(appToken);
fetcher.initialize(serverUrl);

String token = fetcher.login("your-account", "your-password", serverUrl);
String aiReply = fetcher.askQuestion("請介紹一下這家公司", aiServerUrl);
System.out.println(aiReply);
```

### 範例說明

上述程式將：

1. 使用 `appToken` 建立 `PublicKeyFetcher` 實例。
2. 呼叫 `initialize(serverUrl)` 從 IAM 伺服器抓取 RSA 公鑰並準備金鑰配對。
3. 使用帳號與密碼登入 IAM，成功後取得 Token。
4. 呼叫 `askQuestion(...)` 向 AI 伺服器發送提問，例如「請介紹一下這家公司」。
5. 將回答內容列印至終端機。

若需連續提問，可擴充如下：

```java
String[] questions = {
    "幫我介紹一下這家公司 digiwin",
    "公司主要產品有哪些?",
    "公司的優勢在哪裡?"
};

for (String q : questions) {
    String a = fetcher.askQuestion(q, aiServerUrl);
    System.out.println("Q: " + q);
    System.out.println("A: " + a);
    Thread.sleep(1000); // 可避免過度頻繁的請求
}
```

此段示範如何建立多題問答對話流程，並可用於單元測試或 demo 展示。

---

## ⚠️ 注意事項

- 請先呼叫 `initialize()` 再執行 `login()`，否則會拋出錯誤。
- 預設 IV 為 `"ghUb#er57HBh(u%g"`，可透過 `setIV()` 修改。
- 所有加密操作皆使用：
  - RSA/ECB/PKCS1Padding（適用於金鑰交換）
  - AES/CBC/PKCS5Padding（適用於密碼加密）

---

## 🧩 相依套件

- `org.json:json`  
  可從 [Maven Central](https://search.maven.org/) 或使用以下指令引入：

```bash
javac -cp ".:json-20240303.jar" com/digiwin/PublicKeyFetcher.java
java -cp ".:json-20240303.jar" com.digiwin.PublicKeyFetcher
```

---

---

## 📂 專案結構與執行方式

```
├── build                   # 編譯輸出目錄
├── build.sh               # 建構腳本
├── com
│   └── digiwin
│       ├── Example.class               # 測試主程式編譯後的 class
│       ├── Example.java                # 測試主程式原始碼
│       ├── json-20240303.jar          # JSON 處理函式庫（可放入 lib）
│       ├── PublicKeyFetcher.class     # 主功能類別編譯後的 class
│       └── PublicKeyFetcher.java      # 主功能類別原始碼
├── json-20240303.jar       # JSON 函式庫備份（可忽略）
├── lib
│   ├── json-20240303.jar              # JSON 函式庫（放這裡以利 classpath）
│   └── PublicKeyFetcher.jar          # 打包後的功能類別 jar
└── READEME.md              # 專案說明文件
```

### 🔧 編譯與執行方式

若已安裝 JDK 並在 `lib/` 放置 `json-20240303.jar`，可執行以下命令：

```bash
javac -cp ".:lib/json-20240303.jar" com/digiwin/PublicKeyFetcher.java com/digiwin/Example.java
java -cp ".:lib/json-20240303.jar" com.digiwin.Example
```

或使用 `build.sh` 腳本一鍵打包與執行。

## 🛠️ TODO

- 增加錯誤重試與逾時處理
- 支援 AES-256 加密與更多加密選項
- 改為 Streaming API 提問方式（持續推送問答內容）