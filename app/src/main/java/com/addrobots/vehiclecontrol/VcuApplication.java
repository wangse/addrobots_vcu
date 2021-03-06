/* BSD License
		Copyright (c) 2016, Jeff Williams
		All rights reserved.

		Redistribution and use in source and binary forms, with or without
		modification, are permitted provided that the following conditions are met:
		* Redistributions of source code must retain the above copyright
		notice, this list of conditions and the following disclaimer.
		* Redistributions in binary form must reproduce the above copyright
		notice, this list of conditions and the following disclaimer in the
		documentation and/or other materials provided with the distribution.
		* Neither the name of the <organization> nor the
		names of its contributors may be used to endorse or promote products
		derived from this software without specific prior written permission.

		THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
		ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
		WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
		DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
		DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
		(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
		LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
		ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
		(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
		SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.addrobots.vehiclecontrol;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.List;

public class VcuApplication extends Application {

	private static final String TAG = "VcuApplication";
	private UsbService.UsbServiceBinder usbServiceBinder;
	private ServiceConnection usbServiceConnection;
	private UsbService usbService;
	boolean usbServiceIsBound = false;

	public VcuApplication() {
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

//		FirebaseApp firebaseApp = FirebaseApp.getInstance(FirebaseApp.DEFAULT_APP_NAME);
//		if (firebaseApp != null) {
//			List<FirebaseApp> list = FirebaseApp.getApps(this);
//			list.remove(firebaseApp);
//			list = FirebaseApp.getApps(this);
//			if (list.contains(firebaseApp)) {
//
//			}
//			options = firebaseApp.getOptions();
//			if (options.getApiKey() != null) {
//				Log.d(TAG, "apikey: " + options.getApiKey().toString());
//				Log.d(TAG, "appid: " + options.getApplicationId().toString());
//				Log.d(TAG, "db: " + options.getDatabaseUrl().toString());
//				Log.d(TAG, "gcm: " + options.getGcmSenderId().toString());
//				Log.d(TAG, "bucket: " + options.getStorageBucket().toString());
//			}
//		}
//		FirebaseOptions options = new FirebaseOptions.Builder()
//				.setDatabaseUrl("https://addrobots.firebaseio.com")
//				.setApiKey("AIzaSyAjc99u3_RdL53ESiIcW5AlFELwBkdgC0w")
//				.setApplicationId("1:185039441716:android:c8526563e19cfafa")
//				.setGcmSenderId("185039441716")
//				.setStorageBucket("addrobots.appspot.com")
//				.build();
//		FirebaseApp.initializeApp(base, options, FirebaseApp.DEFAULT_APP_NAME);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startServices();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		stopServices();
	}

	private void startServices() {
		startService(new Intent(this.getApplicationContext(), PidService.class));
		startService(new Intent(this.getApplicationContext(), UsbService.class));
		startService(new Intent(this.getApplicationContext(), OpticalFlowService.class));
//		startServices(new Intent(this.getApplicationContext(), FirebaseMsgService.class));

		// We have to bind the USB service to *something* so that we can get it from a broadcastreceiver (later).
		usbServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className, IBinder service) {
				if ((service != null) && (service.getClass().equals(UsbService.UsbServiceBinder.class))) {
					usbServiceBinder = (UsbService.UsbServiceBinder) service;
					usbService = usbServiceBinder.getService();
					usbServiceIsBound = true;
				}
			}

			public void onServiceDisconnected(ComponentName arg0) {
				usbService = null;
				usbServiceIsBound = false;
			}
		};
		Intent intent = new Intent(this.getApplicationContext(), UsbService.class);
		bindService(intent, usbServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void stopServices() {
		stopService(new Intent(this.getBaseContext(), PidService.class));
		stopService(new Intent(this.getBaseContext(), UsbService.class));
		stopService(new Intent(this.getBaseContext(), OpticalFlowService.class));
//		stopServices(new Intent(this.getBaseContext(), FirebaseMsgService.class));
	}
}