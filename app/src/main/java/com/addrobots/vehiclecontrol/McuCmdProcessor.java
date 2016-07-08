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

import com.addrobots.protobuf.McuCmdMsg;
import com.addrobots.protobuf.VcuCmdMsg;

public class McuCmdProcessor {

	private static final String TAG = "McuCmdProcessor";

	private PidController pidController;
	private VcuActivity vcuActivity;
	private int messagesRcvd;

	public McuCmdProcessor(PidController pidController, VcuActivity vcuActivity) {
		this.pidController = pidController;
		this.vcuActivity = vcuActivity;

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
		switch (mcuCmd.getMsgCase()) {
			case McuCmdMsg.McuWrapperMessage.MOTORCMD_FIELD_NUMBER:
				break;
			case McuCmdMsg.McuWrapperMessage.SENSORCMD_FIELD_NUMBER:
				if (mcuCmd.hasSensorCmd()) {
					result = true;
					if ((messagesRcvd % 90) == 0) {
						vcuActivity.xSensorClear();
						vcuActivity.ySensorClear();
						vcuActivity.qSensorClear();
					}
					McuCmdMsg.SensorCmd sensorCmd = mcuCmd.getSensorCmd();
					if (sensorCmd.name.equals("OFX")) {
						vcuActivity.xSensorDisplay("\n" + sensorCmd.value);
					} else if (sensorCmd.name.equals("OFY")) {
						vcuActivity.ySensorDisplay("\n" + sensorCmd.value);
					} else if (sensorCmd.name.equals("OFQ")) {
						vcuActivity.qSensorDisplay("\n" + sensorCmd.value);
					}
				}
				break;
		}
		return result;
	}
}
