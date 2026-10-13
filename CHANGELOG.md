# Changelog

本專案所有值得注意的變更都會記錄於此文件。
格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，版本遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

## [1.0.0-prerelease] - 2026-10-13

### 新增

- 專案文件：使用指南、系統架構與技術文件、系統設計文件、建置與發行指南、MIT License。
- `prerelease` 測試發行版本 Build Type：啟用 R8 程式碼壓縮 / 混淆與資源壓縮，以 Debug 金鑰簽署，版本 `1.0.0-prerelease`。
- 發行產物 `app-prerelease.apk`。

### 說明

- 本版本為 **Pre-release 版本**，以 Debug 金鑰簽署，**非正式上線版本**，僅供測試與內部驗證。

## [1.0.0] - 2026-10-13

### 新增

- API 設定檔（Profile）管理：多組設定、測試連線、模型下拉選單、加密 API Key。
- 對話管理：新增 / 刪除 / 重新命名、延後建立、時間排序。
- 聊天：SSE 串流、思考內容顯示、停止生成、Markdown 渲染、複製 / 再次發問 / 刪除。
- 上下文管理：零依賴 Token 估算與預算裁剪。
- TTS 朗讀：播放 / 暫停 / 停止、總是朗讀、語言選擇。
- 大螢幕適配：NavigationBar / NavigationRail 與平板雙欄。
- App 設定：顯示模式、字型大小、朗讀設定（DataStore 持久化）。
- 除錯記錄：內建記錄頁與可開關記錄器。
- 穩健性：非串流後備、自動重試、逾時、空回覆提示。

[1.0.0-prerelease]: https://github.com/mark216tw/android-llmchat-01/releases
[1.0.0]: https://github.com/mark216tw/android-llmchat-01/releases
