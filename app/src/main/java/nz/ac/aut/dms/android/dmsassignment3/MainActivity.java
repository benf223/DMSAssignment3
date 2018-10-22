package nz.ac.aut.dms.android.dmsassignment3;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements LocationListener {
	Button submitButton;
	LinearLayout contacts;

	ArrayList<AndroidContact> foundContacts;
	HashMap<String, CheckBox> checkBoxes;

	private boolean sendMessage = false;
	private FusedLocationProviderClient mFusedLocationClient;
	private LocationCallback mLocationCallback;

    @Override
    protected void onStart() {
        super.onStart();

        getPermissions();
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        contacts = findViewById(R.id.contactsCheckBoxes);

        inflateContactsList();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest r = new LocationRequest();
        r.setInterval(1);
        r.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (sendMessage) {
                        generateRequest(location);
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(r, mLocationCallback, null);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        generateRequest(location);
                    }
                }
            });
        }

        submitButton = findViewById(R.id.Send);
        submitButton.setActivated(false);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage = true;
            }
        });
    }

	private void inflateContactsList() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
			foundContacts = new ArrayList<>();

			Cursor androidContactsCursor = null;
			ContentResolver contentResolver = getContentResolver();

			try {
				androidContactsCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
			} catch (Exception e) {
				Log.e("Contact Error", e.getMessage());
			}

			if (androidContactsCursor.getCount() > 0) {
				while (androidContactsCursor.moveToNext()) {
					int hasPhoneNumber = Integer.parseInt(androidContactsCursor.getString(androidContactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));

					if (hasPhoneNumber > 0) {
						AndroidContact contact = new AndroidContact();

						contact.contactName = androidContactsCursor.getString(androidContactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
						contact.contactId = androidContactsCursor.getString(androidContactsCursor.getColumnIndex(ContactsContract.Contacts._ID));

						Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{contact.contactId}, null);

						while (phoneCursor.moveToNext()) {
							contact.contactPhoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						}

						phoneCursor.close();

						foundContacts.add(contact);
					}
				}
			}

			androidContactsCursor.close();

			checkBoxes = new HashMap<>();

			for (AndroidContact c : foundContacts) {
				CheckBox box = new CheckBox(this);
				box.setText(c.contactName);

				checkBoxes.put(c.contactId, box);
				contacts.addView(box);
			}
		}
	}

	private void getPermissions() {
		String[] permissions = {Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.INTERNET};
		ActivityCompat.requestPermissions(this, permissions, 1);
	}

	private void sendSMSMessages(String location) {
		for (AndroidContact c : foundContacts) {
			if (checkBoxes.get(c.contactId).isChecked()) {
				location = location == "" ? "Nowhere" : location;

				sendSMSMessage(c.contactPhoneNumber, location);
			}
		}
	}

	protected void sendSMSMessage(String number, String location) {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
			try {
				SmsManager smsManager = SmsManager.getDefault();
				smsManager.sendTextMessage(number, null, location, null, null);
				Toast.makeText(getApplicationContext(), "Location sent.", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(), "Sending message failed, please try again.", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void requestData(String url) {
		RequestPackage requestPackage = new RequestPackage();
		requestPackage.setMethod("GET");
		requestPackage.setUrl(url);

		Downloader downloader = new Downloader();

		downloader.execute(requestPackage);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStatusChanged(String s, int i, Bundle bundle) {

	}

	@Override
	public void onProviderEnabled(String s) {

	}

	@Override
	public void onProviderDisabled(String s) {

	}

	@Override
	public void onLocationChanged(Location location) {
		submitButton.setActivated(location != null);
	}

	private void generateRequest(Location location)
	{
		if (sendMessage)
		{
			String request = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + location.getLatitude() + "," + location.getLongitude() + "&key=AIzaSyCMfm2KXoMkcwEFE-ORZjBseSIMYgmhK-g";
			requestData(request);
			sendMessage = false;
		}
	}

	private class AndroidContact {
		public String contactName = "";
		public String contactPhoneNumber = "";
		public String contactId = "";
	}

	private class Downloader extends AsyncTask<RequestPackage, String, String> {
		@Override
		protected String doInBackground(RequestPackage... params) {
			return HttpManager.getData(params[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject jsonObject = new JSONObject(result);

				JSONArray results = jsonObject.getJSONArray("results");

				sendSMSMessages((String) ((JSONObject)results.get(1)).get("formatted_address"));
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}
}