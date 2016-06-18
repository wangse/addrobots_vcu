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
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.addrobots.protobuf.McuCmdMsg;
import com.addrobots.protobuf.VcuCmdMsg;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.io.FileInputStream;

public class VcuActivity extends AppCompatActivity {

	private PendingIntent mPermissionIntent;
	private Button testButton;
	private Button connectButton;
	private Button sendButton;
	private TextView usbDeviceInfoText;
	private TextView sensorXText;
	private TextView sensorYText;
	private TextView sensorQText;
	private UsbDevice device;
	//private UsbManager manager;
	private UsbCommunicationManager usbManager;
	private Thread usbThread;
	private PidController pidController;
	private int messagesRcvd;

	private static final String ACTION_USB_PERMISSION = "com.addrobots.vehiclecontrol.USB_PERMISSION";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vcu);
		usbDeviceInfoText = (TextView) findViewById(R.id.usb_info_textview);
		sensorXText = (TextView) findViewById(R.id.sensor_x_textview);
		sensorYText = (TextView) findViewById(R.id.sensor_y_textview);
		sensorQText = (TextView) findViewById(R.id.sensor_q_textview);

		testButton = (Button) findViewById(R.id.usb_test_button);
		testButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				usbDeviceInfoText.setText("");
				checkInfo();
			}
		});
		connectButton = (Button) findViewById(R.id.usb_connect_button);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				usbManager.connect();
			}
		});
		sendButton = (Button) findViewById(R.id.usb_send_button);
		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				VcuCmdMsg.VcuWrapperMessage cmd = new VcuCmdMsg.VcuWrapperMessage();
				VcuCmdMsg.Drive drive = new VcuCmdMsg.Drive();
				drive.acceleration = 0.5;
				drive.distance = 5.0;
				drive.velocity = 0.5;
				cmd.setDrive(drive);
				pidController.setCurrentVcuCommand(cmd);
				startBackgroundReadTask();
			}
		});

		usbManager = new UsbCommunicationManager(this);

		pidController = new PidController();
	}

	private void checkInfo() {

		usbDeviceInfoText.setText(usbManager.listUsbDevices());
	}

	private void startBackgroundReadTask() {

		if (usbThread == null) {
			usbThread = new Thread("USB frame processor") {
				public void run() {
					byte frameBytes[];
					while (true) {
						frameBytes = usbManager.receiveFrame();
						if (frameBytes.length > 0) {
							try {
								messagesRcvd++;
								final McuCmdMsg.McuWrapperMessage mcuCmd = McuCmdMsg.McuWrapperMessage.parseFrom(frameBytes);
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										pidController.processMcuMessage(mcuCmd);
										switch (mcuCmd.getMsgCase()) {
											case McuCmdMsg.McuWrapperMessage.MOTORCMD_FIELD_NUMBER:
												break;
											case McuCmdMsg.McuWrapperMessage.SENSORCMD_FIELD_NUMBER:
												if ((messagesRcvd % 90) == 0) {
													sensorXText.setText("");
													sensorYText.setText("");
													sensorQText.setText("");
												}
												if (mcuCmd.hasSensorCmd()) {
													McuCmdMsg.SensorCmd sensorCmd = mcuCmd.getSensorCmd();
													if (sensorCmd.name.equals("OFX")) {
														sensorXText.append("\n" + sensorCmd.value);
													} else if (sensorCmd.name.equals("OFY")) {
														sensorYText.append("\n" + sensorCmd.value);
													} else if (sensorCmd.name.equals("OFQ")) {
														sensorQText.append("\n" + sensorCmd.value);
													}
												}
												break;
										}
									}

								});
							} catch (InvalidProtocolBufferNanoException e) {
								e.printStackTrace();
							}
						}

//						try {
//							Thread.sleep(1);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
					}
				}
			};
			usbThread.start();
		}
	}
}
