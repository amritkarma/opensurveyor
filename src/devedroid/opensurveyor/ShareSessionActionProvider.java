package devedroid.opensurveyor;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.actionbarsherlock.widget.ShareActionProvider;

import devedroid.opensurveyor.data.Session;
import devedroid.opensurveyor.data.SessionManager;

public class ShareSessionActionProvider extends ShareActionProvider {

	private Intent shareIntent;
	private SessionManager session;
	
	public ShareSessionActionProvider(Context context) {
		super(context);
		
		shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("application/octet-stream");
		shareIntent.putExtra(Intent.EXTRA_STREAM,
				Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "surveyor.xml")) );
		setShareIntent(shareIntent);
	}
	
	public void setSession(SessionManager sess) {
		session = sess;
	}
	
	public  boolean onPerformDefaultAction() {
		session.saveSession();
		return super.onPerformDefaultAction();
	}

}