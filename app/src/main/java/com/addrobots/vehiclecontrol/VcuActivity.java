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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class VcuActivity extends AppCompatActivity {

	public static final String VCU_CLEAR_SENSOR_DATA = "VCU_CLEAR_SENSOR_DATA";
	public static final String VCU_X_SENSOR_DATA = "VCU_X_SENSOR_DATA";
	public static final String VCU_Y_SENSOR_DATA = "VCU_Y_SENSOR_DATA";
	public static final String VCU_Q_SENSOR_DATA = "VCU_Q_SENSOR_DATA";

	private static final String TAG = "VcuActivity";

	private Context context;
	private BroadcastReceiver receiver;
	private Button subscribeButton;
	private Button logTokenButton;
	private Button scanUsbButton;
	private Button connectButton;
	private TextView usbDeviceInfoText;
	private TextView sensorXText;
	private TextView sensorYText;
	private TextView sensorQText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				switch (intent.getAction()) {
					case UsbBackgroundService.BGSVC_USB_DEVICE_LIST:
						usbDeviceInfoText.setText(intent.getStringExtra(UsbBackgroundService.BGSVC_USB_DEVICE_LIST));
						break;
					case VcuActivity.VCU_CLEAR_SENSOR_DATA:
						sensorXText.setText("");
						sensorYText.setText("");
						sensorQText.setText("");
						break;
					case VcuActivity.VCU_X_SENSOR_DATA:
						sensorXText.setText(intent.getStringExtra(VcuActivity.VCU_X_SENSOR_DATA));
						break;
					case VcuActivity.VCU_Y_SENSOR_DATA:
						sensorXText.setText(intent.getStringExtra(VcuActivity.VCU_Y_SENSOR_DATA));
						break;
					case VcuActivity.VCU_Q_SENSOR_DATA:
						sensorXText.setText(intent.getStringExtra(VcuActivity.VCU_Q_SENSOR_DATA));
						break;
				}
			}
		};

		context = this.getApplicationContext();

		setContentView(R.layout.activity_vcu);
		usbDeviceInfoText = (TextView) findViewById(R.id.usb_info_textview);
		sensorXText = (TextView) findViewById(R.id.sensor_x_textview);
		sensorYText = (TextView) findViewById(R.id.sensor_y_textview);
		sensorQText = (TextView) findViewById(R.id.sensor_q_textview);

		scanUsbButton = (Button) findViewById(R.id.scan_usb_button);
		scanUsbButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(UsbBackgroundService.BGSVC_USB_SCAN);
				LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
			}
		});
		connectButton = (Button) findViewById(R.id.usb_connect_button);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(UsbBackgroundService.BGSVC_USB_CONNECT);
				LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(VcuActivity.VCU_CLEAR_SENSOR_DATA);
		intentFilter.addAction(VcuActivity.VCU_X_SENSOR_DATA);
		intentFilter.addAction(VcuActivity.VCU_Y_SENSOR_DATA);
		intentFilter.addAction(VcuActivity.VCU_Q_SENSOR_DATA);
		intentFilter.addAction(UsbBackgroundService.BGSVC_USB_DEVICE_LIST);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}
}
