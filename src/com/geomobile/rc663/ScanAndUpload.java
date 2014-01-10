package com.geomobile.rc663;

import com.android.hflibs.Iso15693_native;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import android.os.AsyncTask;



public class ScanAndUpload extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
	
	private static final String TAG = "rc663_15693_java";
	private static final String PW_DEV = "/proc/driver/as3992";
	private Iso15693_native dev = new Iso15693_native();
	private Button start_demo;
	private Button get_info;
	private TextView main_info;
	private TextView card_info;
	private EditText block_nr;
	private CheckBox lock_block;
	private CheckBox lock_afi;
	private CheckBox lock_dsfid;
	private DeviceControl power;
	
	private int block_max = 0;
	private int block_size = 0;
	
	private String imei = "";
	
	
	private class LongRunningGetIO extends AsyncTask <Void, Void, String> {
		String param_sn = "";
		public LongRunningGetIO(String param_sn) {
			this.param_sn = param_sn;
		}
		
		protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			StringBuffer out = new StringBuffer();
			int n = 1;
			while (n>0) {
				byte[] b = new byte[4096];
				n =  in.read(b);
				if (n>0) out.append(new String(b, 0, n));
			}
			return out.toString();
		}
		@Override
		protected String doInBackground(Void... params) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet("http://stacky.takau.net/android/scan.php?message=" + this.param_sn);
			String text = null;
			try {
				HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpEntity entity = response.getEntity();
				text = getASCIIContentFromEntity(entity);
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
			return text;
		}


		protected void onPostExecute(String results) {
			if (results!=null) {
				//EditText et = (EditText)findViewById(R.id.my_edit);
				TextView main_info = (TextView)findViewById(R.id.textView_15693_info);
				main_info.setText(results);
			}
			Button b = (Button)findViewById(R.id.button_15693_search);
			b.setClickable(true);
		}
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanandupload);
        
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        this.imei = telephonyManager.getDeviceId();
        
        //start_demo = (Button)findViewById(R.id.button_15693_demo);
        //start_demo.setOnClickListener(this);
        //start_demo.setEnabled(false);
        
        get_info = (Button)findViewById(R.id.button_15693_search);
        get_info.setOnClickListener(this);
        get_info.setEnabled(false);
        
        main_info = (TextView)findViewById(R.id.textView_15693_info);
        main_info.setMovementMethod(ScrollingMovementMethod.getInstance());
        card_info = (TextView)findViewById(R.id.textView_15693_cardinfo);
        
        //block_nr = (EditText)findViewById(R.id.editText_15693_block);
        
        //lock_block = (CheckBox)findViewById(R.id.checkBox_15693_lockblock);
        //lock_afi = (CheckBox)findViewById(R.id.checkBox_15693_lockafi);
        //lock_dsfid = (CheckBox)findViewById(R.id.checkBox_15693_lockdsfid);
        
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
        
        this.addItemsOnSpinner2();
    }
    
    public void addItemsOnSpinner2() {
    	 
    	Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
    	List<String> list = new ArrayList<String>();
    	list.add("list 1");
    	list.add("list 2");
    	list.add("list 3");
    	ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
    		android.R.layout.simple_spinner_item, list);
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	spinner2.setAdapter(dataAdapter);
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

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if(arg0 == get_info)
		{
			byte[] uid = dev.SearchCard(Iso15693_native.ISO15693_ACTIVATE_ADDRESSED, Iso15693_native.ISO15693_FLAG_UPLINK_RATE_HIGH, Iso15693_native.ISO15693_FLAG_NO_USE_AFI, (byte)0, Iso15693_native.ISO15693_FLAG_ONE_SLOTS, null, 0);
			if(uid == null)
			{
				card_info.setText("Error search card");
				main_info.setText("");
				return;
			}
			card_info.setText("SN: 0x");
			String sn = "";
			for(int i = Iso15693_native.ISO15693_UID_LENGTH - 1; i >= 0; i--)
			{
				card_info.append(String.format("%02X", uid[i]));
				sn = sn + String.format("%02X", uid[i]);
			}
			new LongRunningGetIO(sn + "-" + this.imei).execute();
		
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
