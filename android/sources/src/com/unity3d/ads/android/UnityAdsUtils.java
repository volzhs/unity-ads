package com.unity3d.ads.android;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.unity3d.ads.android.properties.UnityAdsProperties;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsUtils {

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	@SuppressLint("PackageManagerGetSignatures")
	public static boolean isDebuggable() {
	    boolean debuggable = false;
	    boolean problemsWithData = false;
		PackageManager pm;
		String pkgName;

		if (UnityAdsProperties.APPLICATION_CONTEXT != null) {
			pm = UnityAdsProperties.APPLICATION_CONTEXT.getPackageManager();
			pkgName = UnityAdsProperties.APPLICATION_CONTEXT.getPackageName();
		}
		else return false;

	    try {
	        ApplicationInfo appinfo = pm.getApplicationInfo(pkgName, 0);
	        debuggable = (0 != (appinfo.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
	    }
	    catch (NameNotFoundException e) {
			UnityAdsDeviceLog.debug("Could not find name");
	        problemsWithData = true;
	    }

	    if (problemsWithData) {
		    try {
		        PackageInfo pinfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
		        Signature signatures[] = pinfo.signatures;

				for (Signature signature : signatures) {
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					ByteArrayInputStream stream = new ByteArrayInputStream(signature.toByteArray());
					X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
					debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
					if (debuggable)
						break;
				}
		    }
		    catch (NameNotFoundException e) {
				UnityAdsDeviceLog.debug("Could not find name");
		    }
		    catch (CertificateException e) {
				UnityAdsDeviceLog.debug("Certificate exception");
		    }
	    }

	    return debuggable;
	}

	@SuppressLint("DefaultLocale")
	public static String Md5 (String input) {
		if (input == null) return null;

		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		if (m == null) return null;

		byte strData[] = input.getBytes();
		int length = input.length();
		m.update(strData,0,length);
		byte p_md5Data[] = m.digest();

		String mOutput = "";
		for (byte aP_md5Data : p_md5Data) {
			int b = (0xFF & aP_md5Data);
			// if it is a single digit, make sure it have 0 in front (proper padding)
			if (b <= 0xF) mOutput += "0";
			// add number to string
			mOutput += Integer.toHexString(b);
		}
		// hex string to uppercase
		return mOutput.toUpperCase(Locale.US);
	}

	@SuppressWarnings("SameParameterValue")
	public static String readFile (File fileToRead, boolean addLineBreaks) {
		String fileContent = "";
		BufferedReader br;

		if (fileToRead.exists() && fileToRead.canRead()) {
			try {
				br = new BufferedReader(new FileReader(fileToRead));
				String line;
				
				while ((line = br.readLine()) != null) {
					fileContent = fileContent.concat(line);
					if (addLineBreaks)
						fileContent = fileContent.concat("\n");
				}
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Problem reading file: " + e.getMessage());
				return null;
			}

			try {
				br.close();
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Problem closing reader: " + e.getMessage());
			}

			return fileContent;
		}
		else {
			UnityAdsDeviceLog.error("File did not exist or couldn't be read");
		}
		
		return null;
	}

	public static boolean writeFile (File fileToWrite, String content) {
		FileOutputStream fos;

		try {
			fos = new FileOutputStream(fileToWrite);
			fos.write(content.getBytes());
			fos.flush();
			fos.close();
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Could not write file: " + e.getMessage());
			return false;
		}

		UnityAdsDeviceLog.debug("Wrote file: " + fileToWrite.getAbsolutePath());

		return true;
	}

	public static boolean canUseExternalStorage () {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}

	public static void runOnUiThread (Runnable runnable) {
		runOnUiThread(runnable, 0);
	}

	@SuppressWarnings("SameParameterValue")
	private static void runOnUiThread (Runnable runnable, long delay) {
		Handler handler = new Handler(Looper.getMainLooper());

		if (delay  > 0) {
			handler.postDelayed(runnable, delay);
		}
		else {
			handler.post(runnable);
		}
	}
}