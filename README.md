# NewVotePlugin

投票プラグインです。投票済みかどうかを Tab リストで確認できます。

- 参考プラグイン: https://github.com/TeamKun/VotePlugin

## 謝辞

- 本プラグインの原案と演出に関して、やなー氏 (`Yanaaaaa`) に感謝します。
- AI である [GPT-5.3-Codex](https://openai.com/codex)（OpenAI）の支援を受けて開発されました。

## 対応バージョン

- 今回の動作確認済み:
  - Minecraft `1.15.2` / Paper `1.15.2` (JDK 11)
  - Minecraft `1.16.5` / Paper `1.16.5` (JDK 16)
  - Minecraft `1.21.11` / Paper `1.21.11` (Java 23)
- 補足 (Java 23 での直接起動):
  - Minecraft `1.15.2` / Paper `1.15.2` は Paper 側が Java 14 までを要求
  - Minecraft `1.16.5` / Paper `1.16.5` は Paper 側が Java 16 までを要求
- コード観点での互換見込み:
  - `1.15.2` - `1.21.x`

補足:
- 本プラグインは NMS (net.minecraft.server) を使わず Bukkit/Spigot API のみで実装しています。
- サウンド再生は互換フォールバックを実装しているため、中間バージョンでの enum 差分にも耐性があります。

## コマンド一覧

### OP 権限が必要

- `/vs`
  - 1回目: 投票開始
  - 2回目: 投票締切と結果表示
  - `vget` 後: Tab リスト表示の投票先をクリア
- `/vget`
  - 各投票者の投票先を Tab リストに表示

### 権限不要

- `/v <投票先の名称>`
  - 対象に投票

### 特殊コマンド

- `/yvote`
  - `Yanaaaaa` プレイヤー専用コマンド
  - 引数なし: やなーもーど ON/OFF
  - 引数1つ: 演出発動の順位閾値を変更

## 使用方法

1. `/vs` で投票を開始
2. `/v <投票先>` で投票
3. `/vs` で投票締切と投票結果を表示
4. `/vget` で各プレイヤーの投票先を表示
5. `/vs` で Tab リスト上の投票先表示を削除

## 投票先の設定

通常はオンラインプレイヤーが投票先になります。

`config.yml` の `List` に要素を入れると、投票先はその固定リストに切り替わります。

```yaml
List:
  - "対象1"
  - "対象2"
```

空配列 (`List: []`) の場合はオンラインプレイヤーが対象です。

## デバッグとエラーログ

`config.yml`:

```yaml
debug:
  enabled: false
  logStackTrace: true
```

- `debug.enabled: true`
  - コマンド実行時の状態遷移ログを出力
- `debug.logStackTrace: true`
  - 例外発生時にスタックトレースを出力

ログには以下の情報を含める設計です。

- 実行コマンド
- 実行者
- 引数
- プラグイン内部状態 (`vs`, `vget`, `vlist`, 投票数など)

## ビルド

```bash
mvn -DskipTests package
```

生成物:

- `target/NewVote-v8.77778.1.jar`