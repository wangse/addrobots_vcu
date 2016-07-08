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

import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.widget.Button;
import android.widget.TextView;

import org.junit.Test;

public class VcuActivityTest extends ActivityInstrumentationTestCase2<VcuActivity> {

	public VcuActivityTest() {
		super(VcuActivity.class);
	}

	@Test
	public void testActivityExists() {
		VcuActivity activity = getActivity();
		assertNotNull(activity);
	}

	@Test
	public void testUsbScanButton() {
		VcuActivity activity = getActivity();

		final Button usbScanButton = (Button) activity.findViewById(R.id.scan_usb_button);

		getInstrumentation().runOnMainSync(new Runnable() {
			@Override
			public void run() {
				usbScanButton.requestFocus();
			}
		});
		TouchUtils.clickView(this, usbScanButton);
	}

	@Test
	public void testSensorXDisplay() {
		VcuActivity activity = getActivity();

		final TextView sensorXText = (TextView) activity.findViewById(R.id.sensor_x_textview);

		// Send string input value
		getInstrumentation().runOnMainSync(new Runnable() {
			@Override
			public void run() {
				sensorXText.requestFocus();
			}
		});

		getInstrumentation().waitForIdleSync();
		getInstrumentation().sendStringSync("SensorX");
		getInstrumentation().waitForIdleSync();
		assertEquals(sensorXText.getText(), "SensorX");
	}

	@Test
	public void testSensorYDisplay() {
		VcuActivity activity = getActivity();

		final TextView sensorYText = (TextView) activity.findViewById(R.id.sensor_y_textview);

		// Send string input value
		getInstrumentation().runOnMainSync(new Runnable() {
			@Override
			public void run() {
				sensorYText.requestFocus();
			}
		});

		getInstrumentation().waitForIdleSync();
		getInstrumentation().sendStringSync("SensorY");
		getInstrumentation().waitForIdleSync();
		assertEquals(sensorYText.getText(), "SensorY");
	}

	@Test
	public void testSensorQDisplay() {
		VcuActivity activity = getActivity();

		final TextView sensorQText = (TextView) activity.findViewById(R.id.sensor_q_textview);

		// Send string input value
		getInstrumentation().runOnMainSync(new Runnable() {
			@Override
			public void run() {
				sensorQText.requestFocus();
			}
		});

		getInstrumentation().waitForIdleSync();
		getInstrumentation().sendStringSync("SensorQ");
		getInstrumentation().waitForIdleSync();
		assertEquals(sensorQText.getText(), "SensorQ");
	}
}