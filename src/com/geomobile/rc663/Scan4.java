package com.geomobile.rc663;

import android.os.Bundle;

public class Scan4 extends Scan3 {

	@Override
    public void onCreate(Bundle savedInstanceState) {
		this.myTitle = "入库扫描";
		this.myURL = "http://202.120.58.114/api/wasteIn.php";
        super.onCreate(savedInstanceState);
	}
}
