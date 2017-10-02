package kr.neolab.samplecode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kr.neolab.samplecode.Const.Broadcast;
import kr.neolab.samplecode.Const.JsonTag;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.pen.offline.OfflineFileParser;
import kr.neolab.sdk.pen.penmsg.PenMsgType;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wenba100.ocr.Core;
import com.wenba100.ocr.Formula;

import static kr.neolab.samplecode.SettingActivity.LAYOUTMODE_FREE;
import static kr.neolab.samplecode.SettingActivity.LAYOUTMODE_PIGAI;


public class MainActivity extends Activity implements View.OnClickListener {
	public static final String TAG = "pensdk.sample";

	private static final int REQUEST_CONNECT_DEVICE_SECURE = 4;
	private static final int REQUEST_SETTING = 5;

	private PenClientCtrl penClientCtrl;

	private SampleView mSampleView;
	private MathJaxWebView question_details;
	private MathJaxWebView recognition_result;
	private ImageView pigai_result;

	//private ImageView debug_iamge_view;
	private String serverAPI = "";
	private final int LANGUAGE_ENG = 0;
	private final int LANGUAGE_CHI = 1;

	// Notification
	protected Builder mBuilder;
	protected NotificationManager mNotifyManager;
	protected Notification mNoti;

	public InputPasswordDialog inputPassDialog;

	private static final int PIGAI_RESULT_YES = 1;
	private static final int PIGAI_RESULT_NO = 3;
	private static final int PIGAI_RESULT_UNKNOWN = -1;

	private static final int RECOGNIZE_TEXT_ID = 0;
	private static final int RECOGNIZE_FORMULA_ID = 1;
	private static final int RECOGNIZE_PIGAI_ID = 2;
	private static final int FETCH_QUESTION_ID = 3;
	private int nextIdx = 0;
	private int item_id = 0;
	private String item_details = "";

	private final String font_prefix = "<font size=\"5\">";
	private final String font_postfix = "</font>";

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final boolean serverError = HttpUtils.exception_flag;
			if (serverError) {
				Util.showToast(MainActivity.this, "Internal Server Error");
				HttpUtils.exception_flag = false;
			}
			switch (msg.what) {
				case RECOGNIZE_TEXT_ID: {
					try {
						mSampleView.normalizeStrokes();
						if(!serverError) {
							final String result = (String) (msg.obj);
							JSONObject root = new JSONObject(result);
							final String result_text = root.get("recognize_result").toString();
							recognition_result.setText(result_text);
							Util.showToast(MainActivity.this, "Success");
							mSampleView.normalizeStrokes();
						}
					} catch (JSONException e) {
						e.printStackTrace();
						Util.showToast(MainActivity.this, "Recognition Error");
					}
					break;
				}
				case RECOGNIZE_FORMULA_ID:{
					try {
						mSampleView.normalizeStrokes();
						if(!serverError) {
							final String result = (String) (msg.obj);
							JSONObject root = new JSONObject(result);
							String result_text = "";
							result_text += "<p>Recognition Result: \\[" + root.get("parse_result") + "\\]</p><hr/>";
							result_text += "<p>Computing Result: \\[" + root.get("answer") + "\\]</p><hr/>";
							result_text += "<p>Formula Type：" + root.get("expr_type") + "</p><hr/>";
							recognition_result.setText(result_text);
							Util.showToast(MainActivity.this, "Success");
							mSampleView.normalizeStrokes();
						}
					} catch (JSONException e) {
						e.printStackTrace();
						Util.showToast(MainActivity.this, "Recognition Error");
					}
					break;
				}
				case FETCH_QUESTION_ID: {
					final String result = (String) (msg.obj);
					try {
						recognition_result.setText("");
						if(serverError){
							final String full_info  = font_prefix + "<b>Question: </b><br/>Server Connecting Error<hr/><b>Answer: </b><br/>Server Connecting Error<hr/>" + font_postfix;
							question_details.setText(full_info);
							item_details = "";
						}else {
							JSONObject root = new JSONObject(result);
							nextIdx = root.getInt("idx");
							item_id = root.getInt("item_id");
							final String item_content = root.getString("item_content");
							final String item_answer = root.getString("item_answer");
							item_details = font_prefix + "<b>Question: </b><br/>" + item_content + "<hr/><b>Answer: </b><br/>" + item_answer + font_postfix;
							question_details.setText(item_details);
							Util.showToast(MainActivity.this, "Success");
							mSampleView.clearView();

							Button btnSubmitPigai = (Button) findViewById(R.id.buttonSubmitPigai);
							btnSubmitPigai.setEnabled(true);
						}
					} catch (JSONException ex) {
						ex.printStackTrace();
						Util.showToast(MainActivity.this, "Fail to get question");
						final String full_info  = font_prefix + "<b>Question: </b><br/>Failed.<hr/><b>Answer: </b><br/>Failed<hr/>" + font_postfix;
						question_details.setText(full_info);
						item_details = "";
					}
					break;
				}
				case RECOGNIZE_PIGAI_ID:{
					final String result = (String) (msg.obj);
					try {
						mSampleView.normalizeStrokes();
						if(serverError) {
							pigai_result.setImageDrawable(getResources().getDrawable(R.drawable.init));
						}else{
							JSONObject root = new JSONObject(result);

							//score:
							String scoreInfo = "";
							final int score = root.getInt("score");
							switch (score){
								case PIGAI_RESULT_YES:
									pigai_result.setImageDrawable(getResources().getDrawable(R.drawable.right));
									scoreInfo = "Bingo";
									break;
								case PIGAI_RESULT_NO:
									pigai_result.setImageDrawable(getResources().getDrawable(R.drawable.error));
									scoreInfo = "Wrong";
									break;
								case PIGAI_RESULT_UNKNOWN:
									pigai_result.setImageDrawable(getResources().getDrawable(R.drawable.weizhi));
									scoreInfo = "Correcting Failed";
									break;
								default:
									Util.showToast(MainActivity.this, "Unknown Value" + score);
									scoreInfo = "Return Error"+score;
									break;
							}

							final String result_text = root.getString("recognition_text");
							recognition_result.setText(font_prefix + result_text + font_postfix);

							Util.showToast(MainActivity.this, "Correcting Result: " + scoreInfo);
							mSampleView.normalizeStrokes();
						}
					} catch (JSONException ex) {
						ex.printStackTrace();
					}
					break;
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		IntentFilter filter = new IntentFilter(Broadcast.ACTION_PEN_MESSAGE);
		filter.addAction(Broadcast.ACTION_PEN_DOT);
		filter.addAction("firmware_update");

		registerReceiver(mBroadcastReceiver, filter);

		setContentView(R.layout.activity_main);

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("firmware_update"), PendingIntent.FLAG_UPDATE_CURRENT);

		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setContentTitle("Update Pen");
		mBuilder.setSmallIcon(R.drawable.icon);
		mBuilder.setContentIntent(pendingIntent);

		penClientCtrl = PenClientCtrl.getInstance(getApplicationContext());//synchronize

		mSampleView = new SampleView(this);
		LinearLayout frm=(LinearLayout)findViewById(R.id.layout_writing_area);
		frm.addView(mSampleView);

		Button btnSubmitEng = (Button)findViewById(R.id.buttonSubmitEng);
		btnSubmitEng.setOnClickListener(this);

		Button btnSubmitChi = (Button)findViewById(R.id.buttonSubmitChi);
		btnSubmitChi.setOnClickListener(this);

		Button btnSubmitFormula = (Button)findViewById(R.id.buttonSubmitFormula);
		btnSubmitFormula.setOnClickListener(this);

		Button btnSubmitNext = (Button)findViewById(R.id.buttonSubmitNext);
		btnSubmitNext.setOnClickListener(this);

		Button btnSubmitPigai = (Button)findViewById(R.id.buttonSubmitPigai);
		btnSubmitPigai.setOnClickListener(this);
		btnSubmitPigai.setEnabled(false);

		Button btnClear = (Button)findViewById(R.id.buttonClear);
		btnClear.setOnClickListener(this);

		question_details = (MathJaxWebView)findViewById(R.id.result_view);

		recognition_result = (MathJaxWebView)findViewById(R.id.recognition_result);

		pigai_result = (ImageView)findViewById(R.id.pigai_result);

		//init ocr engine
		//progress
		Util.showToast(this,"Loading Model");
		/*
		List<String> assets = new ArrayList<String>();
		assets.add("chiCRNN.model");
		assets.add("engCRNN.model");
		final String modelDirPath = Util.moveAssestToCacheDir(this,assets);
		if (modelDirPath.isEmpty()){
			Util.showToast(this,"Cannot Find Model File");
			finish();
		}
		boolean success = Core.initEngine(modelDirPath);
        if (!success){
            Util.showToast(this,"Loading OCR Model Failed");
        }

        //init formula engine
        final String zippedFile = Util.getFile(this,"Config.zip");
        Util.unzip(zippedFile,modelDirPath);
        success = Formula.initFormulaEngine(modelDirPath);
        if (!success){
            Util.showToast(this,"Loading Formula Model Failed");
        }

        if(success)
        {
            Util.showToast(this,"Finished");
        }
		*/

		showServerStatus();
		updateLayoutStatus();

		mSampleView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
									   int oldBottom) {
				// its possible that the layout is not complete in which case
				// we will get all zero values for the positions, so ignore the event
				if (left == 0 && top == 0 && right == 0 && bottom == 0) {
					return;
				}

				// Do what you need to do with the height/width since they are now set
				//mSampleView.setPageSize(right-left,bottom-top);
			}
		});

		SharedPreferences sp = getSharedPreferences(SettingActivity.app_setting_tag, Context.MODE_PRIVATE);
		final int layoutMode = sp.getInt(SettingActivity.LAYOUTMODE_TAG,SettingActivity.defaultLayoutMode);
		if(layoutMode == LAYOUTMODE_PIGAI) {
			Util.showToast(this, "Getting Question");
			fetchNextQuestion();
		}
	}
	private void recognizeText(final ArrayList<ArrayList<Integer>> strokes, final int language) {
		new Thread() {
			@Override
			public void run() {
				String result="";
				final String strokesJson = strokeToJson(strokes);
				if(serverAPI.equals(SettingActivity.localAPI)){
					//call local recognizer
					if (language == LANGUAGE_ENG) {
						result = Core.recognizeEng(strokesJson);
					} else if (language == LANGUAGE_CHI) {
						result = Core.recognizeChi(strokesJson);
					}
				}else {
					//call remote recognizer
					Map<String, String> params = new HashMap<String, String>();
					params.put("strokes", strokesJson);
					String url = serverAPI;
					if (language == LANGUAGE_ENG) {
						params.put("language", "eng");
						url += "/api/recognition/online";
					} else if (language == LANGUAGE_CHI) {
						params.put("language", "chi");
						url += "/api/recognition/online";
					}
					result = HttpUtils.submitPostData(url, params, "utf-8");
				}
				//send message
				Message tempMsg = mHandler.obtainMessage();
				tempMsg.what = RECOGNIZE_TEXT_ID;
				tempMsg.obj = result;
				mHandler.sendMessage(tempMsg);
			}
		}.start();
	}
	private void recognizeFormula(final ArrayList<ArrayList<Integer>> strokes) {
		if(serverAPI.equals(SettingActivity.localAPI)) {
			Util.showToast(this,"Formula Recognition Cannot Support Offline Mode");
			return;
		}
		new Thread() {
			@Override
			public void run() {
				String result="";
				if(serverAPI.equals(SettingActivity.localAPI)){
					//call local recognizer
					final String strokesStr = strokeToString(strokes);
					result = Formula.recognizeFormula(strokesStr);
				}else {
					final String strokesJson = strokeToJson(strokes);
					//call remote recognizer
					Map<String, String> params = new HashMap<String, String>();
					params.put("strokes", strokesJson);
					String url = serverAPI+"/api/recognition/formula";
					result = HttpUtils.submitPostData(url, params, "utf-8");
				}
				//send message
				Message tempMsg = mHandler.obtainMessage();
				tempMsg.what = RECOGNIZE_FORMULA_ID;
				tempMsg.obj = result;
				mHandler.sendMessage(tempMsg);
			}
		}.start();
	}
	private void callPigai(final ArrayList<ArrayList<Integer>> strokes) {
		if(serverAPI.equals(SettingActivity.localAPI)) {
			Util.showToast(this,"Auto Correcting Cannot Support Offline Mode");
			return;
		}
		new Thread() {
			@Override
			public void run() {
				String result="";
				if(serverAPI.equals(SettingActivity.localAPI)) {

				}else
				{
					final String strokesJson = strokeToJson(strokes);
					//call remote recognizer
					Map<String, String> params = new HashMap<String, String>();
					params.put("item_id",String.valueOf(item_id));
					params.put("strokes", strokesJson);
					String url = serverAPI + "/api/recognition/pigai";
					result = HttpUtils.submitPostData(url, params, "utf-8");
				}
				//send message
				Message tempMsg = mHandler.obtainMessage();
				tempMsg.what = RECOGNIZE_PIGAI_ID;
				tempMsg.obj = result;
				mHandler.sendMessage(tempMsg);
			}
		}.start();
	}
	private void fetchNextQuestion() {
		if(serverAPI.equals(SettingActivity.localAPI)) {
			Util.showToast(this,"Auto Correcting Cannot Support Offline Mode");
			return;
		}
		pigai_result.setImageDrawable(getResources().getDrawable(R.drawable.init));
		final String full_info = font_prefix + "<b>Question: </b><br/>Loading<hr/><b>Answer: </b><br/>Loading<hr/>" + font_postfix;
		question_details.setText(full_info);

		new Thread() {
			@Override
			public void run() {
				//call remote fetcher
				Map<String, String> params = new HashMap<String, String>();
				params.put("idx", String.valueOf(nextIdx));
				String url = serverAPI + "/api/source/pigai";
				final String result = HttpUtils.submitPostData(url, params, "utf-8");
				//send message
				Message tempMsg = mHandler.obtainMessage();
				tempMsg.what = FETCH_QUESTION_ID;
				tempMsg.obj = result;
				mHandler.sendMessage(tempMsg);
			}
		}.start();
	}
	private String strokeToString(final ArrayList<ArrayList<Integer>> strokes){
        String strokesStr = "";
        for (int i = 0; i < strokes.size(); i++) {
            final ArrayList<Integer> stroke = strokes.get(i);
            for (int j = 0; j < stroke.size(); j+=2) {
                final int x = stroke.get(j);
				final int y = stroke.get(j+1);
                strokesStr += String.valueOf(x)+" "+String.valueOf(y) + ",";
            }
            strokesStr = strokesStr.substring(0,strokesStr.length()-1);
            strokesStr += ";";
        }
        strokesStr = strokesStr.substring(0,strokesStr.length()-1);
        return strokesStr;
	}
    private String strokeToJson(final ArrayList<ArrayList<Integer>> strokes){
        try {
            JSONArray strokeArrayObj = new JSONArray();
            for (int i = 0; i < strokes.size(); i++) {
                JSONArray pointArrayObj = new JSONArray();
                final ArrayList<Integer> stroke = strokes.get(i);
                for (int j = 0; j < stroke.size(); j+=2) {
                    final int point_x = stroke.get(j);
					final int point_y = stroke.get(j+1);
                    final int x = point_x;
                    final int y = point_y;
                    JSONObject pointObj = new JSONObject();
                    pointObj.put("x", x);
                    pointObj.put("y", y);
                    pointArrayObj.put(pointObj);
                }
                JSONObject strokeObj = new JSONObject();
                strokeObj.put("points",pointArrayObj);
                strokeArrayObj.put(strokeObj);
            }

            JSONObject root = new JSONObject();
            root.put("strokes",strokeArrayObj);
            final String strokesStr = root.toString();
            return strokesStr;
        }catch (JSONException e){
            e.printStackTrace();
            return "";
        }
    }
	@Override
	public void onClick(View v) {
		showServerStatus();

		switch (v.getId()) {

			case R.id.buttonSubmitEng: {
				Util.showToast(this, "Recognizing");
				recognizeText(mSampleView.processStrokes(),LANGUAGE_ENG);
			}
				break;
			case R.id.buttonSubmitChi: {
				Util.showToast(this,"Recognizing");
				recognizeText(mSampleView.processStrokes(),LANGUAGE_CHI);
			}
				break;
            case R.id.buttonSubmitFormula: {
				Util.showToast(this,"Recognizing");
				recognizeFormula(mSampleView.processStrokes());
            }
            	break;

			case R.id.buttonSubmitNext: {
				Util.showToast(this,"Loading Question");
				fetchNextQuestion();
			}
			break;
			case R.id.buttonSubmitPigai: {
				if(item_details.isEmpty()){
					Util.showToast(this,"Get Question First");
					return;
				}
				if (mSampleView.getStrokes().isEmpty()){
					Util.showToast(this,"Please Write Your Answer");
					return;
				}
				Util.showToast(this,"Recognizing and Correcting");
				ImageView pigaiResult = (ImageView)findViewById(R.id.pigai_result);
				pigaiResult.setImageDrawable(getResources().getDrawable(R.drawable.progress));
				callPigai(mSampleView.processStrokes());
			}
			break;
			case R.id.buttonClear: {
				//question_details.setText("");
				recognition_result.setText("");
				mSampleView.clearView();
				//debug_iamge_view.setImageBitmap(null);
			}
				break;
			default:
				break;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mBroadcastReceiver);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				String address = null;

				if ((address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)) != null) {
					penClientCtrl.connect(address);
				}
			}
			break;
			case REQUEST_SETTING:
			{
				showServerStatus();
				updateLayoutStatus();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return super.onCreateOptionsMenu(menu);
	}

	public void showServerStatus(){
		SharedPreferences sp = getSharedPreferences(SettingActivity.app_setting_tag, Context.MODE_PRIVATE);
		serverAPI = sp.getString(SettingActivity.URL_TAG,SettingActivity.defaultURL);
		if(sp.getBoolean(SettingActivity.OFFLINEMODE_TAG,SettingActivity.defaultOfflineMode)){
			serverAPI=SettingActivity.localAPI;
		}
		/*
		if(serverAPI.equals(SettingActivity.localAPI)){
			Util.showToast(this,"离线识别");
		}else{
			Util.showToast(this,"联网识别");
		}
		*/
	}
	public void updateLayoutStatus(){
		SharedPreferences sp = getSharedPreferences(SettingActivity.app_setting_tag, Context.MODE_PRIVATE);
		final int layoutMode = sp.getInt(SettingActivity.LAYOUTMODE_TAG,SettingActivity.defaultLayoutMode);
		LinearLayout layout_btngrp_free = (LinearLayout)findViewById(R.id.layout_btngrp_free);
		LinearLayout layout_btngrp_pigai = (LinearLayout)findViewById(R.id.layout_btngrp_pigai);
		LinearLayout layout_pigai = (LinearLayout)findViewById(R.id.layout_pigai);
		LinearLayout layout_question = (LinearLayout)findViewById(R.id.layout_question);
		if(layoutMode == LAYOUTMODE_FREE) {
            layout_btngrp_free.setVisibility(View.VISIBLE);
            layout_btngrp_pigai.setVisibility(View.GONE);
			layout_pigai.setVisibility(View.GONE);
			layout_question.setVisibility(View.GONE);
			question_details.setText("");

			ViewGroup.LayoutParams params = mSampleView.getLayoutParams();
			params.height = 800;
			mSampleView.setLayoutParams(params);
			mSampleView.setRenderParams(18.0f);
		}else if(layoutMode == LAYOUTMODE_PIGAI){
            layout_btngrp_free.setVisibility(View.GONE);
            layout_btngrp_pigai.setVisibility(View.VISIBLE);
			layout_pigai.setVisibility(View.VISIBLE);
			layout_question.setVisibility(View.VISIBLE);
			final String empty_item_details = font_prefix + "<b>Question: </b><br/><hr/><b>Answer: </b><br/>" + font_postfix;
			ImageView pigaiResult = (ImageView)findViewById(R.id.pigai_result);
			pigaiResult.setImageDrawable(getResources().getDrawable(R.drawable.init));
			question_details.setText(empty_item_details);

			ViewGroup.LayoutParams params = mSampleView.getLayoutParams();
			params.height = 500;
			mSampleView.setLayoutParams(params);
			mSampleView.setRenderParams(26.0f);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {

		case R.id.action_connect:
			if (!penClientCtrl.isConnected()) {
				startActivityForResult(new Intent(MainActivity.this, DeviceListActivity.class), REQUEST_CONNECT_DEVICE_SECURE);
			}
			return true;

		case R.id.action_disconnect:
			if (penClientCtrl.isConnected()) {
				penClientCtrl.disconnect();
			}
			return true;

		case R.id.action_setting:
			startActivityForResult(new Intent(MainActivity.this, SettingActivity.class),REQUEST_SETTING);
			return true;
		case R.id.action_mode_free:
		{
			SharedPreferences sp = getSharedPreferences(SettingActivity.app_setting_tag, Context.MODE_PRIVATE);
			final int layoutMode = sp.getInt(SettingActivity.LAYOUTMODE_TAG,SettingActivity.defaultLayoutMode);
			if (layoutMode == LAYOUTMODE_FREE){
				return true;
			}else {
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt(SettingActivity.LAYOUTMODE_TAG,LAYOUTMODE_FREE);
				editor.apply();
				updateLayoutStatus();
				return true;
			}
		}
		case R.id.action_mode_pigai:
		{
			SharedPreferences sp = getSharedPreferences(SettingActivity.app_setting_tag, Context.MODE_PRIVATE);
			final int layoutMode = sp.getInt(SettingActivity.LAYOUTMODE_TAG,SettingActivity.defaultLayoutMode);
			if (layoutMode == LAYOUTMODE_PIGAI){
				return true;
			}else {
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt(SettingActivity.LAYOUTMODE_TAG,LAYOUTMODE_PIGAI);
				editor.apply();
				updateLayoutStatus();
				return true;
			}
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public String getExternalStoragePath() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return Environment.getExternalStorageDirectory().getAbsolutePath();
		} else {
			return Environment.MEDIA_UNMOUNTED;
		}
	}

	private void handleDot(int sectionId, int ownerId, int noteId, int pageId, int x, int y, int fx, int fy, int force, long timestamp, int type, int color) {
		mSampleView.addDot(sectionId, ownerId, noteId, pageId, x, y, fx, fy, force, timestamp, type, color);
		//getActionBar().setTitle("x:" + x + ",y:" + y + ",fx:" + fx + ",fy:" + fy);
		// Log.e("Dots", sectionId + "," + ownerId + "," + noteId + "," + pageId + "," + x + "," + y + "," + fx + "," + fy + "," + force + "," + timestamp + ","
		// + type + "," + color);
	}

	private void handleMsg(int penMsgType, String content) {
		Log.d(TAG, "handleMsg : " + penMsgType);

		switch (penMsgType) {
		// Message of the attempt to connect a pen
		case PenMsgType.PEN_CONNECTION_TRY:

			Util.showToast(this, "try to connect.");

			break;

		// Pens when the connection is completed (state certification process is not yet in progress)
		case PenMsgType.PEN_CONNECTION_SUCCESS:

			Util.showToast(this, "connection is successful.");

			break;

		// Message when a connection attempt is unsuccessful pen
		case PenMsgType.PEN_CONNECTION_FAILURE:

			Util.showToast(this, "connection has failed.");

			break;

		// When you are connected and disconnected from the state pen
		case PenMsgType.PEN_DISCONNECTED:

			Util.showToast(this, "connection has been terminated.");

			break;

		// Pen transmits the state when the firmware update is processed.
		case PenMsgType.PEN_FW_UPGRADE_STATUS: {
			try {
				JSONObject job = new JSONObject(content);

				int total = job.getInt(JsonTag.INT_TOTAL_SIZE);
				int sent = job.getInt(JsonTag.INT_SENT_SIZE);

				this.onUpgrading(total, sent);

				Log.d(TAG, "pen fw upgrade status => total : " + total + ", progress : " + sent);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
			break;

		// Pen firmware update is complete
		case PenMsgType.PEN_FW_UPGRADE_SUCCESS:

			this.onUpgradeSuccess();

			Util.showToast(this, "file transfer is complete.");

			break;

		// Pen Firmware Update Fails
		case PenMsgType.PEN_FW_UPGRADE_FAILURE:

			this.onUpgradeFailure(false);

			Util.showToast(this, "file transfer has failed.");

			break;

		// When the pen stops randomly during the firmware update
		case PenMsgType.PEN_FW_UPGRADE_SUSPEND:

			this.onUpgradeFailure(true);

			Util.showToast(this, "file transfer is suspended.");

			break;

		// Offline Data List response of the pen
		case PenMsgType.OFFLINE_DATA_NOTE_LIST:

			try {
				JSONArray list = new JSONArray(content);

				for (int i = 0; i < list.length(); i++) {
					JSONObject jobj = list.getJSONObject(i);

					int sectionId = jobj.getInt(JsonTag.INT_SECTION_ID);
					int ownerId = jobj.getInt(JsonTag.INT_OWNER_ID);
					int noteId = jobj.getInt(JsonTag.INT_NOTE_ID);

					Log.d(TAG, "offline(" + (i + 1) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// if you want to get offline data of pen, use this function.
			// you can call this function, after complete download.
			//
			// iPenCtrl.reqOfflineData( sectionId, ownerId, noteId );

			Util.showToast(this, "offline data list is received.");

			break;

		// Messages for offline data transfer begins
		case PenMsgType.OFFLINE_DATA_SEND_START:

			break;

		// Offline data transfer completion
		case PenMsgType.OFFLINE_DATA_SEND_SUCCESS:

			break;

		// Offline data transfer failure
		case PenMsgType.OFFLINE_DATA_SEND_FAILURE:

			break;

		// Progress of the data transfer process offline
		case PenMsgType.OFFLINE_DATA_SEND_STATUS: {
			try {
				JSONObject job = new JSONObject(content);

				int total = job.getInt(JsonTag.INT_TOTAL_SIZE);
				int received = job.getInt(JsonTag.INT_RECEIVED_SIZE);

				Log.d(TAG, "offline data send status => total : " + total + ", progress : " + received);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
			break;

		// When the file transfer process of the download offline
		case PenMsgType.OFFLINE_DATA_FILE_CREATED: {
			try {
				JSONObject job = new JSONObject(content);

				int sectionId = job.getInt(JsonTag.INT_SECTION_ID);
				int ownerId = job.getInt(JsonTag.INT_OWNER_ID);
				int noteId = job.getInt(JsonTag.INT_NOTE_ID);
				int pageId = job.getInt(JsonTag.INT_PAGE_ID);

				String filePath = job.getString(JsonTag.STRING_FILE_PATH);

				Log.d(TAG, "offline data file created => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId
						+ " filePath : " + filePath);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
			break;

		// Ask for your password in a message comes when the pen
		case PenMsgType.PASSWORD_REQUEST: {
			int retryCount = -1, resetCount = -1;

			try {
				JSONObject job = new JSONObject(content);

				retryCount = job.getInt(JsonTag.INT_PASSWORD_RETRY_COUNT);
				resetCount = job.getInt(JsonTag.INT_PASSWORD_RESET_COUNT);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			inputPassDialog = new InputPasswordDialog(this, this, retryCount, resetCount);
			inputPassDialog.show();
		}
			break;
		}
	}

	public void inputPassword(String password) {
		penClientCtrl.inputPassword(password);
	}

	private void onUpgrading(int total, int progress) {
		mBuilder.setContentText("Sending").setProgress(total, progress, false);
		mNotifyManager.notify(0, mBuilder.build());
	}

	private void onUpgradeFailure(boolean isSuspend) {
		if (isSuspend) {
			mBuilder.setContentText("file transfer is suspended.").setProgress(0, 0, false);
		} else {
			mBuilder.setContentText("file transfer has failed.").setProgress(0, 0, false);
		}
		mNotifyManager.notify(0, mBuilder.build());
	}

	private void onUpgradeSuccess() {
		mBuilder.setContentText("The file transfer is complete.").setProgress(0, 0, false);
		mNotifyManager.notify(0, mBuilder.build());
	}

	private void parseOfflineData() {
		// obtain saved offline data file list
		String[] files = OfflineFileParser.getOfflineFiles();

		if (files == null || files.length == 0) {
			return;
		}

		for (String file : files) {
			try {
				// create offline file parser instance
				OfflineFileParser parser = new OfflineFileParser(file);

				// parser return array of strokes
				Stroke[] strokes = parser.parse();

				if (strokes != null) {
					mSampleView.addStrokes(strokes);
				}

				// delete data file
				parser.delete();
				parser = null;
			} catch (Exception e) {
				Log.e(TAG, "parse file exeption occured.", e);
			}
		}
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (Broadcast.ACTION_PEN_MESSAGE.equals(action)) {
				int penMsgType = intent.getIntExtra(Broadcast.MESSAGE_TYPE, 0);
				String content = intent.getStringExtra(Broadcast.CONTENT);

				handleMsg(penMsgType, content);
			} else if (Broadcast.ACTION_PEN_DOT.equals(action)) {
				int sectionId = intent.getIntExtra(Broadcast.SECTION_ID, 0);
				int ownerId = intent.getIntExtra(Broadcast.OWNER_ID, 0);
				int noteId = intent.getIntExtra(Broadcast.NOTE_ID, 0);
				int pageId = intent.getIntExtra(Broadcast.PAGE_ID, 0);
				int x = intent.getIntExtra(Broadcast.X, 0);
				int y = intent.getIntExtra(Broadcast.Y, 0);
				int fx = intent.getIntExtra(Broadcast.FX, 0);
				int fy = intent.getIntExtra(Broadcast.FY, 0);
				int force = intent.getIntExtra(Broadcast.PRESSURE, 0);
				long timestamp = intent.getLongExtra(Broadcast.TIMESTAMP, 0);
				int type = intent.getIntExtra(Broadcast.TYPE, 0);
				int color = intent.getIntExtra(Broadcast.COLOR, 0);

				handleDot(sectionId, ownerId, noteId, pageId, x, y, fx, fy, force, timestamp, type, color);
			} else if (Broadcast.ACTION_PEN_DOT.equals(action)) {
				penClientCtrl.suspendPenUpgrade();
			}
		}
	};
}
