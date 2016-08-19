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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.List;

public class OpticalFlowService extends IntentService {

	private static final String TAG = "OpticalFLowService";

	static {
		System.loadLibrary("opencv_java3");
		System.loadLibrary("ffmpeg");
	}

	public static final int FRONT_CAMERA = 0;
	public static final int BACK_CAMERA = 1;

	private BaseLoaderCallback loaderCallback;
	private VideoCapture videoCapture;
	private Size previewSize;
	private Mat currentFrame;
	private Mat prevFrame;
	private Point prevMinLoc;
	private Mat matchResult;

	public OpticalFlowService() {
		this("Optical Flow");
	}

	public OpticalFlowService(String threadName) {
		super(threadName);
		matchResult = new Mat();
		currentFrame = new Mat();
		loaderCallback = new LoaderCallback(this);
	}

	private void startCameraThread() {
		Thread cameraThread = new Thread("Camera Thread") {
			@Override
			public void run() {
				while (true) {
//					if (videoCapture.isOpened()) {
					if ((videoCapture.grab()) && (videoCapture.retrieve(currentFrame))) {
						if (prevFrame != null) {
							Imgproc.matchTemplate(currentFrame, prevFrame, matchResult, Imgproc.TM_CCORR_NORMED);
							Core.MinMaxLocResult match = Core.minMaxLoc(matchResult);
							if ((prevMinLoc != null) && (match.minLoc != null)) {
								double moveX = prevMinLoc.x - match.minLoc.x;
								double moveY = prevMinLoc.y - match.minLoc.y;
								prevMinLoc = match.minLoc;
							}
						}
						if (currentFrame.cols() > 0) {
							int xPixels = currentFrame.cols() / 2;
							int yPixels = currentFrame.rows() / 2;
							Rect rect = new Rect(xPixels - 20, yPixels - 20, xPixels + 20, yPixels + 20);
							prevFrame = new Mat(currentFrame, rect);
						}
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//				    }
				}
			}
		};

		cameraThread.start();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "SysLibPath: " + System.getProperty("java.library.path"));
		Log.d(TAG, "SysLibPath: " + this.getApplicationContext().getApplicationInfo().nativeLibraryDir);
		if (!OpenCVLoader.initDebug()) {
			Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
		} else {
			Log.d(TAG, "OpenCV library found inside package. Using it!");
			loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
//		videoCapture = new VideoCapture(0);//Videoio.CV_CAP_ANDROID_BACK + 1);
//		videoCapture.open(0+1);//Videoio.CV_CAP_ANDROID_BACK + 1);

//		List<Size> previewSizes = videoCapture.
//		double smallestPreviewSize = 1280 * 720; // We should be smaller than this...
//		double smallestWidth = 480; // Let's not get smaller than this...
//		for (Size previewSize : previewSizes) {
//			if (previewSize.area() < smallestPreviewSize && previewSize.width >= smallestWidth) {
//				this.previewSize = previewSize;
//			}
//		}
		startCameraThread();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

	}

	private class LoaderCallback extends BaseLoaderCallback {

		public LoaderCallback(Context context) {
			super(context);
		}

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");
					videoCapture = new VideoCapture(Videoio.CV_CAP_FFMPEG);
					//videoCapture.open(0+1);//Videoio.CV_CAP_ANDROID_BACK + 1);
				}
				break;
				default: {
					super.onManagerConnected(status);
				}
				break;
			}
		}
	}

	;
}
