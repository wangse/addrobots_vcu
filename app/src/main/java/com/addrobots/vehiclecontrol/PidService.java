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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.addrobots.protobuf.McuCmdMsg;
import com.addrobots.protobuf.VcuCmdMsg;

public class PidService extends Service {

	private Context context;
	private int messagesRcvd;
	private final IBinder pidServiceBinder = new PidServiceBinder();
	private VcuCmdMsg.VcuWrapperMessage activeVcuCommand;

	private ServiceConnection usbServiceConnection;
	private UsbService usbService;
	boolean usbServiceIsBound = false;

	public PidService() {
		context = this;
	}

	public void reset() {
		messagesRcvd = 0;
	}

	// This class allows us to bind the USB frame processor and Firebase Cloud Messaging.
	public class PidServiceBinder extends Binder {
		PidService getService() {
			return PidService.this;
		}
	}

	public Boolean processVcuCommand(VcuCmdMsg.VcuWrapperMessage vcuCmd) {
		Boolean result = false;
		switch (vcuCmd.getMsgCase()) {
			case HALT:
				VcuCmdMsg.Halt haltCmd = vcuCmd.getHalt();
				processHaltCmd(haltCmd);
				break;
			case DRIVE:
				result = true;
				VcuCmdMsg.Drive driveCmd = vcuCmd.getDrive();
				processDriveCmd(driveCmd);
				break;
			case ORBIT:
				break;
		}
		return result;
	}

	public Boolean processMcuCommand(String deviceId, McuCmdMsg.McuWrapperMessage mcuCmd) {
		Boolean result = false;
		messagesRcvd++;
		Intent intent = null;
		switch (mcuCmd.getMsgCase()) {
			case MOTORCMD:
				break;
			case SENSORCMD:
				result = true;
				if ((messagesRcvd % 60) == 0) {
					intent = new Intent(VcuActivity.VCU_CLEAR_SENSOR_DATA);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

				}
				McuCmdMsg.SensorCmd sensorCmd = mcuCmd.getSensorCmd();
				if (sensorCmd.getName().equals("OFX")) {
					intent = new Intent(VcuActivity.VCU_X_SENSOR_DATA);
					intent.putExtra(VcuActivity.VCU_X_SENSOR_DATA, "\n" + messagesRcvd);//sensorCmd.value);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				} else if (sensorCmd.getName().equals("OFY")) {
					intent = new Intent(VcuActivity.VCU_Y_SENSOR_DATA);
					intent.putExtra(VcuActivity.VCU_Y_SENSOR_DATA, "\n" + messagesRcvd);//sensorCmd.value);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				} else if (sensorCmd.getName().equals("OFQ")) {
					intent = new Intent(VcuActivity.VCU_Q_SENSOR_DATA);
					intent.putExtra(VcuActivity.VCU_Q_SENSOR_DATA, "\n" + messagesRcvd);//sensorCmd.value);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				}
				break;
		}
		return result;
	}

	public static final int FORWARD = 0;
	public static final int REVERSE = 1;
	private static final double WHEEL_ROT_PER_M = 1.0 / (0.15 * Math.PI);

	private void processDriveCmd(VcuCmdMsg.Drive driveCmd) {
		if (usbServiceIsBound) {
			double degPerSec = driveCmd.getVelocity() * WHEEL_ROT_PER_M * 360;
			double accDegPerSec = driveCmd.getAcceleration() * WHEEL_ROT_PER_M * 360;
			McuCmdMsg.MotorCmd motorCmd1 = McuCmdMsg.MotorCmd.newBuilder().setDir(FORWARD).setDegSec(degPerSec).setNum(1).setADegSec(accDegPerSec).build();
			McuCmdMsg.McuWrapperMessage mcuMsgCmd1 = McuCmdMsg.McuWrapperMessage.newBuilder().setMotorCmd(motorCmd1).build();

			McuCmdMsg.MotorCmd motorCmd2 = McuCmdMsg.MotorCmd.newBuilder().setDir(FORWARD).setDegSec(degPerSec).setNum(2).setADegSec(accDegPerSec).build();
			McuCmdMsg.McuWrapperMessage mcuMsgCmd2 = McuCmdMsg.McuWrapperMessage.newBuilder().setMotorCmd(motorCmd2).build();

			usbService.sendMcuCommandToAllDevices(mcuMsgCmd1);
			usbService.sendMcuCommandToAllDevices(mcuMsgCmd2);
		}
	}

	private void processHaltCmd(VcuCmdMsg.Halt haltCmd) {
		McuCmdMsg.MotorCmd motorCmd1 = McuCmdMsg.MotorCmd.newBuilder().setDir(FORWARD).setDegSec(0).setNum(1).setADegSec(2.0).build();
		McuCmdMsg.McuWrapperMessage mcuMsgCmd1 = McuCmdMsg.McuWrapperMessage.newBuilder().setMotorCmd(motorCmd1).build();

		McuCmdMsg.MotorCmd motorCmd2 = McuCmdMsg.MotorCmd.newBuilder().setDir(FORWARD).setDegSec(0).setNum(2).setADegSec(2.0).build();
		McuCmdMsg.McuWrapperMessage mcuMsgCmd2 = McuCmdMsg.McuWrapperMessage.newBuilder().setMotorCmd(motorCmd2).build();

		usbService.sendMcuCommandToAllDevices(mcuMsgCmd1);
		usbService.sendMcuCommandToAllDevices(mcuMsgCmd2);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		context = this.getApplicationContext();
		// Bind the PID controller to this USB frame processor so we can pass it messages.
		usbServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName className,
			                               IBinder service) {
				if (service.getClass().equals(UsbService.UsbServiceBinder.class)) {
					UsbService.UsbServiceBinder binder = (UsbService.UsbServiceBinder) service;
					usbService = binder.getService();
					usbServiceIsBound = true;
				}
			}

			public void onServiceDisconnected(ComponentName arg0) {
				usbServiceIsBound = false;
			}
		};
		// Bind to the USB service so that we can send it messages.
		Intent intent = new Intent(this, UsbService.class);
		bindService(intent, usbServiceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onDestroy() {
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return pidServiceBinder;
	}
}
