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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.addrobots.protobuf.McuCmdMsg;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.util.HashMap;
import java.util.Iterator;

public class UsbBackgroundService extends Service {

	public static final String BGSVC_USB_CONNECT = "USB_CONNECT";
	public static final String BGSVC_USB_SCAN = "USB_SCAN";
	public static final String BGSVC_USB_DEVICE_LIST = "USB_DEVICE_LIST";

	private static final String TAG = "UsbBackgroundService";

	private Boolean isCmdTaskRunning = false;
	private int startMode;
	private Context context;
	private PidControllerService.PidControllerBinder binder;
	private boolean allowRebind;
	private BroadcastReceiver receiver;
	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private UsbDeviceFrameProcessor usbDeviceFrameProcessor;
	private BroadcastReceiver usbBroadcastReceiver;
	private PendingIntent permissionIntent;
	private ServiceConnection pidServiceConnection;
	private PidControllerService pidControllerService;
	boolean pidServiceIsBound = false;

	private static final String ACTION_USB_PERMISSION = "com.addrobots.vehiclecontrol.USB_PERMISSION";

	public Boolean isCommandTaskRunning() {
		return isCmdTaskRunning;
	}

	public void startCommandTask() {
		if (isCmdTaskRunning == false) {
			isCmdTaskRunning = true;

			// Bind to the PID service so that we can send it messages.
			Intent intent = new Intent(this, PidControllerService.class);
			bindService(intent, pidServiceConnection, Context.BIND_AUTO_CREATE);

			Thread usbThread = new Thread("USB frame processor") {
				public void run() {
					byte frameBytes[];
					while (isCmdTaskRunning) {
						frameBytes = usbDeviceFrameProcessor.receiveFrame();
						if (frameBytes.length > 0) {
							try {
								McuCmdMsg.McuWrapperMessage mcuCmd = McuCmdMsg.McuWrapperMessage.parseFrom(frameBytes);
								if (!pidServiceIsBound) {
									Log.d(TAG, "PID controller not bound");
								} else if (!pidControllerService.processMcuCommand(mcuCmd)) {
									Log.d(TAG, "Invalid Mcu command on USB");
								}
							} catch (InvalidProtocolBufferNanoException e) {
								e.printStackTrace();
							}
						}
					}
				}
			};
			usbThread.start();
		}
	}

	public void stopCommandTask() {
		isCmdTaskRunning = false;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		context = this.getApplicationContext();
		usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction() != null) {
					switch (intent.getAction()) {
						case BGSVC_USB_CONNECT:
							if (usbDeviceFrameProcessor.connect()) {
								startCommandTask();
							}
							break;
						case BGSVC_USB_SCAN:
							Intent usbDeviceListIntent = new Intent(UsbBackgroundService.BGSVC_USB_DEVICE_LIST);
							usbDeviceListIntent.putExtra(BGSVC_USB_DEVICE_LIST, listUsbDevices());
							LocalBroadcastManager.getInstance(context).sendBroadcast(usbDeviceListIntent);
							break;
					}
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UsbBackgroundService.BGSVC_USB_SCAN);
		intentFilter.addAction(UsbBackgroundService.BGSVC_USB_CONNECT);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);

		// Bind the PID controller to this USB frame processor so we can pass it messages.
		pidServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className,
			                               IBinder service) {
				binder = (PidControllerService.PidControllerBinder) service;
				pidControllerService = binder.getService();
				pidServiceIsBound = true;
			}

			public void onServiceDisconnected(ComponentName arg0) {
				pidControllerService = null;
				pidServiceIsBound = false;
			}
		};

		// Setup the USB broadcast receiver to get events from the bus.
		setupUsbBroadcastReceiver();
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

	private void setupUsbBroadcastReceiver() {
		if (usbBroadcastReceiver == null) {
			usbBroadcastReceiver = new BroadcastReceiver() {
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					if (ACTION_USB_PERMISSION.equals(action)) {
						synchronized (this) {
							if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
								usbDeviceFrameProcessor = new UsbDeviceFrameProcessor(context, usbManager, usbDevice);
								usbDeviceFrameProcessor.connect();
							} else {
								Log.d(TAG, "Permission denied for USB device");
							}
						}
					} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
						// get the first (only) connected device
						usbDevice = usbManager.getDeviceList().values().iterator().next();
						usbManager.requestPermission(usbDevice, permissionIntent);
						Log.d(TAG, "USB device attached");
						Intent usbDeviceListIntent = new Intent(UsbBackgroundService.BGSVC_USB_DEVICE_LIST);
						usbDeviceListIntent.putExtra(UsbBackgroundService.BGSVC_USB_DEVICE_LIST, listUsbDevices());
						LocalBroadcastManager.getInstance(context).sendBroadcast(usbDeviceListIntent);
					} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
						usbDeviceFrameProcessor.disconnect();
						Log.d(TAG, "USB device detached");
						Intent usbDeviceListIntent = new Intent(UsbBackgroundService.BGSVC_USB_DEVICE_LIST);
						usbDeviceListIntent.putExtra(UsbBackgroundService.BGSVC_USB_DEVICE_LIST, listUsbDevices());
						LocalBroadcastManager.getInstance(context).sendBroadcast(usbDeviceListIntent);
					}
				}
			};
		}
		// Ask permission from user to use the usb device
		permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
		intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		context.registerReceiver(usbBroadcastReceiver, intentFilter);
	}

	public String listUsbDevices() {

		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

		if (deviceList.size() == 0) {
			return "no usb devices found";
		}

		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		String returnValue = "";
		UsbInterface usbInterface;

		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			returnValue += "Name: " + device.getDeviceName();
			returnValue += "\nID: " + device.getDeviceId();
			returnValue += "\nProtocol: " + device.getDeviceProtocol();
			returnValue += "\nClass: " + device.getDeviceClass();
			returnValue += "\nSubclass: " + device.getDeviceSubclass();
			returnValue += "\nProduct ID: " + device.getProductId();
			returnValue += "\nVendor ID: " + device.getVendorId();
			returnValue += "\nMfg Name:" + device.getManufacturerName();
			returnValue += "\nDevice Name:" + device.getDeviceName();
			returnValue += "\nProduct Name:" + device.getProductName();
			returnValue += "\nInterface count: " + device.getInterfaceCount();

			for (int i = 0; i < device.getInterfaceCount(); i++) {
				usbInterface = device.getInterface(i);
				returnValue += "\n  Interface " + i;
				returnValue += "\n\tInterface ID: " + usbInterface.getId();
				returnValue += "\n\tClass: " + usbInterface.getInterfaceClass();
				returnValue += "\n\tProtocol: " + usbInterface.getInterfaceProtocol();
				returnValue += "\n\tSubclass: " + usbInterface.getInterfaceSubclass();
				returnValue += "\n\tEndpoint count: " + usbInterface.getEndpointCount();

				for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
					returnValue += "\n\t  Endpoint " + j;
					returnValue += "\n\t\tAddress: " + usbInterface.getEndpoint(j).getAddress();
					returnValue += "\n\t\tAttributes: " + usbInterface.getEndpoint(j).getAttributes();
					returnValue += "\n\t\tDirection: " + usbInterface.getEndpoint(j).getDirection();
					returnValue += "\n\t\tNumber: " + usbInterface.getEndpoint(j).getEndpointNumber();
					returnValue += "\n\t\tInterval: " + usbInterface.getEndpoint(j).getInterval();
					returnValue += "\n\t\tType: " + usbInterface.getEndpoint(j).getType();
					returnValue += "\n\t\tMax packet size: " + usbInterface.getEndpoint(j).getMaxPacketSize();
				}
			}
		}
		return returnValue;
	}
}
