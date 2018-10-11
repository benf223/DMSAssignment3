package nz.ac.aut.dms.android.dmsassignment3;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
{
	private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
	private static final boolean DEBUG = true;
	Button submitButton;
	LinearLayout contacts;

	ArrayList<AndroidContact> foundContacts;
	HashMap<String, CheckBox> checkBoxes;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getPermissions();
		contacts = findViewById(R.id.contactsCheckBoxes);

		inflateContactsList();

		submitButton = findViewById(R.id.Send);
		submitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				sendSMSMessages();
			}
		});

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				// Should show a map of all known peoples locations
			}
		});
	}

	private void inflateContactsList()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
		{
			foundContacts = new ArrayList<>();

			Cursor androidContactsCursor = null;
			ContentResolver contentResolver = getContentResolver();

			try
			{
				androidContactsCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
			}
			catch (Exception e)
			{
				Log.e("Contact Error", e.getMessage());
			}

			if (androidContactsCursor.getCount() > 0)
			{
				while (androidContactsCursor.moveToNext())
				{
					int hasPhoneNumber = Integer.parseInt(androidContactsCursor.getString(androidContactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));

					if (hasPhoneNumber > 0)
					{
						AndroidContact contact = new AndroidContact();

						contact.contactName = androidContactsCursor.getString(androidContactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
						contact.contactId = androidContactsCursor.getString(androidContactsCursor.getColumnIndex(ContactsContract.Contacts._ID));

						Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", 	new String[]{contact.contactId}, null);

						while (phoneCursor.moveToNext())
						{
							contact.contactPhoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						}

						phoneCursor.close();

						foundContacts.add(contact);
					}
				}
			}

			androidContactsCursor.close();

			checkBoxes = new HashMap<>();

			for (AndroidContact c : foundContacts)
			{
				CheckBox box = new CheckBox(this);
				box.setText(c.contactName);

				checkBoxes.put(c.contactId, box);
				contacts.addView(box);
			}
		}
	}

	private void getPermissions()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
		{
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS))
			{
				ActivityCompat.requestPermissions(this,	new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
			}
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
		{
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE))
			{
				ActivityCompat.requestPermissions(this,	new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_SEND_SMS);
			}
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
		{
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
			{
				ActivityCompat.requestPermissions(this,	new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
			}
		}
	}

	private void sendSMSMessages()
	{
		String location = getLocation();
		for (AndroidContact c : foundContacts)
		{
			if (checkBoxes.get(c.contactId).isChecked())
			{
				sendSMSMessage(c.contactPhoneNumber, location);
			}
		}
	}

	protected void sendSMSMessage(String number, String location)
	{
		if (!DEBUG)
		{
			if (ContextCompat.checkSelfPermission(this,	Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
			{
				try
				{
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(number, null, location, null, null);
					Toast.makeText(getApplicationContext(), "Location sent.", Toast.LENGTH_SHORT).show();
				}
				catch (Exception e)
				{
					Toast.makeText(getApplicationContext(), "Sending message failed, please try again.", Toast.LENGTH_SHORT).show();
				}
			}
		}
		else
		{
			Toast.makeText(getApplicationContext(), "Number: " + number + " Location: " + location, Toast.LENGTH_SHORT);
		}
	}

	private String getLocation()
	{
		return "";
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up submitButton, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private class AndroidContact
	{
		public String contactName = "";
		public String contactPhoneNumber = "";
		public String contactId = "";
	}
}
