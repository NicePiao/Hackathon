package com.baidu.hackathon;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.CameraProfile;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
	private Camera camera;

	private ICustomPicCallback customPicCallBackImpl;

	private enum CameraSize {
		VERY_HIGH, HIGH, MEDIUM, LOW;
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// 当surfaceview创建时开启相机
		if (camera == null) {
			initCamera();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// 当surfaceview关闭时，关闭预览并释放资源
		releaseCamera();
	}

	public void releaseCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	private AtomicBoolean isShutterFinished = new AtomicBoolean(true);

	// 快门
	public void shutter() {
		if (camera != null) {
			if (!isShutterFinished.get()) {
				return;
			}

			isShutterFinished.set(false);
			camera.autoFocus(new AutoFocusCallback() {

				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					try {
						camera.takePicture(null, null, new MyPictureCallback());// 将拍摄到的照片给自定义的对象
					} catch (Exception e) {
						e.printStackTrace();
						isShutterFinished.set(true);
					}
				}
			});

		} 
	}

	// 设置相机参数，并拍照
	private void setCameraParams() {
		Camera.Parameters parameters = camera.getParameters();

		parameters.set("iso", "auto");
		parameters.setAntibanding("auto");
		parameters.setPictureFormat(ImageFormat.JPEG);
		parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
		parameters.set("jpeg-quality", 100);
		parameters
				.setJpegQuality(CameraProfile.getJpegEncodingQualityParameter(
						0, CameraProfile.QUALITY_HIGH));

		setPreviewSize(parameters);
		setPictureSize(parameters);
		setFocusMode(parameters);
		camera.setParameters(parameters);
	}

	private void setFocusMode(Parameters params) {
		List<String> focusModes = params.getSupportedFocusModes();

		if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
			params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
	}

	/**
	 * Set the picture resolution to the camera
	 * 
	 * @param params
	 */
	private void setPictureSize(Camera.Parameters params) {
		// 设置图像尺寸 只能设置系统支持的尺寸 否则会报错
		List<Camera.Size> sizes = params.getSupportedPictureSizes();
		if (sizes != null && !sizes.isEmpty()) {
			Collections.sort(sizes, new Comparator<Camera.Size>() {
				@Override
				public int compare(Camera.Size lhs, Camera.Size rhs) {
					if ((lhs.width > rhs.width) && (lhs.height >= rhs.height))
						return -1;
					else if ((rhs.width > lhs.width)
							&& (rhs.height >= lhs.height))
						return 1;
					else if ((lhs.width == rhs.width)) {
						if (lhs.height > rhs.height)
							return -1;
						else if (rhs.height > lhs.height)
							return 1;
						else
							return 0;
					} else
						return 0;
				}
			});

			Camera.Size size = sizes
					.get(getCameraResAtPosition(CameraSize.VERY_HIGH));
			params.setPictureSize(size.width, size.height);
		}
	}

	/**
	 * Fetches the appropriate camera photo resolution setting
	 * 
	 * @param option
	 * @return
	 */
	private int getCameraResAtPosition(CameraSize option) {
		if (option == CameraSize.VERY_HIGH)
			return 0; // 2592x1944 Very High
		else if (option == CameraSize.HIGH)
			return 7; // 2048x1536 High
		else if (option == CameraSize.MEDIUM)
			return 13; // 1280x960 Medium
		else if (option == CameraSize.LOW)
			return 18; // 640x480 Low
		return 0;
	}

	private void setPreviewSize(Camera.Parameters params) {
		List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
		if (previewSizes != null && !previewSizes.isEmpty()) {
			Collections.sort(previewSizes, new Comparator<Camera.Size>() {
				@Override
				public int compare(Camera.Size lhs, Camera.Size rhs) {
					if ((lhs.width > rhs.width) && (lhs.height >= rhs.height))
						return -1;
					else if ((rhs.width > lhs.width)
							&& (rhs.height >= lhs.height))
						return 1;
					else if ((lhs.width == rhs.width)) {
						if (lhs.height > rhs.height)
							return -1;
						else if (rhs.height > lhs.height)
							return 1;
						else
							return 0;
					} else
						return 0;
				}
			});
		}

		Camera.Size size = previewSizes
				.get(getPreviewSizeAtPosition(CameraSize.VERY_HIGH));
		params.setPreviewSize(size.width, size.height);

	}

	/**
	 * Fetches the appropriate camera photo preview size setting
	 * 
	 * @param option
	 * @return
	 */
	private int getPreviewSizeAtPosition(CameraSize option) {
		if (option == CameraSize.VERY_HIGH)
			return 0; // 1920 - 1080 Very High
		else if (option == CameraSize.HIGH)
			return 0; // 1920 - 1080 High
		else if (option == CameraSize.MEDIUM)
			return 1; // 1280 - 720 Medium
		else if (option == CameraSize.LOW)
			return 18; // 320 - 240 Low
		return 0;
	}

	public void setCustomPicCallBackImpl(
			ICustomPicCallback customPicCallBackImpl) {
		this.customPicCallBackImpl = customPicCallBackImpl;
	}

	// 创建jpeg图片回调数据对象
	private class MyPictureCallback implements PictureCallback {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			if (camera == null)
				return;
			camera.stopPreview();

			if (customPicCallBackImpl != null) {
				customPicCallBackImpl.onPictureTaken(data, picSavePath);
			}
			
			isShutterFinished.set(true);
		}
	};

	public interface ICustomPicCallback {
		void onPictureTaken(byte[] data, String picSavePath);
	}

	private String picSavePath;

	public void setPicSavePath(String picSavePath) {
		this.picSavePath = picSavePath;
	}

	public Camera getCamera() {
		return camera;
	}

	public enum CameraType {
		CAMERA_FACING_FRONT, CAMERA_FACING_BACK
	}

	private CameraType cameraType = CameraType.CAMERA_FACING_BACK;

	public CameraType getCameraType() {
		return cameraType;
	}

	public void restartCamera() {

		releaseCamera();
		initCamera();
	}

	@SuppressLint("NewApi")
	private void initCamera() {

		try {
			CameraInfo cameraInfo = new CameraInfo();
			int cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数

			for (int i = 0; i < cameraCount; i++) {
				Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
				if ((cameraType == CameraType.CAMERA_FACING_BACK && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
						|| (cameraType == CameraType.CAMERA_FACING_FRONT && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)) {
					try {
						camera = Camera.open(i);// 打开当前选中的摄像头
					} catch (Exception e) {
						e.printStackTrace();
					}

					break;
				}
			}

			if (camera == null) {
				camera = Camera.open();
			}

			setCameraParams();

			camera.setPreviewDisplay(getHolder());// 通过surfaceview显示取景画面
			camera.startPreview();// 开始预览
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// 切换前后摄像头
	@SuppressLint("NewApi")
	public void changeCameraFace() {
		CameraInfo cameraInfo = new CameraInfo();
		int cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数

		for (int i = 0; i < cameraCount; i++) {
			Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
			if (cameraType == CameraType.CAMERA_FACING_BACK) {
				// 现在是后置，变更为前置
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置
																					// CAMERA_FACING_BACK后置
					releaseCamera();
					camera = Camera.open(i);// 打开当前选中的摄像头
					try {
						camera.setPreviewDisplay(getHolder());// 通过surfaceview显示取景画面
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					camera.startPreview();// 开始预览
					cameraType = CameraType.CAMERA_FACING_FRONT;
					break;
				}
			} else {
				// 现在是前置， 变更为后置
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					releaseCamera();
					camera = Camera.open(i);
					try {
						camera.setPreviewDisplay(getHolder());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					camera.startPreview();
					cameraType = CameraType.CAMERA_FACING_BACK;
					break;
				}
			}

		}
	}
}