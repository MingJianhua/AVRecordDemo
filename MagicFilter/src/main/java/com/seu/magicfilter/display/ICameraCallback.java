package com.seu.magicfilter.display;

import android.graphics.Bitmap;

public interface ICameraCallback {
	public void onImageReady(Bitmap bitmap);
	public void onARGBDataReady(byte[] bs);
	public void onYUV420DataReady(byte []data);
}
