package tw.com.sc.jasypt;

import org.jasypt.util.text.AES256TextEncryptor;

public class JasyptEncryption {
    public static void main(String[] args) {
        // 檢查是否提供足夠的參數
        if (args.length != 2) {
            System.out.println("使用方式: java JasyptEncryption <密碼> <要加密的文字>");
            System.exit(1);
        }

        AES256TextEncryptor encryptor = new AES256TextEncryptor();
        encryptor.setPassword(args[0]);  // 使用第一個參數作為密碼

        String plainText = args[1];      // 使用第二個參數作為要加密的文字
        String encryptedText = encryptor.encrypt(plainText);

        System.out.println("加密後：ENC(" + encryptedText + ")");
    }
}
