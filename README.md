# 問い合わせ担当抽出ツール

問い合わせ担当者を抽出しslackへ通知するLambda関数です。  
以下の手順でセットアップおよび使用方法を説明します。  
## 環境
- Java 17
- AWS Lambda
## セットアップ
1. リポジトリをクローンします。
   ```bash
   git clone リポジトリURL
    ```
   
2. envファイルを作成し、必要な環境変数を設定します。  
   例:
   ```
   MEMBERS=Alice:<@U123456>,Bob:<@U654321>
   WEBHOOK_URL=https://hooks.slack.com/services/yyyyy/xxx
   START_DATE=2023/01/01
   ```
