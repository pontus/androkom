package org.lindev.androkom;

import java.util.List;
import org.lindev.androkom.KomServer.ConferenceInfo;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a list of persons logged on.
 * 
 * @author jonas
 *
 */
public class WhoIsOn extends ListActivity 
{
	public static final String TAG = "Androkom WhoIsOn";

	/**
     * Instantiate activity.  
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
                
        mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.whoison);
        setListAdapter(mAdapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
    }
    
    /**
     * Update on visible?
     */
    @Override
	public void onResume() {
		super.onResume();

		// mAdapter.clear();
		// update list?

		new populatePersonsTask().execute();
		//populatePersons();
	}
    
    /**
     * Pause
     */
    @Override
    public void onPause()
    {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
    }
    
    /**
     * Called when a person has been clicked. Switch to send message or mail?
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        Toast.makeText(getApplicationContext(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();    
        
        //Intent intent = new Intent(this, Conference.class);
        //intent.putExtra("conference-id", mConferences.get((int)id).id);
        //startActivity(intent);
    }
    

    /**
     * Attempt to get all persons online.
     */
    private class populatePersonsTask extends AsyncTask<Void, Integer, Integer> {
        private final ProgressDialog dialog = new ProgressDialog(WhoIsOn.this);

        protected void onPreExecute() {
        	Log.d(TAG, "populatePersonsTask:onPreExecute start");
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(true);
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.show();
        	Log.d(TAG, "populatePersonsTask:onPreExecute slut");
        }

		protected Integer doInBackground(Void... param) {
        	Log.d(TAG, "populatePersonsTask:doInBackground start");
			//populatePersons();
	        tPersons = fetchPersons();
			
        	Log.d(TAG, "populatePersonsTask:doInBackground slut");
			return 1;
		}

		@SuppressWarnings("unused")
		protected void onPostExecute(final Integer result) 
        { 
        	Log.d(TAG, "populatePersonsTask:onPostExecute start");
            this.dialog.dismiss();
            mPersons = tPersons;
            populatePersons();
        	Log.d(TAG, "populatePersonsTask:onPostExecute slut");
        }

    }

    /**
     * Refresh list of persons.
     */
    private void populatePersons() 
    {
    	Log.d(TAG, "populatePersons");
        mAdapter.clear();

        if (mPersons != null && (!mPersons.isEmpty())) {
        	for(ConferenceInfo elem : mPersons) {
        		mAdapter.add(elem);
        	}

        	mAdapter.notifyDataSetChanged();
        } else {
        	// TODO: Do something here?
        	Log.d(TAG, "populatePersons failed, no Persons");
        	Log.d(TAG, "mPersons is null:"+(mPersons==null));
        	if(mPersons!=null) {
            	Log.d(TAG, "mPersons is empty:"+mPersons.isEmpty());
        	}
        }
    }
 
    private List<ConferenceInfo> fetchPersons() {
    	List<ConferenceInfo> retlist = null;
    	
    	try {
            App app = getApp();
            if (app != null) {
            	KomServer kom = app.getKom();
            	if (kom != null) {
            		if (kom.isConnected()) {
            			retlist = kom.fetchPersons();
            		} else {
            			Log.d(TAG, "Can't fetch persons when no connection");
            	        Toast.makeText(getApplicationContext(), "Lost connection", Toast.LENGTH_SHORT).show();
            			getApp().getKom().reconnect();
            		}
            	}
            }
    	} catch (Exception e) {
    		Log.d(TAG, "fetchPersons failed:"+e);
    		e.printStackTrace();
	        Toast.makeText(getApplicationContext(), "fetchPersons failed, probably lost connection", Toast.LENGTH_SHORT).show();
    	}
		return retlist;
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }
    
 
    private List<ConferenceInfo> tPersons;
    private List<ConferenceInfo> mPersons;
    private ArrayAdapter<ConferenceInfo> mAdapter;
 }