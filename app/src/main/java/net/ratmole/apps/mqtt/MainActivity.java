package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static String PREFS = "mqtt-prefs";

	private static String HOSTNAME = "-Hostname";
	private static String TOPIC = "-Topic";
	private static String USERNAME = "-Username";
	private static String PASSWORD = "-Password";
	private static String PORT = "-Port";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final SharedPreferences sharedPref = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

		setContentView(R.layout.activity_main);

		String sHostname, sTopic, sUsername, sPassword, sPort;
		final EditText vHostname, vTopic, vUsername, vPassword, vPort;

		vHostname	= (EditText) findViewById(R.id.mHost);
		vTopic		= (EditText) findViewById(R.id.mTopic);
		vUsername	= (EditText) findViewById(R.id.mUsername);
		vPassword	= (EditText) findViewById(R.id.mPassword);
		vPort		= (EditText) findViewById(R.id.mPort);

		sHostname	= sharedPref.getString(HOSTNAME, "");
		sTopic		= sharedPref.getString(TOPIC,"");
		sUsername	= sharedPref.getString(USERNAME, "");
		sPassword	= sharedPref.getString(PASSWORD, "");
		sPort		= sharedPref.getString(PORT, "");

		vHostname.setText(sHostname);
		vTopic.setText(sTopic);
		vUsername.setText(sUsername);
		vPassword.setText(sPassword);
		vPort.setText(String.valueOf(sPort));

		final Button button = (Button) findViewById(R.id.mSave);
		final Button messages = (Button) findViewById(R.id.mMessages);

		messages.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntent = new Intent(MainActivity.this, MyListActivity.class);
				MainActivity.this.startActivity(myIntent);
			}
		});

		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (	vHostname.getText().toString().length() >	0	&&
						vTopic.getText().toString().length() 	>	0	&&
						vPort.getText().toString().length()		>	0
				){

					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putString(HOSTNAME, vHostname.getText().toString());
					editor.putString(TOPIC, vTopic.getText().toString());
					editor.putString(USERNAME, vUsername.getText().toString());
					editor.putString(PASSWORD, vPassword.getText().toString());
					editor.putString(PORT, vPort.getText().toString());
					editor.commit();


					final Intent intent = new Intent(getApplicationContext(), MQTTService.class);
					startService(intent);
					finish();
				} else {
					Toast.makeText(getApplicationContext(), "Please Fill Hostname, Topic and Port", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
}
