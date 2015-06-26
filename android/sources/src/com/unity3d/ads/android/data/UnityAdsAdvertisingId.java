package com.unity3d.ads.android.data;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.unity3d.ads.android.UnityAdsDeviceLog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsAdvertisingId {
	private static UnityAdsAdvertisingId impl = null;
	private String advertisingIdentifier = null;
	private boolean limitedAdvertisingTracking = false;

	private static UnityAdsAdvertisingId getImpl() {
		if(impl == null) {
			impl = new UnityAdsAdvertisingId();
		}

		return impl;
	}

	public static void init(final Activity activity) {
		if(!getImpl().fetchAdvertisingId(activity)) {
			getImpl().fetchAdvertisingIdFallback(activity.getApplicationContext());
		}
	}

	public static String getAdvertisingTrackingId() {
		return getImpl().advertisingIdentifier;
	}

	public static boolean getLimitedAdTracking() {
		return getImpl().limitedAdvertisingTracking;
	}

	// Primary implementation

    private boolean fetchAdvertisingId(final Activity context) {
    	try {
    		Class<?> GooglePlayServicesUtil = Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
    		Method isGooglePlayServicesAvailable = GooglePlayServicesUtil.getMethod("isGooglePlayServicesAvailable", Context.class);
    		if(isGooglePlayServicesAvailable.invoke(null, context).equals(0)) { // ConnectionResult.SUCCESS
    			Class<?> AdvertisingClientId = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
        		Method getAdvertisingIdInfo = AdvertisingClientId.getMethod("getAdvertisingIdInfo", Context.class);
        		Object advertisingTrackingInfo = getAdvertisingIdInfo.invoke(null, context);

        		Class<?> Info = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");
        		Method getId = Info.getMethod("getId");
        		advertisingIdentifier = (String)getId.invoke(advertisingTrackingInfo);

        		Method isLimitAdTrackingEnabled = Info.getMethod("isLimitAdTrackingEnabled");
        		limitedAdvertisingTracking = (Boolean)isLimitAdTrackingEnabled.invoke(advertisingTrackingInfo);

        		return true;
    		} else {
    			UnityAdsDeviceLog.debug("Google Play Services not integrated, using fallback");
    			return false;
    		}
    	} catch(Exception e) {
    		UnityAdsDeviceLog.debug("Exception while trying to access Google Play Services " + e);
    		return false;
    	}
    }

    // Fallback implementation

	private void fetchAdvertisingIdFallback(Context context) {
		GoogleAdvertisingServiceConnection connection = new GoogleAdvertisingServiceConnection();
    	Intent localIntent = new Intent("com.google.android.gms.ads.identifier.service.START");
    	localIntent.setPackage("com.google.android.gms");
    	if(context.bindService(localIntent, connection, Context.BIND_AUTO_CREATE)) {
    		try {
    	    	GoogleAdvertisingInfo advertisingInfo = GoogleAdvertisingInfo.GoogleAdvertisingInfoBinder.Create(connection.getBinder());
    			advertisingIdentifier = advertisingInfo.getId();
    			limitedAdvertisingTracking = advertisingInfo.getEnabled(true);
    		} catch(Exception e) {
				UnityAdsDeviceLog.debug("Couldn't get advertising info");
    		} finally {
    			context.unbindService(connection);
    		}
    	}
	}

	private interface GoogleAdvertisingInfo extends IInterface {
		String getId() throws RemoteException;
		boolean getEnabled(boolean paramBoolean) throws RemoteException;

		abstract class GoogleAdvertisingInfoBinder extends Binder implements GoogleAdvertisingInfo {
			public static GoogleAdvertisingInfo Create(IBinder binder) {
				if(binder == null) return null;
				IInterface localIInterface = binder.queryLocalInterface("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
				if((localIInterface != null) && ((localIInterface instanceof GoogleAdvertisingInfo))) {
					return (GoogleAdvertisingInfo)localIInterface;
				}
				return new GoogleAdvertisingInfoImplementation(binder);
			}

			@SuppressWarnings("NullableProblems")
			public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
				switch (code) {
					case 1:
						data.enforceInterface("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
						String str1 = getId();
						reply.writeNoException();
						reply.writeString(str1);
						return true;
					case 2:
						data.enforceInterface("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
						boolean bool1 = 0 != data.readInt();
						boolean bool2 = getEnabled(bool1);
						reply.writeNoException();
						reply.writeInt(bool2 ? 1 : 0);
						return true;
				}
				return super.onTransact(code, data, reply, flags);
			}

			private static class GoogleAdvertisingInfoImplementation implements GoogleAdvertisingInfo {
				private final IBinder _binder;

				GoogleAdvertisingInfoImplementation(IBinder binder) {
					_binder = binder;
				}

				public IBinder asBinder() {
					return _binder;
				}

				public String getId() throws RemoteException {
					Parcel localParcel1 = Parcel.obtain();
					Parcel localParcel2 = Parcel.obtain();
					String str;
					try {
						localParcel1.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
						_binder.transact(1, localParcel1, localParcel2, 0);
						localParcel2.readException();
						str = localParcel2.readString();
					} finally {
						localParcel2.recycle();
						localParcel1.recycle();
					}
					return str;
				}

				public boolean getEnabled(boolean paramBoolean) throws RemoteException {
					Parcel localParcel1 = Parcel.obtain();
					Parcel localParcel2 = Parcel.obtain();
					boolean bool;
					try {
						localParcel1.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
						localParcel1.writeInt(paramBoolean ? 1 : 0);
						_binder.transact(2, localParcel1, localParcel2, 0);
						localParcel2.readException();
						bool = 0 != localParcel2.readInt();
					} finally {
						localParcel2.recycle();
						localParcel1.recycle();
					}
					return bool;
				}
			}
		}
	}

	private class GoogleAdvertisingServiceConnection implements ServiceConnection {
		boolean _consumed = false;
		private final BlockingQueue<IBinder> _binderQueue = new LinkedBlockingQueue<>();

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			try {
				_binderQueue.put(service);
			} catch (InterruptedException localInterruptedException) {
				UnityAdsDeviceLog.debug("Couldn't put service to binder que");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

		public IBinder getBinder() throws InterruptedException {
			if (_consumed) throw new IllegalStateException();
			_consumed = true;
			return _binderQueue.take();
		}
	}
}