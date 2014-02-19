package se.Welly;

import android.app.Dialog;
import android.support.v4.app.FragmentManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

public class StopwatchActivity extends ListActivity implements View.OnClickListener, TimerService.TimerCallback, StopwatchService.TimerCallback {
	private static String TAG = "StopwatchActivity";

    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;



    private static final int ALERT_DIALOG = 1;


    private MediaPlayer player;
    final Context context=this;
    private Button brewAddTime;
    private Button brewDecreaseTime;
    private Button startBrew;
    private TextView brewTimeLabel;

    protected int brewTime = 3;
    protected CountDownTimer brewCountDownTimer;
    public boolean isBrewing = false;

	// View elements in stopwatch.xml
	private TextView m_elapsedTime;
	private Button m_start;
	private Button m_pause;
	private Button m_reset;
	private Button m_lap;
	private ArrayAdapter<String> m_lapList;


	// Timer to update the elapsedTime display
    private final long mFrequency = 100;    // milliseconds
    private final int TICK_WHAT = 2;
	private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
        	updateElapsedTime();
        	sendMessageDelayed(Message.obtain(this, TICK_WHAT), mFrequency);
        }
    };

    // Connection to the backgorund StopwatchService

    private StopwatchService m_stopwatchService;
    private TimerService m_timerService;

    private ServiceConnection m_TimerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            m_timerService = ((TimerService.LocalBinder)service).getService();
            m_timerService.setTimerCallback(StopwatchActivity.this);


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            m_timerService = null;

        }
    };
        private ServiceConnection m_stopwatchServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			m_stopwatchService = ((StopwatchService.LocalBinder)service).getService();
            m_stopwatchService.setStopwatchCallback(StopwatchActivity.this);

            showCorrectButtons();


		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			m_stopwatchService = null;

        }
	};

	private void bindStopwatchService() {
        bindService(new Intent(this, StopwatchService.class),
        			m_stopwatchServiceConn, Context.BIND_AUTO_CREATE);

    }
	private void unbindStopwatchService() {
		if ( m_stopwatchService != null ) {
			unbindService(m_stopwatchServiceConn);
        }


	}
	private void bindTimerService() {
        bindService(new Intent(this, TimerService.class),
                m_TimerServiceConnection, Context.BIND_AUTO_CREATE);

    }
	private void unbindTimerService() {
		if ( m_timerService != null ) {
			unbindService(m_TimerServiceConnection);
        }

	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        setContentView(R.layout.stopwatch);
        setListAdapter(new ArrayAdapter<String>(this, R.layout.laps_row));

        startService(new Intent(this, StopwatchService.class));
        bindStopwatchService();


        startService(new Intent(this, TimerService.class));
        bindTimerService();

        m_elapsedTime = (TextView)findViewById(R.id.ElapsedTime);

        m_start = (Button)findViewById(R.id.StartButton);
        m_pause = (Button)findViewById(R.id.PauseButton);
        m_reset = (Button)findViewById(R.id.ResetButton);
        m_lap = (Button)findViewById(R.id.LapButton);

        m_lapList = (ArrayAdapter<String>)getListAdapter();

        mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), mFrequency);


        brewAddTime = (Button) findViewById(R.id.brew_time_up);
        brewDecreaseTime = (Button) findViewById(R.id.brew_time_down);
        startBrew = (Button) findViewById(R.id.brew_start);
        brewTimeLabel = (TextView) findViewById(R.id.brew_time);

        // Setup ClickListeners
        brewAddTime.setOnClickListener(this);
        brewDecreaseTime.setOnClickListener(this);
        startBrew.setOnClickListener(this);

        // Set the initial brew values

        setBrewTime(3);


        Button stop = (Button) findViewById(R.id.brew_stop);
        stop.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                player.stop();
                return false;
            }
        });

        play(this, getAlarmSound());

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindStopwatchService();
        unbindTimerService();
    }

    private void showCorrectButtons() {
    	Log.d(TAG, "showCorrectButtons");

    	if ( m_stopwatchService != null ) {
    		if ( m_stopwatchService.isStopwatchRunning() ) {
    			showPauseLapButtons();
    		} else {
    			showStartResetButtons();
    		}
    	}

    }

    private void showPauseLapButtons() {
    	Log.d(TAG, "showPauseLapButtons");

    	m_start.setVisibility(View.GONE);
    	m_reset.setVisibility(View.GONE);
    	m_pause.setVisibility(View.VISIBLE);
    	m_lap.setVisibility(View.VISIBLE);
    }

    private void showStartResetButtons() {
    	Log.d(TAG, "showStartResetButtons");

    	m_start.setVisibility(View.VISIBLE);
    	m_reset.setVisibility(View.VISIBLE);
    	m_pause.setVisibility(View.GONE);
    	m_lap.setVisibility(View.GONE);
    }

    public void onStartClicked(View v) {
    	Log.d(TAG, "start button clicked");
    	m_stopwatchService.start();
        //m_timerService.start();

        showPauseLapButtons();
    }

    public void onPauseClicked(View v) {
    	Log.d(TAG, "pause button clicked");
    	m_stopwatchService.pause();
    	//m_timerService.pause();

    	showStartResetButtons();
    }

    public void onResetClicked(View v) {
    	Log.d(TAG, "reset button clicked");
    	m_stopwatchService.reset();
    	//m_timerService.reset();

    	m_lapList.clear();
    }

    public void onLapClicked(View v) {
    	Log.d(TAG, "lap button clicked");
    	m_stopwatchService.lap();

        //m_timerService.lap();

    	m_lapList.insert(m_stopwatchService.getFormattedElapsedTime(), 0);

       // m_lapList.insert(m_timerService.getFormattedElapsedTime(), 0);
    }

    public void updateElapsedTime() {
    	if ( m_stopwatchService != null )
    		m_elapsedTime.setText(m_stopwatchService.getFormattedElapsedTime());


       /* if ( m_timerService != null )
            m_elapsedTime.setText(m_timerService.getFormattedElapsedTime());*/
    }

    private void play(Context context, Uri alert) {
        player = new MediaPlayer();
        try {
            player.setDataSource(context, alert);
            final AudioManager audio = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
                player.prepare();
            }
        } catch (IOException e) {
            Log.e("Error....", "Check code...");
        }
    }

    private Uri getAlarmSound() {
        Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alertSound == null) {
            alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alertSound == null) {
                alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        return alertSound;
    }

    // Connect interface elements to properties


    /** Methods **/

    /**
     * Set an absolute value for the number of minutes to brew. Has no effect if a brew
     * is currently running.
     * @param minutes The number of minutes to brew.
     */
   public void setBrewTime(int minutes) {
        if(isBrewing)
            return;

        brewTime = minutes;

        if(brewTime < 1)
            brewTime = 1;

        brewTimeLabel.setText(String.valueOf(brewTime) + "m");
    }

    /**
     * Set the number of brews that have been made, and update the interface.
     * @param count The new number of brews
     */


    /**
     * Start the brew timer
     */
    public void startBrew() {

        // Create a new CountDownTimer to track the brew time


        m_timerService.start(brewTime * 60 * 1000);
//        brewCountDownTimer.start();
        startBrew.setText("Stop");
        isBrewing = true;
    }






    @Override
    protected Dialog onCreateDialog( int id )
    {
        Dialog dialog = null;
        if ( id == ALERT_DIALOG )
        {
            ContextThemeWrapper ctw = new ContextThemeWrapper( this, R.style.MyTheme );
            CustomBuilder builder = new CustomBuilder( ctw );
            builder.setMessage( "Hello World" )
                    .setTitle( "Alarm Begin" )
                    .setIcon( android.R.drawable.ic_dialog_alert )
                    .setCancelable( false )
                    .setPositiveButton( "Stop Alarm",
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick( DialogInterface dialog, int which )
                                {
                                    dialog.dismiss();
                                    player.stop();
                                }
                            }
                    );
            dialog = builder.create();
        }
        if ( dialog == null )
        {
            dialog = super.onCreateDialog( id );
        }
        return dialog;
    }

    public void onFinish() {
        onFinish();
        if (isBrewing = false)
        player.start();
        showDialog(ALERT_DIALOG);

    }

    /**
     * Stop the brew timer
     */



    public void stopBrew() {
        if(brewCountDownTimer != null)
            brewCountDownTimer.cancel();
              m_timerService.stop();
        //showDialog( ALERT_DIALOG );

        isBrewing = false;
        startBrew.setText("Start");
    }

    /** Interface Implementations **/
  /* (non-Javadoc)
   * @see android.view.View.OnClickListener#onClick(android.view.View)
   */
    public void onClick(View v) {

            // Prepare intent which is triggered if the
            // notification is selected;

            // Build notification
            // Actions are just fake

        if(v == brewAddTime)
            setBrewTime(brewTime + 1);
        else if(v == brewDecreaseTime)
            setBrewTime(brewTime -1);
        else if(v == startBrew) {
            if(isBrewing)
                stopBrew();
            else
                startBrew();
        }
    }

    @Override
    public void onTimerValueChanged(String timerValue) {
        brewTimeLabel.setText(timerValue);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }



        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);

            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.ElapsedTime);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }
}