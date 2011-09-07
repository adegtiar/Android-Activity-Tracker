/**
 * A Util class that provides the ability to hash a string 
 */

package edu.berkeley.security.eventtracker.network;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;

public class HashingUtils {

	/**
	 * Returns a md5 hash of the given input string NOTE: Do not modify this
	 * implementation without also modifying the md5 hash implementation server
	 * side as well.
	 */
	public static String hashPassword(String password) {
		byte[] hashedString = HashingUtils.hash(password);
		String s = Base64.encodeToString(hashedString, 0);
		return s.substring(0, s.length() - 3);

	}

	private static byte[] hash(String password) {

		MessageDigest digest = null;
		try {
			// MD5-128
			digest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		digest.update(password.getBytes());
		byte messageDigest[] = digest.digest();
		return messageDigest;

	}

}
