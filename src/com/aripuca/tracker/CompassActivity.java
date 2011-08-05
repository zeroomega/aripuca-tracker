package com.aripuca.tracker;

import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class CompassActivity extends Activity {
	
	protected class CompassBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Log.d(Constants.TAG, "CompassActivity: BROADCAST MESSAGE RECEIVED");
			
			Bundle bundle = intent.getExtras();
			
			updateCompass(bundle.getFloat("azimuth"));
			
		}
			
	}
	/**
	 * Compass updates broadcast receiver
	 */
	CompassBroadcastReceiver compassBroadcastReceiver;	
	
	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	private long declinationLastUpdate = 0;

	private float declination;

	/**
	 * Initialize the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// reference to application object
		myApp = ((MyApp) getApplicationContext());

		setContentView(R.layout.compass);

	}

	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		
		super.onPause();
	}
	
	@Override
	public void onResume() {

		// registering receiver for compass updates 
		IntentFilter filter = new IntentFilter("com.aripuca.tracker.COMPASS_UPDATES_ACTION");
		compassBroadcastReceiver = new CompassBroadcastReceiver();
		registerReceiver(compassBroadcastReceiver, filter);

		super.onResume();
		
	}
	
	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float azimuth) {

		boolean trueNorth = myApp.getPreferences().getBoolean("true_north", true);

		float rotation = 0;

		if (trueNorth && myApp.getCurrentLocation() != null) {

			long now = System.currentTimeMillis();

			// let's request declination every 15 minutes, not every compass update
			if (now - declinationLastUpdate > 15 * 60 * 1000) {
				declination = Utils.getDeclination(myApp.getCurrentLocation(), now);
				Log.d(Constants.TAG, "declination update: " + declination);
				declinationLastUpdate = now;
			}

		} else {
			declination = 0;
		}

		// magnetic north to true north
		rotation = getAzimuth(azimuth + declination);

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(rotation, 0)
					+ Utils.DEGREE_CHAR + " "
					+ Utils.getDirectionCode(rotation));
		}

		// true north compass
		if (findViewById(R.id.compassImage) != null) {

			CompassImage compassImage = (CompassImage) findViewById(R.id.compassImage);

			if (compassImage.getVisibility() == View.VISIBLE) {

				// Bitmap arrowBitmap =
				// BitmapFactory.decodeResource(getResources(),
				// R.drawable.windrose);
				// BitmapDrawable bmd = new BitmapDrawable(arrowBitmap);
				compassImage.setAngle(360 - rotation - getOrientationAdjustment());
				// compassImage.setAlpha(230);
				compassImage.invalidate();
				// compassImage.setImageDrawable(bmd);
			}
		}

		// magnetic north compass
		if (findViewById(R.id.compassImage2) != null) {

			CompassImage compassImage2 = (CompassImage) findViewById(R.id.compassImage2);

			if (compassImage2.getVisibility() == View.VISIBLE) {

				compassImage2.setAngle(360 - rotation + declination - getOrientationAdjustment());
				compassImage2.setAlpha(50);
				compassImage2.invalidate();
			}

		}

	}

	protected float getAzimuth(float az) {

		if (az > 360) {
			return az - 360;
		}

		return az;

	}

	/**
	 * Returns compass rotation angle when orientation of the phone changes
	 */
	private int getOrientationAdjustment() {

		switch (this.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				return 0;
			case Configuration.ORIENTATION_LANDSCAPE:
				return 90;
		}

		return 0;
	}
}