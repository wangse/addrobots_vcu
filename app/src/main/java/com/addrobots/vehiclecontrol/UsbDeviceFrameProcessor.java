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

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class UsbDeviceFrameProcessor {

	public static final byte END = (byte) 0xC0;                        //0300; /* indicates end of packet */
	public static final byte ESC = (byte) 0xDB;                        //0333; /* indicates byte stuffing */
	public static final byte ESC_END = (byte) 0xDC;                        //0334; /* ESC ESC_END means END data byte */
	public static final byte ESC_ESC = (byte) 0xDD;                        //0335; /* ESC ESC_ESC means ESC data byte */

	private static final String TAG = "UsbDeviceFrameProcessor";
	private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";

	private static final int USB_RECIP_INTERFACE = 0x01;
	private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;
	private static final int SET_LINE_CODING = 0x20;  // USB CDC 1.1 section 6.2
	private static final int MAX_FRAME_BYTES = 256;

	private Boolean isConnected = false;
	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private UsbInterface usbInterface;
	private UsbEndpoint input, output;
	private UsbDeviceConnection usbDeviceConnection;
	private Context context;
	private byte[] recvBuffer = new byte[MAX_FRAME_BYTES];
	private int recvBufferSize;
	private int recvBufferOffset;

	public UsbDeviceFrameProcessor(Context context, UsbManager usbManager, UsbDevice usbDevice) {
		this.context = context;
		this.usbDevice = usbDevice;
		this.usbManager = usbManager;
		// Request permission to access the device.
		setupConnection();
	}

	public Boolean connect() {
		Boolean result = true;

		usbDeviceConnection = usbManager.openDevice(usbDevice);
		usbDeviceConnection.claimInterface(usbInterface, true);

		// set flow control to 8N1 at 115200 baud
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

		usbDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_CLASS | 0x01, 0x20, 0, 0, msg, msg.length, 5000);
		usbDeviceConnection.controlTransfer(0x21, 0x22, 0x1, 0, null, 0, 0);

		isConnected = true;
		return result;
	}

	public void disconnect() {
		if (usbDeviceConnection != null) {
			usbDeviceConnection.close();
			usbDeviceConnection.releaseInterface(usbInterface);
			usbDeviceConnection = null;
		}
		isConnected = false;
	}

	public Boolean isConnected() {
		return isConnected;
	}

	public String getDeviceId() {
		return usbDevice.getSerialNumber();
	}

	public byte[] receiveFrame() {
		byte[] frameBuffer = new byte[MAX_FRAME_BYTES];
		byte nextByte;
		int bytesReceived = 0;

		// Loop reading bytes until we put together  a whole packet.
		loop:
		while (isConnected()) {
			nextByte = readByte();
			// Because we might leave readByte due to a disconnect.
			if (isConnected()) {
				switch (nextByte) {

					// if it's an END character then we're done with the packet.
					case END:
						if (bytesReceived > 0) {
							break loop;
							//return result;
						} else {
							break;
						}
					case ESC:
						nextByte = readByte();
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
						try {
							frameBuffer[bytesReceived++] = (byte) nextByte;
						} catch (ArrayIndexOutOfBoundsException e) {
							// Note the error and send the full frame up for handling.
							Log.d(TAG, "Serial framing error", e);
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

	public void sendFrame(byte[] frameBytes) {
//		byte[] frameBytes = inByteArrayStream.toByteArray();
		byte[] buffer = new byte[MAX_FRAME_BYTES];
		int bufPos = 0;
		int bytesToSend = Math.min(MAX_FRAME_BYTES, frameBytes.length);

		if (frameBytes.length > bytesToSend) {
			Log.e(TAG, "Packet contains more bytes than can fit on radio!");
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
			Log.e(TAG, e.getMessage());
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
				sentBytes += usbDeviceConnection.bulkTransfer(output, data, sentBytes, len, 1000);
			}
		}

		return (sentBytes == len);
	}

	private byte readByte() {
		if (recvBufferOffset == recvBufferSize) {
			// wait for some data from the mcu
			recvBufferOffset = 0;
			recvBufferSize = 0;
			while ((isConnected) && (recvBufferSize < 1)) {
				if (usbDeviceConnection != null) {
					recvBufferSize = usbDeviceConnection.bulkTransfer(input, recvBuffer, recvBufferOffset, recvBuffer.length - recvBufferOffset, 1000);
				}
			}
		}

		if (isConnected) {
			return recvBuffer[recvBufferOffset++];
		} else {
			// Might as well make it a frame end.
			return END;
		}
	}

	private int sendAcmControlMessage(int request, int value, byte[] buf) {
		return usbDeviceConnection.controlTransfer(USB_RT_ACM, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
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

	private void setupConnection() {
		// find the right interface
		for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
			// communications device class (CDC) type device
			if (usbDevice.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
				usbInterface = usbDevice.getInterface(i);

				// find the endpoints
				for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
					if (usbInterface.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT && usbInterface.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						// from android to device
						output = usbInterface.getEndpoint(j);
					}

					if (usbInterface.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN && usbInterface.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						// from device to android
						input = usbInterface.getEndpoint(j);
					}
				}
			}
		}
	}

	private void hexDumpArray(byte[] inByteArray) {
		String text = "";
		int pos = 0;
		for (pos = 0; pos < inByteArray.length; pos++) {
			if (pos % 16 == 0) {
				if (text.length() > 0) {
					Log.d(TAG, text);
				}
				text = String.format("\t%02x: ", pos);
			} else {
				text += " ";
			}
			text += String.format("%02x", inByteArray[pos]);
		}
		if (pos != 0) {
			Log.d(TAG, text);
		}
	}
}