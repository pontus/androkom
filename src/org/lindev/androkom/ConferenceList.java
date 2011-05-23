package org.lindev.androkom;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.lindev.androkom.KomServer.ConferenceInfo;
import org.lindev.androkom.KomServer.TextInfo;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a list of all conferences with unread texts.
 * 
 * @author henrik
 *
 */
public class ConferenceList extends ListActivity 
{
	public static final String TAG = "Androkom ConferenceList";

	/**
     * Instantiate activity.  
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        // Use a custom layout file
        setContentView(R.layout.main);

        emptyview = (TextView) findViewById(android.R.id.empty);

        mTimer = new Timer();
    
        mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.conflistconf);
        setListAdapter(mAdapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);   
    }
    
    /**
     * While activity is active, keep a timer running to periodically refresh
     * the list of conferences with unread messages.
     */
    @Override
    public void onResume()
    {
        super.onResume();
               
              
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
            	 
				// Must populate list in UI thread.
                runOnUiThread(new Runnable() {
                    public void run() {                     
                    	 new PopulateConferenceTask().execute();      
                    }
                    
                });                     
            }
            
        }, 500, 10000);
        
    }
    
    /**
     * If activity is no longer active, cancel periodic updates.
     */
    @Override
    public void onPause()
    {
        super.onPause();
        mTimer.cancel();
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if (isFinishing())
            getApp().doUnbindService();
    }
    
    /**
     * Called when a conference has been clicked. Switch to Conference activity, 
     * passing the ID along.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        Toast.makeText(getApplicationContext(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();    
        
        Intent intent = new Intent(this, Conference.class);
        intent.putExtra("conference-id", mConferences.get((int)id).id);
        startActivity(intent);

    }
    
    /**
     * Show options menu. Currently does nothing useful.
     */
    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.conferencelist, menu);
        return true;
    }
 
    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
		Log.d(TAG, "onOptionsItemSelected");
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.menu_settings_id:
			Log.d(TAG, "Starting menu");
			startActivity(new Intent(this, ConferencePrefs.class));
			return true;

		case R.id.menu_createnewtext_id:
            intent = new Intent(this, CreateNewText.class);    
            intent.putExtra("recipient_type", 1);
            startActivity(intent);
			return true;

		case R.id.menu_createnewmail_id:
            intent = new Intent(this, CreateNewText.class);    
            intent.putExtra("recipient_type", 2);
            startActivity(intent);
			return true;

		case R.id.menu_createnewIM_id:
            intent = new Intent(this, CreateNewIM.class);    
            startActivity(intent);
			return true;

		case R.id.menu_seewhoison_id:
			seewhoison();
			return true;

		case R.id.menu_endast_id:
            intent = new Intent(this, Endast.class);    
            startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case android.view.KeyEvent.KEYCODE_X:
			XDialog dialog = new XDialog(this);
			dialog.show();
			return true;
		case android.view.KeyEvent.KEYCODE_Q:
		case 4: // back in emulator
			finish();
		default:
			Log.d(TAG, "onKeyup unknown key:" + keyCode + " " + event);
		}
		return false;
	}

	protected void seewhoison() {
        Intent intent = new Intent(this, WhoIsOn.class);    
        startActivity(intent);
	}
	
	
	
	private class PopulateConferenceTask extends AsyncTask<Void, Void, List<ConferenceInfo> > 
    {
		@Override
        protected void onPreExecute() 
        {
        	setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        @Override
        protected List<ConferenceInfo> doInBackground(final Void... args) 
        {
        	return fetchConferences();        	       	
        }

        @Override
        protected void onPostExecute(final List<ConferenceInfo> fetched) 
        {
        	setProgressBarIndeterminateVisibility(false);

            mAdapter.clear();
            mConferences = fetched;
            
            if (mConferences != null && (!mConferences.isEmpty())) {
            	for(ConferenceInfo elem : mConferences) {
            		mAdapter.add(elem);
            	}

            	mAdapter.notifyDataSetChanged();
            } else {
            	// TODO: Do something here?
            	Log.d(TAG, "populateConferences failed, no Conferences");
            	Log.d(TAG, "mConferences is null:"+(mConferences==null));
            	if(mConferences!=null) {
                	Log.d(TAG, "mConferences is empty:"+mConferences.isEmpty());
            	}
            	String currentDateTimeString = new Date().toLocaleString();
            	emptyview.setText(getString(R.string.no_unreads)+"\n"+
            			currentDateTimeString+"\n"+
            			getString(R.string.local_time)
            			);
            }
        }

    }

 
    private List<ConferenceInfo> fetchConferences() {
    	List<ConferenceInfo> retlist = null;
    	
    	try {
            App app = getApp();
            if (app != null) {
            	KomServer kom = app.getKom();
            	if (kom != null) {
            		if (kom.isConnected()) {
            			retlist = kom.fetchConferences();
            		} else {
            			Log.d(TAG, "Can't fetch conferences when no connection");
            	        Toast.makeText(getApplicationContext(), "Lost connection", Toast.LENGTH_SHORT).show();    
            			getApp().getKom().reconnect();
            		}
            	}
            }
    	} catch (Exception e) {
    		Log.d(TAG, "fetchConferences failed:"+e);
    		e.printStackTrace();
	        Toast.makeText(getApplicationContext(), "fetchConferences failed, probably lost connection", Toast.LENGTH_SHORT).show();    
    	}
		return retlist;
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }
    
 
    private List<ConferenceInfo> mConferences;
    private ArrayAdapter<ConferenceInfo> mAdapter;
    private Timer mTimer;
    TextView emptyview;
 }