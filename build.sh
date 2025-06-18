#!/bin/bash

# 清理舊文件
rm -f lib/PublicKeyFetcher.jar
rm -rf build
mkdir -p build/com/digiwin lib

# 複製依賴包
cp json-20240303.jar javax.json-*.jar lib/

# 複製源代碼到構建目錄
cp com/digiwin/PublicKeyFetcher.java build/com/digiwin/

# 創建 MANIFEST.MF (注意目錄位置)
mkdir -p build/META-INF
cat > build/META-INF/MANIFEST.MF << EOF
Manifest-Version: 1.0
Created-By: Digiwin
Main-Class: com.digiwin.PublicKeyFetcher
Class-Path: json-20240303.jar javax.json-1.1.4.jar javax.json-api-1.1.4.jar

EOF

# 切換到構建目錄進行編譯
cd build
javac -cp "../lib/*" com/digiwin/PublicKeyFetcher.java

# 確保在正確目錄下打包
jar cvfm ../lib/PublicKeyFetcher.jar META-INF/MANIFEST.MF com/digiwin/*.class

# 返回原目錄並清理
cd ..
rm -rf build/com build/META-INF

echo "Build complete. JAR created at lib/PublicKeyFetcher.jar"