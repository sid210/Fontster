package com.chromium.fontinstaller;

import java.io.File;
import java.io.IOException;

import com.chromium.fontinstaller.util.IabHelper;
import com.chromium.fontinstaller.util.IabResult;
import com.chromium.fontinstaller.util.Inventory;
import com.chromium.fontinstaller.util.Purchase;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class About extends PreferenceActivity {

	int easterEggClicks = 10;
	private static final String TAG = "com.chromium.fontinstaller";
	IabHelper mHelper;
	static final String ITEM_SKU = "com.chromium.fontster.donate";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.about_preference);	

		ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

		String base64EncodedPublicKey = 
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjZla6YOR6Od3NTxs98KfxlQV68oTdGZmiRO2vzVIQ7W3oYP6FjNpjMhyckjvkD2sCtHBPU6OjkSD75TzB17knW2FK7ZZGQkEMdCLD9qX8IbJ17w0UDGpW3b71sOWiOV92f0aIzgRZFl7IERzsgzFEnpCLx3Yxdl4JLmErPaE19ZxHeJ+r25O4NMYCmzAPHdmtBOKRiNGqg6gQPsZqBlfy0XV+pEZB7HagpMlaYvy0eBXA5PkvtpRjDciukOw3j6hqTK/FHepNC4PxO1BWBjGaeZh4u/HbGzXplI8UHZOThfUpHWqzIM3kT8bJ9+JfrViM7pLYpBSy9+xhIGIcKHKkQIDAQAB";

		mHelper = new IabHelper(this, base64EncodedPublicKey);

		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) 
			{
				if (!result.isSuccess()) {
					Log.d(TAG, "In-app Billing setup failed: " + 
							result);
				} else {             
					Log.d(TAG, "In-app Billing is set up OK");
				}
			    mHelper.queryInventoryAsync(mReceivedInventoryListener);
			}
		});
		
		Preference clearCache = (Preference) findPreference("clearCache");
		clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {

				File downloadDir = new File("/sdcard/DownloadedFonts");
				File previewDir = new File("/sdcard/SampleFonts");

				if (downloadDir.exists() || previewDir.exists()) {
					AsyncTask<Void, Void, Void> cleanCache = new AsyncTask<Void, Void, Void>()  { 
						//display progress dialog while fonts are copied in background
						ProgressDialog cleanCacheProgress;

						@Override
						protected void onPreExecute() {
							super.onPreExecute();
							cleanCacheProgress = new ProgressDialog (About.this);
							cleanCacheProgress.setMessage("Clearing downloaded fonts...");
							cleanCacheProgress.show();
						}

						@Override
						protected Void doInBackground(Void... params) {

							String downloads = "rm -r /sdcard/DownloadedFonts";
							String previews = "rm -r /sdcard/SampleFonts";
							Runtime runtime = Runtime.getRuntime();
							try {
								runtime.exec(downloads);
								runtime.exec(previews);
							}
							catch (IOException e) { 

							}
							return null;
						}

						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							if (cleanCacheProgress != null) {
								if (cleanCacheProgress.isShowing()) {
									cleanCacheProgress.dismiss();
								}
							}
							CustomAlerts.showBasicAlert ("Done", "Cached fonts have been deleted.", About.this);

						}
					};
					cleanCache.execute((Void[])null);

				}
				else
					CustomAlerts.showBasicAlert ("Nothing to clean", "There are currently no cached fonts.", About.this);
				return true; 
			}
		});

		Preference requestFont = (Preference) findPreference("fontRequest");
		requestFont.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				CustomAlerts.showRequestAlert(About.this);	
				return true; 
			}
		});

		Preference reboot = (Preference) findPreference("reboot");
		reboot.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {

				final Dialog confirm = new Dialog(About.this);

				confirm.requestWindowFeature(Window.FEATURE_NO_TITLE);
				confirm.setContentView(R.layout.two_button_alert);	
				TextView alertTitle = (TextView) confirm.findViewById(R.id.title);
				alertTitle.setText("Confirm reboot");
				TextView alertMessage = (TextView) confirm.findViewById(R.id.message);
				alertMessage.setText("Are you sure you want to reboot?");
				Button positiveButton = (Button) confirm.findViewById(R.id.positive);
				Button negativeButton = (Button) confirm.findViewById(R.id.negative);

				positiveButton.setOnClickListener(new View.OnClickListener() { // confirm yes
					public void onClick(View v){
						confirm.cancel();
						try {
							Process softReboot = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot"});	
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				});	
				negativeButton.setOnClickListener(new View.OnClickListener() { // confirm no
					public void onClick(View v){
						confirm.cancel();
					}			
				});
				confirm.show();
				return true; 
			}
		});

		Preference restartSysUI = (Preference) findPreference("restartSysUI");
		restartSysUI.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				try {
					Process restartSysUI = Runtime.getRuntime().exec(new String[] { "su", "-c", "pkill com.android.systemui"});	
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				return true; 
			}
		});

		Preference splash = (Preference) findPreference("viewSplash");
		splash.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent openSplash = new Intent (About.this, Splash.class);
				startActivity(openSplash);				
				return true; 
			}
		});

		Preference donateInAppBilling = (Preference) findPreference("inAppBilling");
		donateInAppBilling.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {				
				mHelper.launchPurchaseFlow(About.this, ITEM_SKU, 10001, mPurchaseFinishedListener, "mypurchasetoken");
				return true; 
			}
		});
		
		Preference source = (Preference) findPreference("source");
		source.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent git = new Intent();
				git.setAction(Intent.ACTION_VIEW);
				git.addCategory(Intent.CATEGORY_BROWSABLE);
				git.setData(Uri.parse("https://github.com/Chromium1/FontInstaller"));
				startActivity(git);
				return true; 
			}
		});

		Preference site = (Preference) findPreference("website");
		site.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent site = new Intent();
				site.setAction(Intent.ACTION_VIEW);
				site.addCategory(Intent.CATEGORY_BROWSABLE);
				site.setData(Uri.parse("https://fontster.cf"));
				startActivity(site);
				return true; 
			}
		});

		Preference contact = (Preference) findPreference("contact");
		contact.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent email = new Intent();
				email.setAction(Intent.ACTION_VIEW);
				email.addCategory(Intent.CATEGORY_BROWSABLE);
				email.setData(Uri.parse("mailto:priyesh.96@hotmail.com"));
				startActivity(email);
				return true; 
			}
		});

		Preference donate = (Preference) findPreference("donate");
		donate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent donate = new Intent();
				donate.setAction(Intent.ACTION_VIEW);
				donate.addCategory(Intent.CATEGORY_BROWSABLE);
				donate.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
						"&business=XR9WBFXGZ9G5E&lc=CA&item_name=Font%20Installer&currency_code" +
						"=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
				startActivity(donate);
				return true; 
			}
		});

		Preference ethan = (Preference) findPreference("ethan");
		ethan.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				easterEggClicks--;
				if (easterEggClicks < 0){
					Intent aboutEE = new Intent(About.this, AboutEE.class);
					startActivity(aboutEE);
					easterEggClicks = 10;
				}
				return true; 
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {     
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) 
		{
			if (result.isFailure()) {
				Toast.makeText(getApplicationContext(), "Failed to make purchase.", Toast.LENGTH_LONG).show();
				return;
			}      
			else if (purchase.getSku().equals(ITEM_SKU)) {
				consumeItem();
			}

		}
	};

	public void consumeItem() {
		mHelper.queryInventoryAsync(mReceivedInventoryListener);
	}

	IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			if (result.isFailure()) {
				// Handle failure				
			}
			Purchase donate = inventory.getPurchase(ITEM_SKU);

	        if (donate != null) {
	            mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU), mConsumeFinishedListener);
	        }
		}
	};

	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			if (result.isSuccess()) {		    	 
				CustomAlerts.showBasicAlert("Thanks", "We appreciate your support.", About.this);
			} else {
				// handle error
			}
		}
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHelper != null) mHelper.dispose();
		mHelper = null;
	}
}
