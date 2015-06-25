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
import java.util.ArrayList;

import javax.security.auth.x500.X500Principal;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.properties.UnityAdsConstants;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsUtils {

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
	private static String _cacheDirectory = null;

	public static boolean isDebuggable(Context ctx) {
	    boolean debuggable = false;
	    boolean problemsWithData = false;

	    if (ctx == null) return false;

	    PackageManager pm = ctx.getPackageManager();
	    try {
	        ApplicationInfo appinfo = pm.getApplicationInfo(ctx.getPackageName(), 0);
	        debuggable = (0 != (appinfo.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
	    }
	    catch (NameNotFoundException e) {
			UnityAdsDeviceLog.debug("Could not find name");
	        problemsWithData = true;
	    }

	    if (problemsWithData) {
		    try {
		        PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
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
		return mOutput.toUpperCase();
	}

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

	public static void removeFile (String fileName) {
		if(fileName != null) {
			File removeFile = new File (fileName);
			File cachedVideoFile = new File (UnityAdsUtils.getCacheDirectory() + "/" + removeFile.getName());

			if (cachedVideoFile.exists()) {
				if (!cachedVideoFile.delete())
					UnityAdsDeviceLog.error("Could not delete: " + cachedVideoFile.getAbsolutePath());
				else
					UnityAdsDeviceLog.debug("Deleted: " + cachedVideoFile.getAbsolutePath());
			}
			else {
				UnityAdsDeviceLog.debug("File: " + cachedVideoFile.getAbsolutePath() + " doesn't exist.");
			}
		}
	}

	public static long getSizeForLocalFile (String fileName) {
		File removeFile = new File (fileName);
		File cachedVideoFile = new File (UnityAdsUtils.getCacheDirectory() + "/" + removeFile.getName());
		long size = -1;

		if (cachedVideoFile.exists()) {
			size = cachedVideoFile.length();
		}

		return size;
	}

	public static void chooseCacheDirectory(Activity activity) {
		File externalCacheDirectory = activity.getExternalCacheDir();

		// If possible, use app private external cache directory. It requires no external storage permission for api level 19+ devices.
		if(externalCacheDirectory != null) {
			_cacheDirectory = externalCacheDirectory.getAbsolutePath() + "/" + UnityAdsConstants.CACHE_DIR_NAME;
		} else {
			_cacheDirectory = Environment.getExternalStorageDirectory() + "/" + UnityAdsConstants.CACHE_DIR_NAME;
		}
	}

	public static String getCacheDirectory () {
		return _cacheDirectory;
	}

	public static boolean canUseExternalStorage () {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}

	public static File createCacheDir () {
		File tdir = new File (getCacheDirectory());
		if (tdir.mkdirs()) {
			boolean success = UnityAdsUtils.writeFile(new File(getCacheDirectory() + "/.nomedia"), "");
			if (!success) UnityAdsDeviceLog.debug("Could not write .nomedia file");
		}

		return tdir;
	}

	public static boolean isFileRequiredByCampaigns (String fileName, ArrayList<UnityAdsCampaign> campaigns) {
		if (fileName == null || campaigns == null) return false;

		File seekFile = new File(fileName);

		if (seekFile.getName().equals(".nomedia"))
			return true;

		for (UnityAdsCampaign campaign : campaigns) {
			File matchFile = new File(campaign.getVideoFilename());
			if (seekFile.getName().equals(matchFile.getName()))
				return true;
		}

		return false;
	}

	public static boolean isFileInCache (String fileName) {
		File targetFile = new File (fileName);
		File testFile = new File(getCacheDirectory() + "/" + targetFile.getName());
		return testFile.exists();
	}

	public static void runOnUiThread (Runnable runnable) {
		runOnUiThread(runnable, 0);
	}

	public static void runOnUiThread (Runnable runnable, long delay) {
		Handler handler = new Handler(Looper.getMainLooper());

		if (delay  > 0) {
			handler.postDelayed(runnable, delay);
		}
		else {
			handler.post(runnable);
		}
	}
}
