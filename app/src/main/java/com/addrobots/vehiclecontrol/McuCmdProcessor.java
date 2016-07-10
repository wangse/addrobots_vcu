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
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.addrobots.protobuf.McuCmdMsg;
import com.addrobots.protobuf.VcuCmdMsg;

public class McuCmdProcessor {

	private static final String TAG = "McuCmdProcessor";

	private Context context;
	private PidController pidController;
	private int messagesRcvd;

	public McuCmdProcessor(PidController pidController, Context context) {
		this.pidController = pidController;
		this.context = context;

		VcuCmdMsg.VcuWrapperMessage cmd = new VcuCmdMsg.VcuWrapperMessage();
		VcuCmdMsg.Drive drive = new VcuCmdMsg.Drive();
		drive.acceleration = 0.5;
		drive.distance = 5.0;
		drive.velocity = 0.5;
		cmd.setDrive(drive);
		pidController.processVcuCommand(cmd);
	}

	public Boolean processCommand(McuCmdMsg.McuWrapperMessage mcuCmd) {
		Boolean result = false;
		messagesRcvd++;
		pidController.processMcuCommand(mcuCmd);
		Intent intent = null;
		switch (mcuCmd.getMsgCase()) {
			case McuCmdMsg.McuWrapperMessage.MOTORCMD_FIELD_NUMBER:
				break;
			case McuCmdMsg.McuWrapperMessage.SENSORCMD_FIELD_NUMBER:
				if (mcuCmd.hasSensorCmd()) {
					result = true;
					if ((messagesRcvd % 90) == 0) {
						intent = new Intent(VcuActivity.VCU_CLEAR_SENSOR_DATA);
						LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

					}
					McuCmdMsg.SensorCmd sensorCmd = mcuCmd.getSensorCmd();
					if (sensorCmd.name.equals("OFX")) {
						intent = new Intent(VcuActivity.VCU_X_SENSOR_DATA);
						intent.putExtra(VcuActivity.VCU_X_SENSOR_DATA, "\n" + sensorCmd.value);
						LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
					} else if (sensorCmd.name.equals("OFY")) {
						intent = new Intent(VcuActivity.VCU_Y_SENSOR_DATA);
						intent.putExtra(VcuActivity.VCU_Y_SENSOR_DATA, "\n" + sensorCmd.value);
						LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
					} else if (sensorCmd.name.equals("OFQ")) {
						intent = new Intent(VcuActivity.VCU_Q_SENSOR_DATA);
						intent.putExtra(VcuActivity.VCU_Q_SENSOR_DATA, "\n" + sensorCmd.value);
						LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
					}
				}
				break;
		}
		return result;
	}
}

