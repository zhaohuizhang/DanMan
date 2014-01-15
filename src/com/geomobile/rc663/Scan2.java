package com.geomobile.rc663;

import com.android.hflibs.Iso15693_native;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.*;

import android.os.AsyncTask;

public class Scan2 extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
	
	private static final String TAG = "rc663_15693_java";
	private static final String PW_DEV = "/proc/driver/as3992";
	private Iso15693_native dev = new Iso15693_native();
	private Button start_demo;
	private Button get_info;
	private Button submit;
	private TextView main_info;
	private TextView card_info;
	private TextView textView1;
	private EditText block_nr;
	private CheckBox lock_block;
	private CheckBox lock_afi;
	private CheckBox lock_dsfid;
	private DeviceControl power;
	private Spinner spinner2;
	private ListView listView;
	private EditText editText1;
	
	private int block_max = 0;
	private int block_size = 0;
	
	private String imei = "";
	private ArrayAdapter adapter;
	// private String[] myStringArray = {"gen1", "gen2"};
	private List<String> items = new ArrayList<String>();
	private IOCallback fetchController = null, submitController = null;
	private HashMap<String, String> wasteOptionsMap = new HashMap<String, String>();
	private HashMap<String, String> wasteItemTypeMap = new HashMap<String, String>();
	private HashMap<String, String> wasteItemSNMap = new HashMap<String, String>();
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan2);
        
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        this.imei = telephonyManager.getDeviceId();
        
        //start_demo = (Button)findViewById(R.id.button_15693_demo);
        //start_demo.setOnClickListener(this);
        //start_demo.setEnabled(false);
        
        get_info = (Button)findViewById(R.id.button_15693_search);
        get_info.setOnClickListener(this);
        get_info.setEnabled(true);
        
        //submit = (Button)findViewById(R.id.button_15693_upload);
        //submit.setOnClickListener(this);
        //submit.setEnabled(true);
        
        //textView1 = (TextView)findViewById(R.id.textView1);
        //editText1 = (EditText)findViewById(R.id.editText1);
        main_info = (TextView)findViewById(R.id.textView_15693_info);
        main_info.setMovementMethod(ScrollingMovementMethod.getInstance());
        card_info = (TextView)findViewById(R.id.textView_15693_cardinfo);
        
        //block_nr = (EditText)findViewById(R.id.editText_15693_block);
        
        //lock_block = (CheckBox)findViewById(R.id.checkBox_15693_lockblock);
        //lock_afi = (CheckBox)findViewById(R.id.checkBox_15693_lockafi);
        //lock_dsfid = (CheckBox)findViewById(R.id.checkBox_15693_lockdsfid);
        
        //items.add(new String("aloha_1"));
        
        power = new DeviceControl();
        if(power.DeviceOpen(PW_DEV) < 0)
        {
        	main_info.setText(R.string.msg_error_power);
        	return;
        }
        Log.d(TAG, "open file ok");
        
        if(power.PowerOnDevice() < 0)
        {
        	power.DeviceClose();
        	main_info.setText(R.string.msg_error_power);
        	return;
        }
        Log.d(TAG, "open power ok");
        
        try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
        
        Log.d(TAG, "begin to init");
        if(dev.InitDevice() != 0)
        {
        	power.PowerOffDevice();
        	power.DeviceClose();
        	main_info.setText(R.string.msg_error_dev);
        	return;
        }
        Log.d(TAG, "init ok");
        get_info.setEnabled(true);
        
        
    }
    
    @Override
    public void onDestroy()
    {
    	Log.d(TAG, "on destory");
    	dev.ReleaseDevice();
    	power.PowerOffDevice();
    	power.DeviceClose();
    	super.onDestroy();
    }
    
    public void debugMessage(String msg)
    {
    	TextView main_info = (TextView)findViewById(R.id.textView_15693_info);
		main_info.setText(msg);
    }
    
    public class NullCallback implements IOCallback {
    	public void httpRequestDidFinish(int success, String value) {
    		
    	}
    }
    
    public class FetchItemCallbackController implements IOCallback {
    	Scan2 activity;
    	String sn;
    	ProgressDialog progDialog;
    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    	public FetchItemCallbackController(Scan2 activity, String sn) {
    		this.sn = sn;
    		this.activity = activity;
    		NameValuePair postContent = new BasicNameValuePair("sn", sn);
    		nameValuePairs.add(postContent);
    		new LongRunningGetIO("http://stacky.takau.net/android/fetchItem.php?imei=" + activity.imei, nameValuePairs, this).execute();
    		
    		progDialog = ProgressDialog.show(activity, "正在获取信息",
    	            "请稍候...", true);
    	}
    	
    	public void parseJSON(String value) throws JSONException
    	{
    		JSONObject jObject = new JSONObject(value);
    		activity.popupEditText(sn, jObject.getString("num"));
    	}
    	
    	public void httpRequestDidFinish(int success, String value)
    	{
    		try {
				parseJSON(value);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		progDialog.dismiss();
    		activity.fetchController = null;
    	}
    }
    
    public class SubmitCallbackController implements IOCallback {
    	Scan2 activity;
    	ProgressDialog progDialog;
    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    	public SubmitCallbackController(Scan2 activity, JSONObject postJson) {
    		this.activity = activity;
    		NameValuePair postContent = new BasicNameValuePair("items", postJson.toString());
    		nameValuePairs.add(postContent);
    		new LongRunningGetIO("http://stacky.takau.net/android/postNewItems.php?imei=" + activity.imei, nameValuePairs, this).execute();
    		
    		progDialog = ProgressDialog.show(activity, "正在上传",
    	            "请稍候...", true);
    	}
    	private void parseJSON(String value) throws JSONException
    	{
    		/*
    		JSONObject jObject = new JSONObject(value);
    		JSONArray jArray = jObject.getJSONArray("wasteOptions");
    		for (int i=0; i < jArray.length(); i++)
    		{
    		    try {
    		        JSONObject oneObject = jArray.getJSONObject(i);
    		        // Pulling items from the array
    		        String optionName = oneObject.getString("name");
    		        String optionId = oneObject.getString("id");
    		        activity.wasteOptionsMap.put(optionName, optionId);
    		        list.add(optionName);
    		    } catch (JSONException e) {
    		        // Oops
    		    }
    		}*/
    	}
    	
    	public void httpRequestDidFinish(int success, String value) {
    		progDialog.dismiss();
    		
    		// nameValuePairs.add(new BasicNameValuePair("id", "12345"));
    		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	        builder.setTitle("NVPUpload")
	        .setMessage(value)
	        .setCancelable(false)
	        .setNegativeButton("确定",new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	            }
	        });
	        AlertDialog alert = builder.create();
	        alert.show();
	        
	        activity.submitController = null;
    	}
    }
    
    public void popupEditText(String sn, String value)
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

    	alert.setTitle("修改重量");
    	alert.setMessage("RFID: " + sn);

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	input.setText(value);
    	alert.setView(input);

    	alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {

    	  // Do something with value!
    	  }
    	});

    	alert.setNegativeButton("取消", new DialogInterface.OnClickListener() {
    	  public void onClick(DialogInterface dialog, int whichButton) {
    	    // Canceled.
    	  }
    	});

    	alert.show();
    }
    
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if(arg0 == get_info)
		{
			Log.d(TAG, "get_info clicked");
			
			final ProgressDialog scanningDialog = ProgressDialog.show(this, "正在扫描 RFID 设备",
    	            "请稍候...", true);
			Thread newThread = new Thread() {
				@Override
				public void run() {
					try {
						sleep(1000);
						scanningDialog.dismiss();
					} catch (InterruptedException e) {
						scanningDialog.dismiss();
						e.printStackTrace();
					}
				}
			};
			newThread.start();
			
			byte[] uid = dev.SearchCard(Iso15693_native.ISO15693_ACTIVATE_ADDRESSED, Iso15693_native.ISO15693_FLAG_UPLINK_RATE_HIGH, Iso15693_native.ISO15693_FLAG_NO_USE_AFI, (byte)0, Iso15693_native.ISO15693_FLAG_ONE_SLOTS, null, 0);
			if(uid == null)
			{
				card_info.setText("Error search card");
				main_info.setText("");
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setTitle("请将 RFID 标签置于识别区")
		        .setMessage("未检测到任何 RFID 标签！")
		        .setCancelable(false)
		        .setNegativeButton("确定",new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		            }
		        });
		        AlertDialog alert = builder.create();
		        alert.show();
				return;
			}
			card_info.setText("SN: 0x");
			String sn = "";
			for(int i = Iso15693_native.ISO15693_UID_LENGTH - 1; i >= 0; i--)
			{
				card_info.append(String.format("%02X", uid[i]));
				sn = sn + String.format("%02X", uid[i]);
			}
			//new LongRunningGetIO(sn + "-" + this.imei, new NullCallback()).execute();
			//this.addNewItemToList(sn);
			
			if(fetchController == null)
				fetchController = new FetchItemCallbackController(this, sn);
			
		
			byte[] cinfo = dev.ReadCardInfo();
			if(cinfo == null)
			{
				main_info.setText("Error get cardinfo, maybe card don't support");
				return;
			}
			main_info.setText(String.format("AFI:					0x%x\n", cinfo[Iso15693_native.ISO15693_INFO_AT_AFI]));
			main_info.append(String.format("DSFID:				0x%x\n", cinfo[Iso15693_native.ISO15693_INFO_AT_DSFID]));
			main_info.append(String.format("BLOCK NUMBERS:	%d\n", cinfo[Iso15693_native.ISO15693_INFO_AT_BLOCK_NR]));
			main_info.append(String.format("BLOCK SIZE:			%d\n", cinfo[Iso15693_native.ISO15693_INFO_AT_BLOCK_SIZE]));
			main_info.append(String.format("IC :					0x%x\n\n", cinfo[Iso15693_native.ISO15693_INFO_AT_IC]));
			//start_demo.setEnabled(true);
			
			block_max = cinfo[Iso15693_native.ISO15693_INFO_AT_BLOCK_NR];
			block_size = cinfo[Iso15693_native.ISO15693_INFO_AT_BLOCK_SIZE];
		}
	}
    
}