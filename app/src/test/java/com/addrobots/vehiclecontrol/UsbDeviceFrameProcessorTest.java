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

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(manifest = "src/test/resources/robolectric/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class UsbDeviceFrameProcessorTest {

	private UsbDeviceFrameProcessor usbDeviceFrameProcessor;

	private final VcuActivity vcuActivity = mock(VcuActivity.class);
	private final Context context = mock(Context.class);
	private final UsbDevice usbDevice = mock(UsbDevice.class);
	private final UsbManager usbManager = mock(UsbManager.class);
	private final UsbDeviceConnection usbDeviceConnection = mock(UsbDeviceConnection.class);

	@Before
	public void setUp() throws Exception {
		when(vcuActivity.getApplicationContext()).thenReturn(context);
		when(context.getSystemService(Context.USB_SERVICE)).thenReturn(usbManager);
		usbDeviceFrameProcessor = new UsbDeviceFrameProcessor(vcuActivity.getApplicationContext(), usbManager, usbDevice);
	}

	@Test
	public void testUsbConnection() throws Exception {

		// Create a valid device list for the USB bus.
		HashMap<String, UsbDevice> deviceList = new HashMap<String, UsbDevice>();
		deviceList.put("Test", usbDevice);
		when(usbManager.getDeviceList()).thenReturn(deviceList);
		when(usbManager.openDevice(usbDevice)).thenReturn(usbDeviceConnection);

		usbDeviceFrameProcessor.connect();
		assertTrue(usbDeviceFrameProcessor.isConnected());
		usbDeviceFrameProcessor.disconnect();
		assertFalse(usbDeviceFrameProcessor.isConnected());

	}

	@Test
	public void testGetFrame() {

		// Create a valid device list for the USB bus.
		HashMap<String, UsbDevice> deviceList = new HashMap<String, UsbDevice>();
		deviceList.put("Test", usbDevice);
		when(usbManager.getDeviceList()).thenReturn(deviceList);
		when(usbManager.openDevice(usbDevice)).thenReturn(usbDeviceConnection);

		// While this may seem messy, the reality is that stream bytes emerge from system resources, not interfaces.
		byte[] cmdBytes = {0x12, 0x11, 0x0A, 0x03, 0x4F, 0x46, 0x58, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF0, 0x3F, 0x1A, 0x01, 0x50};
		byte[] frameBytes = new byte[cmdBytes.length + 5];
		System.arraycopy(cmdBytes, 0, frameBytes, 0, cmdBytes.length);
		// Add a frame end, and a few extra bytes to not invoke internal bulkTransfer calls.
		frameBytes[cmdBytes.length] = UsbDeviceFrameProcessor.END;
		frameBytes[cmdBytes.length + 1] = UsbDeviceFrameProcessor.END;
		frameBytes[cmdBytes.length + 2] = UsbDeviceFrameProcessor.END;

		try {
			Field field = UsbDeviceFrameProcessor.class.getDeclaredField("recvBuffer");
			field.setAccessible(true);
			field.set(usbDeviceFrameProcessor, frameBytes);

			field = UsbDeviceFrameProcessor.class.getDeclaredField("recvBufferSize");
			field.setAccessible(true);
			field.set(usbDeviceFrameProcessor, frameBytes.length);

			usbDeviceFrameProcessor.connect();
			byte[] frame = usbDeviceFrameProcessor.receiveFrame();
			assertArrayEquals(frame, cmdBytes);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}