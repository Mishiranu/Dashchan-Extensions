package chan.application;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;

public class UriHandlerActivity extends Activity {
	private static final String ACTION = "chan.intent.action.HANDLE_URI";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent initialIntent = getIntent();
		Intent intent = new Intent(ACTION).setData(initialIntent.getData());
		Bundle extras = initialIntent.getExtras();
		if (extras != null) {
			intent.putExtras(extras);
		}
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			// Ignore exception
		}
		finish();
		System.exit(0);
	}
}
