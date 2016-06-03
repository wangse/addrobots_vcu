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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class UsbCommunicationManager {
	static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	private static final int USB_RECIP_INTERFACE = 0x01;
	private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;

	private static final int SET_LINE_CODING = 0x20;  // USB CDC 1.1 section 6.2

	private static final int MAX_FRAME_BYTES = 256;

	private static final byte END = (byte) 0xC0;                        //0300; /* indicates end of packet */
	private static final byte ESC = (byte) 0xDB;                        //0333; /* indicates byte stuffing */
	private static final byte ESC_END = (byte) 0xDC;                        //0334; /* ESC ESC_END means END data byte */
	private static final byte ESC_ESC = (byte) 0xDD;                        //0335; /* ESC ESC_ESC means ESC data byte */

	private Boolean shouldRun = Boolean.FALSE;
	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private UsbInterface intf = null;
	private UsbEndpoint input, output;
	private UsbDeviceConnection connection;
	private PendingIntent permissionIntent;
	private Context context;
	private byte[] recvBuffer = new byte[MAX_FRAME_BYTES];
	private int recvBufferSize;
	private int recvBufferOffset;

	public UsbCommunicationManager(Context context) {
		this.context = context;
		usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

		// ask permission from user to use the usb device
		permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		context.registerReceiver(usbReceiver, filter);
	}

	public void connect() {
		// check if there's a connected usb device
		if (usbManager.getDeviceList().isEmpty()) {
			Log.d("USB", "No connected devices");
			return;
		}

		// get the first (only) connected device
		usbDevice = usbManager.getDeviceList().values().iterator().next();

		// user must approve of connection
		usbManager.requestPermission(usbDevice, permissionIntent);

		shouldRun = Boolean.TRUE;
	}

	public void stop() {
		context.unregisterReceiver(usbReceiver);
	}

	public byte[] receiveFrame() {
		byte[] frameBuffer = new byte[MAX_FRAME_BYTES];
		byte nextByte;
		int bytesReceived = 0;

		// Loop reading bytes until we put together  a whole packet.
		loop:
		while (shouldRun()) {
			nextByte = readByte();

			if (shouldRun()) {
				switch (nextByte) {

					// if it's an END character then we're done with the packet.
					case END:
						if (bytesReceived > 0) {
							break loop;
							//return result;
						} else {
							break;
						}

						/*
						 * If it's the same code as an ESC character, wait
						 * and get another character and then figure out
						 * what to store in the packet based on that.
						 */
					case ESC:
						nextByte = readByte();

						/* If "c" is not one of these two, then we
						 * have a protocol violation.  The best bet
						 * seems to be to leave the byte alone and
						 * just stuff it into the packet
						 */
						switch (nextByte) {
							case ESC_END:
								nextByte = END;
								break;
							case ESC_ESC:
								nextByte = ESC;
								break;
							default:
								break;
						}

					default:
						// here we fall into the default handler and let it store the character for us.
						try {
							frameBuffer[bytesReceived++] = (byte) nextByte;
						} catch (ArrayIndexOutOfBoundsException e) {
							// Note the error and send the full frame up for handling.
							Log.d("USB", "Serial framing error", e);
							bytesReceived = MAX_FRAME_BYTES;
							break loop;
						}
				}
			}
		}

		// Create a byte array that is exactly the right size.
		byte[] result = new byte[bytesReceived];
		System.arraycopy(frameBuffer, 0, result, 0, bytesReceived);

		return result;
	}

	public void sendFrame(ByteArrayOutputStream inByteArrayStream) {
		byte[] frameBytes = inByteArrayStream.toByteArray();
		byte[] buffer = new byte[MAX_FRAME_BYTES];
		int bufPos = 0;
		int bytesToSend = Math.min(MAX_FRAME_BYTES, frameBytes.length);

		if (frameBytes.length > bytesToSend) {
			Log.e("USB", "Packet contains more bytes than can fit on radio!");
		}

		for (int i = 0; i < bytesToSend; i++) {
			byte nextByte = frameBytes[i];

			if (nextByte == ESC) {
				buffer[bufPos++] = ESC;
				buffer[bufPos++] = ESC_ESC;
			} else if (nextByte == END) {
				buffer[bufPos++] = ESC;
				buffer[bufPos++] = ESC_END;
			} else {
				buffer[bufPos++] = nextByte;
			}
		}
		buffer[bufPos++] = END;

		//clrRTS();
		writeBytes(buffer, bufPos);
		//setRTS();

		try {
			hexDumpArray(frameBytes);
		} catch (Exception e) {
			Log.e("USB", e.getMessage());
		}
	}

	private Boolean writeBytes(byte[] data, int len) {
		if (usbDevice == null) {
			return Boolean.FALSE;
		}

		int sentBytes = 0;
		synchronized (this) {
			while (sentBytes < len) {
				// send data to usb device
				sentBytes += connection.bulkTransfer(output, data, sentBytes, data.length, 1000);
			}
		}

		return (sentBytes == len);
	}

	private byte readByte() {
		if (recvBufferOffset == recvBufferSize) {
			// wait for some data from the mcu
			while (recvBufferSize < 1) {
				recvBufferSize = connection.bulkTransfer(input, recvBuffer, recvBufferOffset, recvBuffer.length - recvBufferOffset, 3000);
			}
			recvBufferOffset = 0;
		}

		return recvBuffer[recvBufferOffset++];
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

	private void setupConnection() {
		// find the right interface
		for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
			// communications device class (CDC) type device
			if (usbDevice.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
				intf = usbDevice.getInterface(i);

				// find the endpoints
				for (int j = 0; j < intf.getEndpointCount(); j++) {
					if (intf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT && intf.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						// from android to device
						output = intf.getEndpoint(j);
					}

					if (intf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN && intf.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						// from device to android
						input = intf.getEndpoint(j);
					}
				}
			}
		}
	}

	private Boolean shouldRun() {
		return shouldRun;
	}

	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				// broadcast is like an interrupt and works asynchronously with the class, it must be synced just in case
				synchronized (this) {
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						setupConnection();

						connection = usbManager.openDevice(usbDevice);
						connection.claimInterface(intf, true);

						// set flow control to 8N1 at 9600 baud
						int baudRate = 115200;
						byte stopBitsByte = 1;
						byte parityBitesByte = 0;
						byte dataBits = 8;
						byte[] msg = {
								(byte) (baudRate & 0xff),
								(byte) ((baudRate >> 8) & 0xff),
								(byte) ((baudRate >> 16) & 0xff),
								(byte) ((baudRate >> 24) & 0xff),
								stopBitsByte,
								parityBitesByte,
								(byte) dataBits
						};

						connection.controlTransfer(UsbConstants.USB_TYPE_CLASS | 0x01, 0x20, 0, 0, msg, msg.length, 5000);
						connection.controlTransfer(0x21, 0x22, 0x1, 0, null, 0, 0);
					} else {
						Log.d("USB", "Permission denied for USB device");
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				Log.d("USB", "USB device detached");
			}
		}
	};

	private int sendAcmControlMessage(int request, int value, byte[] buf) {
		return connection.controlTransfer(USB_RT_ACM, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
	}

	private int setAcmLineCoding(int bitRate, int stopBits, int parity, int dataBits) {
		byte[] msg = {
				(byte) (bitRate & 0xff),
				(byte) ((bitRate >> 8) & 0xff),
				(byte) ((bitRate >> 16) & 0xff),
				(byte) ((bitRate >> 24) & 0xff),

				(byte) stopBits,
				(byte) parity,
				(byte) dataBits};
		return sendAcmControlMessage(SET_LINE_CODING, 0, msg);
	}

	protected void hexDumpArray(byte[] inByteArray) {
		String text = "";
		int pos = 0;
		for (pos = 0; pos < inByteArray.length; pos++) {
			if (pos % 16 == 0) {
				if (text.length() > 0) {
					Log.d("USB", text);
				}
				text = String.format("\t%02x: ", pos);
			} else {
				text += " ";
			}
			text += String.format("%02x", inByteArray[pos]);
		}
		if (pos != 0) {
			Log.d("USB", text);
		}
	}

}