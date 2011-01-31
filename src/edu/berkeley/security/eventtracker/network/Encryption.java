package edu.berkeley.security.eventtracker.network;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
/**
 *Class providing helpful encryption methods
 */
public class Encryption {


	    public static byte[] hash(String password){

			MessageDigest digest=null;
			try {
				//MD5-128
				digest = java.security.MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			digest.update(password.getBytes());
			byte messageDigest[] = digest.digest();
			int lengthOfKey=messageDigest.length;
			return messageDigest; 
			
	    }
	    
	    public static byte[] decrypt(String cipherText, Key key) {
	    	Cipher dCipher = null;
			try {
				dCipher = Cipher.getInstance("AES");
			} catch (NoSuchAlgorithmException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchPaddingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	try {
				dCipher.init(Cipher.DECRYPT_MODE, key);
			} catch (InvalidKeyException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	byte[] decrypted = null;
			try {
				decrypted = dCipher.doFinal(Base64.decode(cipherText, 0));
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return decrypted;
	    	
	    	
	    }
	    //converts a byte array into a string
	    public static String base64(byte[] cipherText){
	    	return Base64.encodeToString(cipherText, 0);
	    }
	    
	    

	    public static byte[] encrypt(String plainText, Key key){

			Cipher cipher = null;
			try {
				cipher = Cipher.getInstance("AES");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				cipher.init(Cipher.ENCRYPT_MODE, key);
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] encrypted = null;
			try {
				encrypted = cipher.doFinal(plainText.getBytes());
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return encrypted;
	    }
}
