# DB設計書

## 1. この設計書の目的

この文書は、やること管理アプリがデータを保存するための設計です。**テーブル**（同じ種類の情報を行と列で保存する箱）は、MySQL の `todos` 1つだけを使います。やること1件が、`todos` の1行に対応します。

画面の配置やボタンの設計は、この文書には含めません。

## 2. テーブル一覧

| テーブル名 | 用途 | 1行が表すもの |
|---|---|---|
| `todos` | やることを保存する | 登録したやること1件 |

## 3. `todos` テーブルの列設計

**列**（テーブル内の項目）と、その役割は次のとおりです。`NULL` は「値が入っていないこと」を表します。

| 列名 | 表示名・用途 | 型（保存形式） | NULLを許可 | 初期値（登録時の最初の値） | 制約・補足 |
|---|---|---|---|---|---|
| `id` | 番号 | `BIGINT` | いいえ | 自動採番 | **主キー**（各行を重複なく見分ける番号）。1から自動で増える。 |
| `title` | やること | `VARCHAR(255)` | いいえ | なし | 最大255文字。空白だけはアプリ側の入力確認で保存しない。 |
| `detail` | メモ | `VARCHAR(255)` | はい | `NULL` | 最大255文字。任意入力。 |
| `category` | ジャンル | `VARCHAR(255)` | いいえ | なし | 「デザイン」「マーケティング」「プログラミング」「資格」「就職活動」のいずれか。 |
| `priority` | 優先度 | `INT` | いいえ | `2` | `1`=高、`2`=中、`3`=低。 |
| `due_date` | 期限 | `DATE` | はい | `NULL` | 任意の日付。時刻は保存しない。 |
| `completed` | 完了状態 | `BOOLEAN` | いいえ | `FALSE` | `FALSE`（0）=未完了、`TRUE`（1）=完了。 |
| `created_at` | 登録日時 | `DATETIME` | いいえ | 登録時の現在日時 | MySQLが自動で設定する。 |
| `updated_at` | 更新日時 | `DATETIME` | いいえ | 登録時の現在日時 | MySQLが登録時と更新時に自動で設定する。 |
| `deleted_at` | 削除日時 | `DATETIME` | はい | `NULL` | `NULL` は通常表示、日時が入っている行は削除済みとして扱う。 |

## 4. 要件と列の対応

| 要件 | 対応する列 | 設計での扱い |
|---|---|---|
| 1件を見分ける番号 | `id` | 主キーとし、編集・削除する1件を指定するために使う。 |
| やることを必須で登録・編集する | `title` | `NOT NULL`（空の値を許可しない）にする。255文字まで保存できる。空白だけの入力はアプリ側で未入力として扱う。 |
| メモを任意で登録・編集する | `detail` | `NULL`を許可する。255文字まで保存できる。 |
| 5種類のジャンルから選ぶ | `category` | `NOT NULL`にし、許可された5つの値だけを保存する制約を付ける。検索時は完全一致で使う。 |
| 高・中・低の優先度を選ぶ | `priority` | `1`・`2`・`3`だけを保存する制約を付ける。初期値は仕様どおり`2`（中）。 |
| 期限を任意で登録・編集する | `due_date` | `NULL`を許可し、日付だけを保存する。並び替えに使う。 |
| 完了・未完了を管理する | `completed` | `FALSE`を未完了、`TRUE`を完了として保存する。新規登録時の初期値は未完了。 |
| 登録日時を残す | `created_at` | 登録時にMySQLが現在日時を自動設定する。画面の入力欄には出さない。 |
| 最後の変更日時を残す | `updated_at` | 登録・更新のたびにMySQLが現在日時を自動設定する。画面の入力欄には出さない。 |
| 名前の一部で絞り込む | `title` | 部分一致で検索する対象。検索する言葉そのものは保存しない。 |
| ジャンルで絞り込む | `category` | 完全一致で検索する対象。 |
| 期限の近い順・遠い順に並べる | `due_date` | 日付順で並び替える対象。 |

## 5. DDL（テーブルを作るSQL）

**DDL**（データベースの箱や列を作るためのSQL）は、次のとおりです。MySQL 8.0.16以降を前提にしています。`CHECK` は、保存できる値を限定するためのルールです。

```sql
CREATE TABLE todos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255) NULL,
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_todos PRIMARY KEY (id),
    CONSTRAINT chk_todos_category CHECK (
        category IN ('デザイン', 'マーケティング', 'プログラミング', '資格', '就職活動')
    ),
    CONSTRAINT chk_todos_priority CHECK (priority IN (1, 2, 3)),
    CONSTRAINT chk_todos_completed CHECK (completed IN (0, 1)),

    INDEX idx_todos_category (category),
    INDEX idx_todos_due_date (due_date)
);
```

`AUTO_INCREMENT` は、番号を自動で1ずつ増やす指定です。`INDEX`（索引、検索や並び替えを速くする目次）は、ジャンルでの絞り込みと期限順の表示に備えて付けています。名前の「一部を含む」検索は、通常の索引では効きにくいため、ここでは専用の索引を追加しません。

## 6. 保存時のルール

データベースの制約に加え、次の内容はアプリの入力確認で守ります。これは、利用者に決められたエラーメッセージを表示するためです。

| 確認する内容 | 対象列 | 理由 |
|---|---|---|
| `title`、`category`、`priority` が未入力でないこと | `title`、`category`、`priority` | 必須項目のため。半角空白・全角空白だけも未入力として扱う。 |
| `title` と `detail` が255文字以内であること | `title`、`detail` | 要件の文字数上限を守るため。 |
| ジャンルと優先度が選択肢内であること | `category`、`priority` | DBの`CHECK`制約と同じルールで、保存前に分かりやすく知らせるため。 |
| 期限は未入力でもよいこと | `due_date` | `NULL`を保存できるようにする。追加の入力確認は行わない。 |

## 7. 期限未入力の並び順

期限がないやることは、期限があるやることの**後ろ**に表示します。期限の近い順・遠い順のどちらでも同じです。期限未入力の行どうしは、`id` の小さい順で安定して表示します。

例として、一覧取得時のSQLは次の形にします。`ASC` は小さい日付から、`DESC` は大きい日付から並べる指定です。

```sql
-- 期限が近い順（期限未入力は最後）
ORDER BY due_date IS NULL ASC, due_date ASC, id ASC

-- 期限が遠い順（期限未入力は最後）
ORDER BY due_date IS NULL ASC, due_date DESC, id ASC
```

## 8. 対象外のデータ

ログイン利用者、画面の検索条件、操作結果メッセージ、登録・編集・削除のログは、`todos` テーブルには保存しません。要件で使用するテーブルは`todos`だけであり、ログの保存先となる別テーブルは作成しません。
