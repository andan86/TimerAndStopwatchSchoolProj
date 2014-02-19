package se.Welly;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by andan86 on 12/02/14.
 */
public class Timer extends Activity {

    public interface GetTime {
        public long now();
    }

    private GetTime SystemTime = new GetTime() {
        @Override
        public long now() {	return System.currentTimeMillis(); }
    };


    private GetTime m_time;
    private State m_state;
    private long m_startTimer;
    private long m_pauseOffset;
    private long m_stopTimer;
    /**
     * Implements a method that returns the current time, in milliseconds.
     * Used for testing
     */

    public enum State { PAUSED, RUNNING };

    public Timer() {
        m_time = SystemTime;

    }
    public Timer(GetTime time) {
        m_time = time;

    }
    /**
     * Default way to get time. Just use the system clock.
     */


    /**
     * What is the stopwatch doing?
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stopwatch);





    }

    public void start() {
        if ( m_state == State.PAUSED ) {

        m_startTimer = m_time.now();
        m_state = State.RUNNING;
        }
    }

    public void stop() {
        if ( m_state == State.RUNNING ) {

            m_stopTimer = m_time.now();
        m_state = State.PAUSED;
        }
    }

    public long getElapsedTime() {
        if ( m_state == State.PAUSED ) {
            return (m_stopTimer - m_startTimer) + m_pauseOffset;
        } else {
            return (m_time.now() - m_startTimer) + m_pauseOffset;
        }
    }
}
