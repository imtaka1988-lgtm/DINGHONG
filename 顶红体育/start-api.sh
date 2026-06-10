#!/bin/bash
export ODDS_API_KEY='24b4f41df3efc29a862dffdf3d4f3e0c'

export DEEPSEEK_API_KEY="DEEPSEEK_API_KEY_PLACEHOLDER"
export BAIDU_SEARCH_KEY="BAIDU_SEARCH_KEY_PLACEHOLDER"

export WECHAT_APPID="wx02b7b2822291875d"
export WECHAT_SECRET="d27eef30f6a7bd26088b047a5738f5b9"

cd /data/dinghong/app/dinghong-api

pkill -f "dinghong-api-1.0.0.jar" || true

nohup java -jar target/dinghong-api-1.0.0.jar > api.log 2>&1 &
