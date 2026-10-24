#!/usr/bin/env bash
echo "=========================================================="
echo "Biên dịch KuCoin Pre-FOMO Scanner cho Samsung Galaxy A32"
echo "=========================================================="
chmod +x gradlew
./gradlew assembleDebug
echo ""
echo "=> Xong! File APK đã sẵn sàng tại:"
echo "   app/build/outputs/apk/debug/app-debug.apk"
echo "=========================================================="
