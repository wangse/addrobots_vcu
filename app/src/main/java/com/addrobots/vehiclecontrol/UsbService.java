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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.addrobots.protobuf.McuCmdMsg;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.util.HashMap;
import java.util.Iterator;

public class UsbService extends Service {

	public static final String BGSVC_USB_DEVICE_LIST = "USB_DEVICE_LIST";

	private static final String TAG = "UsbService";

	private Boolean isCmdTaskRunning = false;
	private int startMode;
	private boolean allowRebind;
	private UsbDeviceFrameProcessor usbDeviceFrameProcessor;
	private PidService.PidServiceBinder pidServiceBinder;

	private final IBinder usbServiceBinder = new UsbServiceBinder();

	private ServiceConnection pidServiceConnection;
	private PidService pidService;
	boolean pidServiceIsBound = false;

	// This class allows us to bind the USB frame processor and Firebase Cloud Messaging.
	public class UsbServiceBinder extends Binder {
		UsbService getService() {
			return UsbService.this;
		}
	}

	public Boolean isCommandTaskRunning() {
		return isCmdTaskRunning;
	}

	public void startCommandTask() {
		if (isCmdTaskRunning == false) {
			isCmdTaskRunning = true;

			if (pidServiceIsBound) {
				pidService.reset();
				Thread usbThread = new Thread("USB frame processor") {
					public void run() {
						byte frameBytes[];
						while (isCmdTaskRunning) {
							frameBytes = usbDeviceFrameProcessor.receiveFrame();
							if (frameBytes.length > 0) {
								try {
									McuCmdMsg.McuWrapperMessage mcuCmd = McuCmdMsg.McuWrapperMessage.parseFrom(frameBytes);
									if (!pidService.processMcuCommand(mcuCmd)) {
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
			} else {
				Log.d(TAG, "PID controller not bound");
			}
		}
	}

	public void stopCommandTask() {
		isCmdTaskRunning = false;
	}

	public void createDevice(UsbManager usbManager, UsbDevice usbDevice) {
		usbDeviceFrameProcessor = new UsbDeviceFrameProcessor(this, usbManager, usbDevice);
		usbDeviceFrameProcessor.connect();
		startCommandTask();
	}

	public void destoryDevice(UsbManager usbManager, UsbDevice usbDevice) {
		stopCommandTask();
		if (usbDeviceFrameProcessor != null) {
			usbDeviceFrameProcessor.disconnect();
			Log.d(TAG, "USB device detached");

			Intent clearSamplesIntent = new Intent(VcuActivity.VCU_CLEAR_SENSOR_DATA);
			LocalBroadcastManager.getInstance(this).sendBroadcast(clearSamplesIntent);

			Intent usbDeviceListIntent = new Intent(UsbService.BGSVC_USB_DEVICE_LIST);
			usbDeviceListIntent.putExtra(UsbService.BGSVC_USB_DEVICE_LIST, UsbService.listUsbDevices(usbManager));
			LocalBroadcastManager.getInstance(this).sendBroadcast(usbDeviceListIntent);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Bind the PID controller to this USB frame processor so we can pass it messages.
		pidServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className,
			                               IBinder service) {
				pidServiceBinder = (PidService.PidServiceBinder) service;
				pidService = pidServiceBinder.getService();
				pidServiceIsBound = true;
			}

			public void onServiceDisconnected(ComponentName arg0) {
				pidService = null;
				pidServiceIsBound = false;
			}
		};

		// Bind to the PID service so that we can send it messages.
		Intent intent = new Intent(this, PidService.class);
		bindService(intent, pidServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return startMode;
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getComponent().getClassName().equals(UsbService.class.getName())) {
			return usbServiceBinder;
		} else {
			return pidServiceBinder;
		}
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

	}

	public static String listUsbDevices(UsbManager usbManager) {

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
