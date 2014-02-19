package se.Welly;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

public class TimerService extends Service {


    private static final int  ALERT_DIALOG = 1;

    private static final String TAG = "TimerService";
    private static final int NOTIFICATION_ID = 1;
    // Timer to update the ongoing notification
    private final long mFrequency = 100;    // milliseconds
    private final int TICK_WHAT = 2;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
             updateNotification();
            sendMessageDelayed(Message.obtain(this, TICK_WHAT), mFrequency);
        }
    };
    public boolean isBrewing;
    private CountDownTimer brewCountDownTimer;
    private MediaPlayer player;
    private long  msTick;
    private TimerCallback mTimerCallback;
    private String ticker;
    private AlarmManager brewAlarmManager;
    private int id;
    private Timer m_timer;
    private LocalBinder m_binder = new LocalBinder();
    private NotificationManager m_notificationMgr;
    private Notification m_notification;
    private int msTimeLeft;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "bound");

        return m_binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "created");
        Log.v("onCreate", "onCreate");

        m_timer = new Timer();
        m_notificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        createNotification();


        play(this, getAlarmSound());

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public void createNotification() {
        Log.d(TAG, "creating notification");

        int icon = R.drawable.ic_launcher;
        CharSequence tickerText = "Timer";
        long when = System.currentTimeMillis();

        m_notification = new Notification(icon, tickerText, when);
        m_notification.flags |= Notification.FLAG_ONGOING_EVENT;
        m_notification.flags |= Notification.FLAG_NO_CLEAR;
    }

    public void updateNotification() {
         Log.d(TAG, "updating notification");

        Context context = getApplicationContext();
        CharSequence contentTitle = "Timer";
        String stringTick = (ticker);

        Intent notificationIntent = new Intent(this, StopwatchActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        // the next two lines initialize the Notification, using the configurations above
        m_notification.setLatestEventInfo(context, contentTitle,stringTick, contentIntent);
        m_notificationMgr.notify(NOTIFICATION_ID, m_notification);
    }

    public void showNotification() {
        Log.d(TAG, "showing notification");

        updateNotification();
        mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), mFrequency);
    }

    public void hideNotification() {
        Log.d(TAG, "removing notification");

        m_notificationMgr.cancel(NOTIFICATION_ID);
        mHandler.removeMessages(TICK_WHAT);
    }

    public void start(int timeleft) {
        msTimeLeft = timeleft;

        brewCountDownTimer = new CountDownTimer(msTimeLeft, 100) {

            @Override
            public void onTick(long millisUntilFinished) {
                long hours=0, minutes=0, seconds=0, tenths=0;
                StringBuilder sb = new StringBuilder();

                String timercount = String.valueOf(millisUntilFinished);
                Log.v("time left", timercount);
                if (millisUntilFinished < 1000) {
                    tenths = millisUntilFinished / 100;
                } else if (millisUntilFinished < 60000) {
                    seconds = millisUntilFinished / 1000;
                    millisUntilFinished -= seconds * 1000;
                    tenths = (millisUntilFinished / 100);
                } else if (millisUntilFinished < 3600000) {
                    hours = millisUntilFinished / 3600000;
                    millisUntilFinished -= hours * 3600000;
                    minutes = millisUntilFinished / 60000;
                    millisUntilFinished -= minutes * 60000;
                    seconds = millisUntilFinished / 1000;
                    millisUntilFinished -= seconds * 1000;
                    tenths = (millisUntilFinished / 100);
                }

                Log.v("VALUES", "HOURS: " + String.valueOf(hours));
                Log.v("VALUES", "MINS: " + String.valueOf(minutes));
                Log.v("VALUES", "SECS: " +  String.valueOf(seconds));
                Log.v("VALUES",  "TENTHS: " +  String.valueOf(tenths));


                if (hours > 0) {
                    sb.append(hours).append(":")
                            .append(formatDigits(minutes)).append(":")
                            .append(formatDigits(seconds)).append(".")
                            .append(tenths);
                } else {
                    sb.append(formatDigits(minutes)).append(":")
                            .append(formatDigits(seconds)).append(".")
                            .append(tenths);
                }
                  ticker = String.valueOf(sb);

                  updateNotification();

                  notifyTimerCallback(ticker);

            }



            @Override
            public void  onFinish() {
                if( isBrewing = false )
                {


                triggerAlarm();
                player.start();
                }



            }


        };

        msTimeLeft = timeleft;
        Log.d(TAG, "start");
        m_timer.start();
        brewCountDownTimer.start();

        showNotification();
    }

    public void triggerAlarm() {


        player.start();



        //  Toast.makeText(this, "TIMER", Toast.LENGTH_SHORT).show();

        // ALARM MANAGER
        // PENDING INTENT
        // BROADCAST _> MAIN ACTIVITY

        Intent alarmIntent = new Intent(this, StopwatchActivity.class);
        alarmIntent.putExtra("alertDialog", 20);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(alarmIntent);

    }

    public void stop() {
        Log.d(TAG, "stop");
        m_timer.stop();
        brewCountDownTimer.cancel();

        hideNotification();
    }

    private String formatDigits(long num) {
        return (num < 10) ? "0" + num : new Long(num).toString();
    }


    /*public long getElapsedTime() {
        return m_timer.getElapsedTime();
    }

   /* public String getFormattedElapsedTime() {
        return formatElapsedTime(getElapsedTime());
    }*/

    private void notifyTimerCallback(String notifiTimer) {
        if(mTimerCallback != null) {
            mTimerCallback.onTimerValueChanged(notifiTimer);
        }
    }

    public void setTimerCallback(TimerCallback timerCallback) {
        mTimerCallback = timerCallback;
    }

    public interface TimerCallback {
        void onTimerValueChanged(String timerValue);
    }

    public class  LocalBinder extends Binder {
        TimerService getService() {
            return  TimerService.this;
        }
    }



}