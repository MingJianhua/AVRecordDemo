package com.seu.magicfilter.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class SaveTask extends AsyncTask<Bitmap, Integer, String>{
	
	private onPictureSaveListener mListener;
	private Context mContext;
	private File mFile;
	public SaveTask(Context context, File file, onPictureSaveListener listener){
		this.mContext = context;
		this.mListener = listener;
		this.mFile = file;
	}
	
	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(final String result) {
		// TODO Auto-generated method stub
		if(result != null)
			MediaScannerConnection.scanFile(mContext,
	                new String[] {result}, null,
	                new MediaScannerConnection.OnScanCompletedListener() {
	                    @Override
	                    public void onScanCompleted(final String path, final Uri uri) {
	                        if (mListener != null) 
	                        	mListener.onSaved(result);                      
	                    }
            	});
	}

	@Override
	protected String doInBackground(Bitmap... params) {
		// TODO Auto-generated method stub
		if(mFile == null)
			return null;
		return saveBitmap(params[0]);
	}
	
	private String saveBitmap(Bitmap bitmap) {

		if (mFile.exists()) {
			mFile.delete();
		}
		try {
			FileOutputStream out = new FileOutputStream(mFile);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();
			bitmap.recycle();
			setPictureDegreeZero(mFile.toString());
			return mFile.toString();
		} catch (FileNotFoundException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
		} catch (IOException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
		}
		return null;
	}
	/**
	 * 将图片的旋转角度置为0  ，此方法可以解决某些机型拍照后图像，出现了旋转情况
	 *
	 * @Title: setPictureDegreeZero
	 * @param path
	 * @return void
	 * @date 2012-12-10 上午10:54:46
	 */
	private void setPictureDegreeZero(String path) {
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			Log.e("TAG", exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));
			// 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
			// 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
			exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "no");
			exifInterface.saveAttributes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public interface onPictureSaveListener{
		void onSaved(String result);
	}
}
