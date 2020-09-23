package chan.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.util.List;

public class UriHandlerActivity extends Activity {
	private static final String ACTION = "chan.intent.action.HANDLE_URI";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = getIntent().getData();
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments != null && segments.size() >= 2) {
				String directory = segments.get(segments.size() - 2);
				if ("Archive".equals(directory)) {
					String origin = segments.get(segments.size() - 1);
					Intent initialIntent = getIntent();
					Intent intent = new Intent(ACTION).setData(Uri.parse("http://localhost/null/res/" + origin));
					Bundle extras = initialIntent.getExtras();
					if (extras != null) {
						intent.putExtras(extras);
					}
					try {
						startActivity(intent);
					} catch (ActivityNotFoundException e) {
						// Ignore
					}
				}
			}
		}
		finish();
		System.exit(0);
	}
}
