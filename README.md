# Galaxy Quick Panel

「One Shade」のような、画面上部から引き下ろして使える **Galaxy (One UI) 風のカスタムクイックパネル** Android アプリです。
標準の通知シェードとは別に、自分好みのクイック設定パネルをどのアプリの上にもオーバーレイ表示できます。

## 特徴

- **どこからでも引き下ろし** — 画面最上部の細いトリガー領域を下にスワイプするとパネルが開きます。
- **One UI 風デザイン** — ダーク基調・丸いトグルタイル・青いアクセント・角丸パネル。
- **クイック設定タイル** — Wi-Fi / Bluetooth / ライト / 自動回転 / サウンド(通常・バイブ・サイレント) / 機内モード / 位置情報 / 設定。
- **スライダー** — 画面の明るさとメディア音量をその場で調整。
- **時計＆日付** — パネル上部に現在時刻と日付を表示。
- **常駐サービス** — フォアグラウンドサービスで動作し、再起動後も自動復帰（設定が有効な場合）。

## 動作の仕組み

Android のサードパーティアプリは、近年 Wi-Fi・Bluetooth・機内モード・位置情報などの無線系を直接 ON/OFF できません。
そのため本アプリは公式アプリ（One Shade など）と同様に、

- **直接操作できるもの**（ライト・明るさ・自動回転・サウンドモード・メディア音量）はその場で切り替え、
- **OS が制限しているもの**（Wi-Fi・Bluetooth・機内モード・位置情報）はタイルの状態を表示しつつ、タップで該当のシステム設定パネルへ誘導します。

## 必要な権限

| 権限 | 用途 |
| --- | --- |
| 他のアプリの上に表示 (`SYSTEM_ALERT_WINDOW`) | パネルを画面に重ねて表示 |
| システム設定の変更 (`WRITE_SETTINGS`) | 明るさ・自動回転の変更 |
| 通知の表示 (`POST_NOTIFICATIONS`) | 常駐サービスの通知 |

権限はアプリ内の画面から順番に許可できます。すべて許可してから「クイックパネルを有効にする」を押してください。

## ビルド方法

Android SDK が必要です。Android Studio で開くか、コマンドラインから:

```bash
# SDK の場所を指定（未作成の場合）
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

./gradlew assembleDebug
# 生成物: app/build/outputs/apk/debug/app-debug.apk
```

- minSdk: 26 (Android 8.0)
- targetSdk / compileSdk: 34 (Android 14)
- 言語: Kotlin / View Binding

## プロジェクト構成

```
app/src/main/java/com/kama/galaxyquickpanel/
├── MainActivity.kt                 権限の許可とサービスの有効/無効を行う設定画面
├── service/
│   ├── QuickPanelService.kt        トリガーを常駐させるフォアグラウンドサービス
│   └── BootReceiver.kt             再起動後の自動復帰
├── panel/
│   ├── QuickPanelController.kt     トリガー＆パネルのオーバーレイ管理・アニメーション
│   └── PanelRootView.kt            戻るキーでパネルを閉じるルートビュー
├── tiles/
│   ├── Tile.kt                     タイルのモデル
│   └── TileFactory.kt              既定のタイル一覧
├── system/
│   └── SystemController.kt         実際の端末操作（ライト/明るさ/音量/設定誘導 等）
└── util/
    ├── Permissions.kt              権限チェック
    └── Prefs.kt                    設定の保存
```

## 注意事項

- ジェスチャーナビゲーション端末では、画面最上部のシステムジェスチャー領域とトリガーが重なる場合があります。`res/values/dimens.xml` の `trigger_height` で高さを調整できます。
- 「サイレント」への切り替えは、端末によっては「サイレント通知（DND）」へのアクセス許可が必要なことがあります。
