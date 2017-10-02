package kr.neolab.samplecode;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.R.attr.duration;

public class Util
{
	private static Toast toast = null;
	public static void showToast( Context context, final String msg )
	{
		if (toast != null)
		{
			toast.setText(msg);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.show();
		} else
		{
			toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
			toast.show();
		}
	}

	private final static String CACHE_DIR_NAME = "wenba_cache";
	public static final String moveAssestToCacheDir(Activity context, final List<String> assests){
		try {
			//File fDir = new File(context.getExternalCacheDir() + "/" + CACHE_DIR_NAME + "/");
			File fDir = new File("/storage/sdcard0/wenba/" + CACHE_DIR_NAME + "/");
			final String dirPath = fDir.getAbsolutePath() + "/";
			/*
			if (fDir.exists())
			{
				fDir.delete();
			}
			*/
			if (!fDir.exists()) {
				dirChecker(dirPath);
				for (int i = 0; i < assests.size(); i++) {
					final String curAssestName = assests.get(i);
					final String curAssestPath = dirPath + curAssestName;
					InputStream in = context.getAssets().open(curAssestName);
					OutputStream out = new FileOutputStream(new File(curAssestPath));
					byte[] buf = new byte[1024];
					int len = in.read(buf);
					while (len != -1) {
						out.write(buf);
						len = in.read(buf);
					}
					in.close();
					out.close();
				}
			}
			return dirPath;
		}catch (Exception e){
			return "";
		}
	}
	public static final String getFile(Activity context, String name) {
		File f = new File(context.getExternalCacheDir(), name);
		if (!f.exists()) {
			try {
				InputStream in = context.getAssets().open(name);
				OutputStream out = new FileOutputStream(f);
				byte[] buf = new byte[1024];
				int len = in.read(buf);
				while (len != -1) {
					out.write(buf);
					len = in.read(buf);
				}
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return f.getAbsolutePath();
	}

	private  static boolean zip2file(ZipInputStream zin ,FileOutputStream fout){
		try {
			byte[] buffer = new byte[2048];
			BufferedOutputStream bos = new BufferedOutputStream(fout, buffer.length);
			int size;
			while ((size = zin.read(buffer, 0, buffer.length)) != -1) {
				bos.write(buffer, 0, size);
			}
			//Close up shop..
			bos.flush();
			bos.close();

			fout.flush();
			return true;
		}catch (IOException e){
			return false;
		}
	}
	public static void unzip(String zipFile, String location) {
		try  {
			FileInputStream fin = new FileInputStream(zipFile);
			ZipInputStream zin = new ZipInputStream(fin);
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				Log.v("Decompress", "Unzipping " + ze.getName());

				if(ze.isDirectory()) {
					dirChecker(location + ze.getName());
				} else {
					FileOutputStream fout = new FileOutputStream(location + ze.getName());
					zip2file(zin,fout);
					zin.closeEntry();
					fout.close();
				}

			}
			zin.close();
		} catch(Exception e) {
			Log.e("Decompress", "unzip", e);
		}

	}

	private static void dirChecker(String dir) {
		File f = new File(dir);
		if(!f.exists()) {
			boolean success = f.mkdir();
			assert success;
		}
	}
}
