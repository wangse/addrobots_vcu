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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class VcuActivity extends AppCompatActivity {

	private static final String TAG = "VcuActivity";

	private PendingIntent mPermissionIntent;
	private Button subscribeButton;
	private Button logTokenButton;
	private Button scanUsbButton;
	private Button connectButton;
	private TextView usbDeviceInfoText;
	private TextView sensorXText;
	private TextView sensorYText;
	private TextView sensorQText;

	private UsbProcessor usbProcessor;
	private McuCmdProcessor mcuCmdProcessor;

	private static final String ACTION_USB_PERMISSION = "com.addrobots.vehiclecontrol.USB_PERMISSION";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_vcu);
		usbDeviceInfoText = (TextView) findViewById(R.id.usb_info_textview);
		sensorXText = (TextView) findViewById(R.id.sensor_x_textview);
		sensorYText = (TextView) findViewById(R.id.sensor_y_textview);
		sensorQText = (TextView) findViewById(R.id.sensor_q_textview);

		scanUsbButton = (Button) findViewById(R.id.scan_usb_button);
		scanUsbButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				usbDeviceInfoText.setText(usbProcessor.listUsbDevices());
			}
		});
		connectButton = (Button) findViewById(R.id.usb_connect_button);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (usbProcessor.connect()) {
					usbProcessor.startCommandTask(mcuCmdProcessor);
				}
			}
		});

		subscribeButton = (Button) findViewById(R.id.subscribe_button);
		subscribeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// [START subscribe_topics]
				FirebaseMessaging.getInstance().subscribeToTopic("robot");
				Log.d(TAG, "Subscribed to robot topic");
				// [END subscribe_topics]
			}
		});

		logTokenButton = (Button) findViewById(R.id.log_token_button);
		logTokenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "InstanceID token: " + FirebaseInstanceId.getInstance().getToken());
			}
		});

		usbProcessor = new UsbProcessor(this);
		PidController pidController = new PidController();
		mcuCmdProcessor = new McuCmdProcessor(pidController, this);
	}

	public void xSensorDisplay(String text) {
		sensorXText.append(text);
	}

	public void ySensorDisplay(String text) {
		sensorYText.append(text);
	}

	public void qSensorDisplay(String text) {
		sensorQText.append(text);
	}

	public void xSensorClear() {
		sensorXText.setText("");
	}

	public void ySensorClear() {
		sensorYText.setText("");
	}

	public void qSensorClear() {
		sensorQText.setText("");
	}
}
