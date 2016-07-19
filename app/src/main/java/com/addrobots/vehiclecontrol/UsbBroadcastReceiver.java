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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Receivers need to be short-lived, but we see a USB device we need a long-lived background service.
 * In addition, services cannot accept meta-data for device filtering (for permissions) only receivers and acitivities can do this.
 */
public class UsbBroadcastReceiver extends BroadcastReceiver {

	private static final String ACTION_USB_PERMISSION = "com.addrobots.vehiclecontrol.USB_PERMISSION";

	private static final String TAG = "UsbBroadcastReceiver";

	private UsbService.UsbServiceBinder usbServiceBinder;
	private ServiceConnection usbServiceConnection;
	private UsbService usbService;
	boolean usbServiceIsBound = false;

	public UsbBroadcastReceiver() {
		super();

		// Bind the PID controller to this USB frame processor so we can pass it messages.
		usbServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className, IBinder service) {
				usbServiceBinder = (UsbService.UsbServiceBinder) service;
				usbService = usbServiceBinder.getService();
				usbServiceIsBound = true;
			}

			public void onServiceDisconnected(ComponentName arg0) {
				usbService = null;
				usbServiceIsBound = false;
			}
		};
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		Intent peekIntent = new Intent(context.getApplicationContext(), UsbService.class);
		usbServiceBinder = (UsbService.UsbServiceBinder) peekService(context, peekIntent);
		if (usbServiceBinder != null) {
			usbService = usbServiceBinder.getService();
			UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice usbDevice = (UsbDevice) intent.getExtras().get(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//					usbServiceBinder.getService().createDevice(usbManager, usbDevice);
						usbService.createDevice(usbManager, usbDevice);
					} else {
						Log.d(TAG, "Permission denied for USB device");
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				UsbDevice usbDevice = (UsbDevice) intent.getExtras().get(UsbManager.EXTRA_DEVICE);
				if ((usbDevice.getVendorId() == context.getResources().getInteger(R.integer.add_robots_usb_vendorid))
						&& (usbDevice.getProductId() == context.getResources().getInteger(R.integer.bot_v1_usb_prodid))) {
					Log.d(TAG, "USB device attached:" + usbDevice.getDeviceName());

					if (!usbManager.hasPermission(usbDevice)) {
						PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
						usbManager.requestPermission(usbDevice, permissionIntent);
					} else {
//					usbServiceBinder.getService().createDevice(usbManager, usbDevice);
						usbService.createDevice(usbManager, usbDevice);
					}
					Intent usbDeviceListIntent = new Intent(UsbService.BGSVC_USB_DEVICE_LIST);
					usbDeviceListIntent.putExtra(UsbService.BGSVC_USB_DEVICE_LIST, UsbService.listUsbDevices(usbManager));
					LocalBroadcastManager.getInstance(context).sendBroadcast(usbDeviceListIntent);
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice usbDevice = (UsbDevice) intent.getExtras().get(UsbManager.EXTRA_DEVICE);
				if ((usbDevice.getVendorId() == context.getResources().getInteger(R.integer.add_robots_usb_vendorid))
						&& (usbDevice.getProductId() == context.getResources().getInteger(R.integer.bot_v1_usb_prodid))) {
//				usbServiceBinder.getService().destoryDevice(usbManager, usbDevice);
					usbService.destoryDevice(usbManager, usbDevice);
				}
			}
		}
	}
}