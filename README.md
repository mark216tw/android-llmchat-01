# LLM Chat — Android 應用程式功能說明

一個可自行輸入 LLM API 連線參數、與任意 **OpenAI 相容**模型對話的 Android App。
使用者可建立多組 API 設定檔、管理多個對話，並以串流方式與模型聊天。

- **版本**：`1.0.0`
- **授權**：MIT License（見 [LICENSE](LICENSE)）

---

## 文件索引

| 文件 | 說明 |
| --- | --- |
| [使用指南](docs/使用指南.md) | 安裝與操作說明（含 FAQ）。 |
| [系統架構與技術文件](docs/系統架構與技術文件.md) | 技術棧、模組架構、資料流、網路層與安全性。 |
| [系統設計文件](docs/系統設計文件.md) | 設計目標、UI/UX、狀態管理與錯誤處理。 |
| [建置與發行指南](docs/建置與發行指南.md) | 建置指令、測試發行版與 GitHub Release 流程。 |
| [CHANGELOG](CHANGELOG.md) | 版本變更記錄。 |
| [LICENSE](LICENSE) | MIT License。 |

---

## 目錄

- [專案簡介](#專案簡介)
- [技術棧](#技術棧)
- [功能總覽](#功能總覽)
  - [1. API 設定檔（Profile）](#1-api-設定檔profile)
  - [2. 對話管理](#2-對話管理)
  - [3. 聊天](#3-聊天)
  - [4. 上下文管理](#4-上下文管理)
  - [5. Markdown 渲染](#5-markdown-渲染)
  - [6. 大螢幕適配](#6-大螢幕適配)
  - [7. 設定（顯示模式 / 字型大小）](#7-設定顯示模式--字型大小)
  - [8. 除錯記錄](#8-除錯記錄)
  - [9. 穩健性與相容性](#9-穩健性與相容性)
- [架構與資料流](#架構與資料流)
- [專案結構](#專案結構)
- [資料模型](#資料模型)
- [建置方式](#建置方式)
- [注意事項與已知限制](#注意事項與已知限制)

---

## 專案簡介

- 應用程式名稱：**LLM Chat**
- Package：`com.example.llmchat`
- 使用者自行輸入 API 連線參數（Base URL / API Key / 模型），連線測試成功後即可在 App 內與 LLM 對話。
- 支援建立多個對話、刪除對話、刪除單一問答、複製問題 / 回覆、再次發問。

---

## 技術棧

| 類別 | 選用 |
| --- | --- |
| 語言 / UI | Kotlin + Jetpack Compose（Material 3） |
| 架構 | MVVM + Repository（單一 `app` module） |
| 導航 | Navigation Compose |
| 網路 | Retrofit + OkHttp + `okhttp-sse` + kotlinx.serialization |
| 本機儲存 | Room（對話 / 訊息）、DataStore（設定） |
| 依賴注入 | Hilt |
| 安全 | `androidx.security.crypto`（加密 API Key） |
| 非同步 | Coroutines + Flow |
| 版本 | minSdk 26 / targetSdk 35 / compileSdk 35 |

---

## 功能總覽

### 1. API 設定檔（Profile）

支援 **多組設定檔**，可隨時切換使用中的設定檔。

- **清單頁**
  - 新增、編輯、刪除設定檔（刪除有確認對話框）。
  - 以 RadioButton 選擇「使用中」的設定檔。
  - 顯示名稱、Base URL 與模型名稱。
- **編輯頁欄位**
  - 設定名稱
  - Base URL（OpenAI 相容端點，通常以 `/v1` 結尾）
  - API Key（可切換顯示 / 隱藏）
  - 模型名稱（必填，需與 `/models` 回報的 id 一致）
  - System Prompt（可留空）
  - Temperature（滑桿 0.0–2.0）
  - Max Output Tokens（**0 = 不指定**，由伺服器決定；思考型模型建議留 0）
  - Context Window（模型上下文長度）
  - Thinking 模式（DeepSeek）：預設（不指定）/ 啟用 / 停用
  - Reasoning Effort：預設 / `none` / `low` / `high` / `max`
- **測試連線**
  - 呼叫 `GET /models` 驗證端點與金鑰。
  - 成功後列出可用模型 id，提供**下拉選單挑選模型**，避免手打錯誤。
  - 失敗時顯示原因（401 / 403 / 404 / 429 / 伺服器錯誤等）。
- **安全性**
  - API Key 以 `EncryptedSharedPreferences` 加密儲存（Android Keystore），不明文落盤、不寫入記錄。

### 2. 對話管理

- **對話清單頁**
  - 新增對話（若尚未設定 API，會引導至設定頁）。對話採**延後建立**：只有在送出第一則訊息後才寫入資料庫，未輸入就返回清單不會留下空白對話。
  - 刪除對話（含確認；連帶刪除該對話所有訊息）。
  - 長按 / 點擊圖示重新命名。
  - 依「最後更新時間」由新到舊排序。
  - **標題搜尋**：清單上方提供搜尋欄並隨清單捲動；輸入關鍵字即時篩選對話標題（不分大小寫），欄位右側 **x** 可一鍵清除並恢復完整清單。
  - 標題「對話」右側以小字顯示**目前全域啟用的 API 設定檔名稱**（隨設定頁切換即時更新）。
- **長按訊息選單**
  - 在聊天頁**長按使用者訊息**會彈出選單：**複製**、**再次發問**、**刪除**。
    - **複製**：把問題內容複製到系統剪貼簿。
    - **再次發問**：把該問題內容帶入輸入框，可編輯後重新送出（不會自動刪除原問答）。
    - **刪除**：確認後刪除該問題**及其對應的助理回答**。
  - **長按助理回覆**提供：**複製**、**朗讀**（以系統 TTS 唸出回覆；朗讀前會移除 Markdown 符號，但保留清單標記，如有序 `1.`/`2.` 與無序 `-`/`*`/`+`）。
  - 不允許單獨刪除助理回覆。

### 3. 聊天

- 使用 OpenAI 相容 `POST {baseUrl}/chat/completions`。
- **SSE 串流**：透過 `okhttp-sse` 逐字接收回覆。
- **思考內容顯示**：解析 DeepSeek `reasoning_content`，以淡化的「思考中…」區塊呈現。
- **停止生成**：串流中可中斷；已產生的內容仍會保存。
- **訊息氣泡**：使用者靠右、助理靠左；助理回覆以 Markdown 呈現；氣泡下方顯示時間戳（當日 `HH:mm`，非當日 `MM-dd HH:mm`）。
- **複製訊息**：長按任一訊息可**複製問題**或**複製回覆**（見「對話管理」）。
- **朗讀回覆**：長按助理回覆選擇「朗讀」；朗讀期間頂部列顯示**播放/暫停**與**停止**圖示，正在朗讀的氣泡會以主色外框標示。可於設定開啟「總是朗讀」讓每次回覆完成後自動朗讀。
- **進入定位**：由對話清單（含平板雙欄）進入既有對話時，標題列立即顯示對話名稱，畫面頂端停在**最後一個問題**；載入期間顯示進度指示。
- **捲動**：只有在使用者已位於底部時才跟隨串流內容自動捲動；往上閱讀時不被打斷，並可點右下「回到底部」按鈕。
- **送出**：點送出鈕或按鍵盤 Enter／Send 鍵皆可送出（串流中為停止鈕）。
- **錯誤列**：顯示 HTTP 錯誤與**伺服器回傳的錯誤內容**。
- **Token 資訊列**：顯示本次估算 token 數與被省略的早期訊息數。
- **標題列**：顯示當前對話標題。

### 4. 上下文管理

LLM API 本身無狀態，每次請求需送出「裁剪後」的訊息子集；完整歷史永遠保存在本機。

- **零依賴 Token 估算**：中日韓字元約 1 token/字，其他約 4 字元/token。
- **Token 預算裁剪**：
  - 預算 = `Context Window` − 保留輸出（`maxTokens` 或預設 2048）− 5% 安全邊界。
  - 從**最舊的非 system 訊息**開始丟棄，直到符合預算。
  - 對齊到 `user` 訊息邊界（避免歷史以 assistant 開頭）。
  - **System Prompt 永不裁剪**。
- 僅裁剪送給 API 的子集，資料庫內容不受影響。

### 5. Markdown 渲染

助理回覆支援 Markdown（使用者訊息維持純文字），自製零相依渲染器，支援：

- 標題（`#`～`######`）
- **粗體**、*斜體*、~~刪除線~~
- `` `行內程式` `` 與 ```` ``` ```` 程式碼區塊（等寬字 + 可橫向捲動）
- 無序 / 有序清單
- 引言（`>`）
- 分隔線（`---`）
- 連結（`[文字](網址)`，以主色 + 底線呈現）
- **表格**：GFM 表格以框線呈現，每欄固定寬度、允許換行；寬度至少撐滿可用空間（直向螢幕會延伸至畫面右緣），欄位過多時可**橫向捲動**。

### 6. 大螢幕適配

- 依螢幕寬度自動切換（門檻 600dp）：
  - **精簡（手機）**：底部 `NavigationBar`，單頁堆疊導航。
  - **展開 (平板/大螢幕)**：左側 `NavigationRail`；對話頁為**清單 + 聊天雙欄**。
- 已修正 compact 模式下的雙重系統列 inset，浮動按鈕（＋）正確貼齊底部導覽列。
- 進入聊天（非清單）路由時，底部導覽列與清單頁的新增（＋）浮動按鈕會與聊天頁轉場**同步淡出/收起**（約 160ms），不再瞬間消失或位移。
- 除「由對話清單進入聊天」外，關閉其他頁面切換的過渡動畫（`NavHost` 全域轉場為 `None`）；僅 `chat` 路由覆寫為由右側滑入 / 返回滑出。平板雙欄直接在清單選取，不經路由、無轉場。

### 7. 設定（顯示模式 / 字型大小）

由**對話清單（主畫面）右上角齒輪圖示**進入「設定」頁（底部導覽的「API設定」分頁仍是 API 設定檔管理，兩者不同）。

- **顯示模式**：系統預設 / 淺色 / 深色，預設為**系統預設**；即時套用至整個 App（含系統列圖示配色）。
- **字型大小**：以滑桿調整 **12–24sp**，附即時預覽；套用至聊天訊息氣泡（含 Markdown 標題、程式碼區塊、表格，依比例縮放）、輸入框與 Token 資訊列。
- **總是朗讀**：開啟後，LLM 每次回覆完成會自動以 TTS 朗讀該回覆。
- **朗讀語言**：系統預設 / 中文（台灣）/ English；若裝置不支援所選語言，會提示並改用系統預設語音。
- 各項設定皆持久化（DataStore），重開 App 後保留。

### 8. 除錯記錄

App 內建除錯記錄頁（「API設定」頁右上角蟲蟲圖示），無需 Logcat。

- **記錄內容**：送出的請求（body，API Key 不在其中）、每個原始 SSE 事件、非串流後備結果、`finish_reason`、錯誤（含伺服器回應內容）。
- **記憶體記錄器**：上限 500 筆、單筆截斷，執行緒安全。
- **操作**：由新到舊顯示、顏色分級；「複製全部」可貼出；「清空」刪除記錄。
- **可開關**：可設定「啟用記錄」開啟 / 關閉（狀態持久化；關閉後不再寫入）。

### 9. 穩健性與相容性

- **非串流後備**：若伺服器忽略 `stream=true` 而未回任何串流內容且未失敗，自動改用一次非串流請求。
- **自動重試**：`/models`、非串流後備，以及「尚未收到任何內容」的串流連線，遇 `IOException` 或 429/5xx 會以指數退避最多重試 3 次。
- **串流結束判定**：收到 `data: [DONE]` **或**帶 `finish_reason` 的分塊即結束。
- **逾時**：串流閒置逾時 120 秒，避免連線卡死。
- **空回覆提示**：若最終無內容，依 `finish_reason` 顯示中文提示（`length` / `content_filter` / `tool_calls` 等）。
- **參數清理**：送出前將 `temperature` 四捨五入至 2 位。
- **相容其他端點**：`thinking` 欄位僅在使用者明確設定時才送出，維持對一般 OpenAI 相容服務的相容性。

---

## 架構與資料流

```
UI (Compose)  ──►  ViewModel (Hilt)  ──►  Repository  ──►  Room / DataStore
                                              │
                                              └►  Remote (Retrofit / OkHttp SSE)  ──►  LLM API
```

- 畫面狀態以 `StateFlow` 暴露，UI 以 `collectAsStateWithLifecycle()` 訂閱。
- 對話與訊息透過 Room `Flow` 即時更新。
- 聊天流程：組裝上下文（`ContextBuilder`）→ 串流請求（`ChatStreamClient`）→ 逐字更新 UI → 完成後寫入資料庫。

---

## 專案結構

```
app/src/main/java/com/example/llmchat/
├── LlmChatApp.kt                # @HiltAndroidApp
├── MainActivity.kt              # 進入點、自適應導航（NavigationBar / NavigationRail）
├── data/
│   ├── datastore/               # SettingsDataStore（Profile、除錯開關、顯示模式、字型大小、朗讀語言）
│   ├── local/                   # Room：Entity / DAO / AppDatabase
│   ├── remote/                  # Retrofit API、DTO、SSE 串流（ChatStreamClient）
│   ├── repository/              # ProfileRepository / ConversationRepository / ChatRepository
│   ├── security/                # CryptoManager（加密 API Key）
│   └── settings/                # AppSettingsStore（顯示模式 / 字型大小）
├── debug/                       # DebugLogStore（記憶體除錯記錄）
├── di/                          # AppModule（Hilt 提供者、Room Migration）
├── domain/
│   ├── context/                 # TokenEstimator / ContextConfig / ContextBuilder
│   └── model/                   # Profile / Conversation / Message / ChatRole / ThemeMode / TtsLanguage
├── util/                        # AppLog（集中 Logcat TAG）
└── ui/
    ├── chat/                    # ChatScreen / ChatViewModel / MarkdownText / SpeechController
    ├── conversations/           # ConversationListScreen / ConversationViewModel
    ├── debug/                   # DebugLogScreen / DebugLogViewModel
    ├── navigation/              # Routes
    ├── settings/                # ProfileList / ProfileEdit / AppSettingsScreen / ViewModel
    └── theme/                   # Color / Theme / Type / LocalChatFontSize / LocalTtsLocale
```

---

## 資料模型

| 表 | 欄位 |
| --- | --- |
| `profiles` | id, name, baseUrl, model, systemPrompt, temperature, maxTokens, contextWindow, thinkingMode, reasoningEffort, createdAt |
| `conversations` | id, profileId(FK), title, createdAt, updatedAt |
| `messages` | id, conversationId(FK), role, content, createdAt |

- 外鍵設為 `CASCADE`：刪除 Profile 會連帶刪除其對話；刪除對話會連帶刪除其訊息。
- 資料庫版本 v2（含 `MIGRATION_1_2`，保留既有資料）。

---

## 建置方式

需求：JDK 17、Android SDK（platform 35、build-tools 35）、網路（首次下載相依）。
完整說明見 [建置與發行指南](docs/建置與發行指南.md)。

```powershell
# 於專案根目錄
.\gradlew.bat assembleDebug        # 除錯版
.\gradlew.bat assemblePrerelease   # 測試發行版（R8 壓縮 + Debug 金鑰簽署）

# 產物
# app\build\outputs\apk\debug\app-debug.apk
# app\build\outputs\apk\prerelease\app-prerelease.apk
```

`prerelease` 為測試發行版本（Build Type `prerelease`）：啟用 R8 程式碼壓縮 / 混淆與資源壓縮，以 **Debug 金鑰簽署**，版本為 `1.0.0-prerelease`。

使用版本：Gradle 8.9 / AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.09.02。

---

## 注意事項與已知限制

- **需要自行提供 API**：本 App 不內建任何金鑰，使用者的 API Key 僅加密儲存於裝置本機。
- **明文 HTTP**：允許 cleartext 流量，以支援自架的 Ollama / LM Studio / vLLM 等本機服務；請僅設定可信任的端點。
- **模型 id 必填**：需與端點的 `/models` 回報一致；可於設定頁測試連線後由下拉選單挑選。
- **思考型模型**：`thinking` 與 `reasoning_effort` 屬 DeepSeek 特有欄位；部分第三方轉接服務不支援，若遇 400 請將 Thinking 模式設為「預設（不指定）」。
- **逐字顯示**：採伺服器串流；若端點一次回傳整段內容，則會一次顯示。
- **Token 估算**：為近似值，僅用於裁剪判斷，非精準計費。
