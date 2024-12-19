package tw.com.sc.jasypt;

import org.jasypt.util.text.AES256TextEncryptor;

public class JasyptDecryption {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("使用方式: java JasyptDecryption <密碼> <加密文字>");
            System.out.println("範例: java JasyptDecryption myPassword ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)");
            System.exit(1);
        }

        String password = args[0];
        String encryptedText = args[1];

        if (encryptedText.startsWith("ENC(") && encryptedText.endsWith(")")) {
            encryptedText = encryptedText.substring(4, encryptedText.length() - 1);
        }

        AES256TextEncryptor encryptor = new AES256TextEncryptor();
        encryptor.setPassword(password);

        try {
            String decryptedText = encryptor.decrypt(encryptedText);
            System.out.println("解密後：" + decryptedText);
        } catch (Exception e) {
            System.out.println("解密失敗：" + e.getMessage());
        }
    }
}   
