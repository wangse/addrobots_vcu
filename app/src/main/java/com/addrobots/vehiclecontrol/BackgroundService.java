/*
  BSD License
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class BackgroundService extends Service {

	public static final String BGSVC_USB_CONNECT = "USB_CONNECT";
	public static final String BGSVC_USB_SCAN = "USB_SCAN";
	public static final String BGSVC_USB_DEVICE_LIST = "USB_DEVICE_LIST";

	private static final String TAG = "BackgroundService";

	private int startMode;
	private IBinder binder;
	private boolean allowRebind;
	private BroadcastReceiver receiver;
	private UsbProcessor usbProcessor;
	private McuCmdProcessor mcuCmdProcessor;

	private static final String ACTION_USB_PERMISSION = "com.addrobots.vehiclecontrol.USB_PERMISSION";

	@Override
	public void onCreate() {
		super.onCreate();

		Context context = this.getApplicationContext();
		usbProcessor = new UsbProcessor(context);
		PidController pidController = new PidController();
		mcuCmdProcessor = new McuCmdProcessor(pidController, context);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction() != null) {
					switch (intent.getAction()) {
						case BGSVC_USB_CONNECT:
							if (usbProcessor.connect()) {
								usbProcessor.startCommandTask(mcuCmdProcessor);
							}
							break;
						case BGSVC_USB_SCAN:
							Intent usbDeviceListIntent = new Intent(BackgroundService.BGSVC_USB_DEVICE_LIST);
							usbDeviceListIntent.putExtra(BGSVC_USB_DEVICE_LIST, usbProcessor.listUsbDevices());
							LocalBroadcastManager.getInstance(context).sendBroadcast(usbDeviceListIntent);
							break;
					}
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BackgroundService.BGSVC_USB_SCAN);
		intentFilter.addAction(BackgroundService.BGSVC_USB_CONNECT);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return startMode;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return allowRebind;
	}

	@Override
	public void onRebind(Intent intent) {

	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}
}
