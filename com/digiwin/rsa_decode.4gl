-- rsa_decode.4gl

IMPORT base

MAIN
  DEFINE ch base.Channel
  DEFINE result, decoded_chunk, b64_chunk STRING
  DEFINE outfile STRING
  DEFINE ls_cmd STRING
  LET outfile = "decrypted.txt"

  -- 清空輸出檔
  LET ch = base.Channel.create()
  CALL ch.openFile(outfile, "w")
  CALL ch.writeLine("")
  CALL ch.close()
    DISPLAY "開始解密..."
  LET ls_cmd = 'head -n 1 encrypted.b64 > part.b64'
  RUN ls_cmd
  LET ls_cmd = 'openssl base64 -d -in part.b64 -out part.bin'
  RUN ls_cmd
  LET ls_cmd = 'openssl rsautl -decrypt -inkey private_key.pem -in part.bin > ' || outfile
  RUN ls_cmd

  DISPLAY "解密完成，結果儲存於 decrypted.txt"
END MAIN
