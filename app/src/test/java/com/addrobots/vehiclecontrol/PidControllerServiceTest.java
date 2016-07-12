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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.test.RenamingDelegatingContext;

import com.addrobots.protobuf.McuCmdMsg;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PidControllerServiceTest {

	private Boolean ofxBroadcastSeen = false;
	private Boolean ofyBroadcastSeen = false;
	private Boolean ofqBroadcastSeen = false;

	private Context context;
	private PidControllerService pidControllerService;

	@Before
	public void init() {
		context = Robolectric.application.getApplicationContext(); //mock(Context.class);
		pidControllerService = new PidControllerService();
	}

	@Test
	public void testUsbProcessor() throws Exception {

		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				switch (intent.getAction()) {
					case VcuActivity.VCU_X_SENSOR_DATA:
						ofxBroadcastSeen = true;
						break;
					case VcuActivity.VCU_Y_SENSOR_DATA:
						ofyBroadcastSeen = true;
						break;
					case VcuActivity.VCU_Q_SENSOR_DATA:
						ofqBroadcastSeen = true;
						break;
				}
			}
		};

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(VcuActivity.VCU_CLEAR_SENSOR_DATA);
		intentFilter.addAction(VcuActivity.VCU_X_SENSOR_DATA);
		intentFilter.addAction(VcuActivity.VCU_Y_SENSOR_DATA);
		intentFilter.addAction(VcuActivity.VCU_Q_SENSOR_DATA);
		intentFilter.addAction(UsbBackgroundService.BGSVC_USB_DEVICE_LIST);
		LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);

		// Test a OFX sensor command from raw bytes.
		byte[] cmdBytes = {0x12, 0x11, 0x0A, 0x03, 0x4F, 0x46, 0x58, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF0, 0x3F, 0x1A, 0x01, 0x50};
		McuCmdMsg.McuWrapperMessage mcuCmd = McuCmdMsg.McuWrapperMessage.parseFrom(cmdBytes);
		assertTrue(pidControllerService.processMcuCommand(mcuCmd));
		assertTrue(ofxBroadcastSeen);
	}
}