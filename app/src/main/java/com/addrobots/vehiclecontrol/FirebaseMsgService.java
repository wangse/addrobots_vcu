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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.addrobots.protobuf.VcuCmdMsg;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.util.Map;

public class FirebaseMsgService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMsgService";
	private static final String VCU_CMD_TAG = "VCU_CMD";

	private ServiceConnection pidServiceConnection;
	private PidService pidService;
	boolean pidServiceIsBound = false;

	@Override
	public void onCreate() {
		super.onCreate();
		// Bind the PID controller to this USB frame processor so we can pass it messages.
		pidServiceConnection = new ServiceConnection() {

			public void onServiceConnected (ComponentName className,
			                                IBinder service){
				PidService.PidServiceBinder binder = (PidService.PidServiceBinder) service;
				pidService = binder.getService();
				pidServiceIsBound = true;
			}

			public void onServiceDisconnected(ComponentName arg0) {
				pidServiceIsBound = false;
			}
		};

	}

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        String body = notification.getBody();
	    Map<String, String> dataMap = remoteMessage.getData();
	    for (String data: dataMap.keySet()) {
		    if (data.equals(VCU_CMD_TAG)) {
			    try {
				    VcuCmdMsg.VcuWrapperMessage vcuCmd = VcuCmdMsg.VcuWrapperMessage.parseFrom(dataMap.get(data).getBytes());
				    if (!pidServiceIsBound) {
					    Log.d(TAG, "PID controller not bound");
				    } else if (!pidService.processVcuCommand(vcuCmd)) {
					    Log.d(TAG, "Invalid Vcu command on USB");
				    }
			    } catch (InvalidProtocolBufferNanoException e) {
				    e.printStackTrace();
			    }
		    }
		    Log.d(TAG, data);
		    Log.d(TAG, dataMap.get(data));
	    }
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification Message Body: " + body);

    }

    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, VcuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("FCM Message")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
