package edu.berkeley.security.eventtracker.network;
import android.util.Base64;
  
import javax.crypto.Cipher;  
import javax.crypto.SecretKeyFactory;  
import javax.crypto.spec.PBEKeySpec;  
import javax.crypto.spec.PBEParameterSpec;  
import java.io.ByteArrayOutputStream;  
import java.util.Random;
  
public class GibberishAESCrypto {  
  
  private static final String CIPHER_ALG = "PBEWITHMD5AND256BITAES-CBC-OPENSSL";  
  private static final String PREFIX = "Salted__";  
  private static final String UTF_8 = "UTF-8";  
  
  public static String encrypt(String plainText, char[] password) throws Exception {  
    byte[] salt = new byte[8];
    new Random().nextBytes(salt);  
  
    Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, salt, password);  
    byte[] cipherText = cipher.doFinal(plainText.getBytes(UTF_8));  
  
    ByteArrayOutputStream baos = new ByteArrayOutputStream(cipherText.length + 16);  
    baos.write(PREFIX.getBytes(UTF_8));  
    baos.write(salt);  
    baos.write(cipherText);  
  
    return Base64.encodeToString(baos.toByteArray(),0);  
  }  
  
  public static String decrypt(String cipherText, char[] password) throws Exception {  
    byte[] input = Base64.decode(cipherText,0);  
  
    String prefixText = new String(input, 0, 8, UTF_8);  
//    Validate.isTrue(prefixText.equals(PREFIX), "Invalid prefix: ", prefixText);  
  
    byte[] salt = new byte[8];  
    System.arraycopy(input, 8, salt, 0, salt.length);  
  
    Cipher cipher = createCipher(Cipher.DECRYPT_MODE, salt, password);  
    byte[] plainText = cipher.doFinal(input, 16, input.length - 16);  
  
    return new String(plainText, UTF_8);  
  }  
  
  private static Cipher createCipher(int cipherMode, byte[] salt, char[] password)  
      throws Exception {  
  
    PBEKeySpec pbeSpec = new PBEKeySpec(password);  
    SecretKeyFactory keyFact = SecretKeyFactory.getInstance(CIPHER_ALG);  
    PBEParameterSpec defParams = new PBEParameterSpec(salt, 0);  
  
    Cipher cipher = Cipher.getInstance(CIPHER_ALG);  
    cipher.init(cipherMode, keyFact.generateSecret(pbeSpec), defParams);  
    return cipher;  
  } 
}