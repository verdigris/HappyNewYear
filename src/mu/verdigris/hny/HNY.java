package mu.verdigris.hny;

import java.lang.IllegalAccessException;
import java.lang.InterruptedException;
import java.lang.reflect.Field;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class HNY extends Activity
    implements SoundPool.OnLoadCompleteListener {

    private static final String TAG = "HNY";
    private static final int IDLE = 0;
    private static final int RUNNING = 1;
    private static final int STOPPING = 2;
    private static final int STATE_MSG[] = {
        R.string.start, R.string.stop, R.string.stopping };
    private Handler handler;
    private Button btn;
    private View.OnClickListener btnListener;
    private Random rnd;
    private SoundPool sp;
    private Map<String, List<Integer>> snd;
    private int sndLoading;
    private Thread seq;
    private int state;
    private List<Integer> stateMsg;
    private List<MyNumberPicker> np;

    /* ToDo: check return value of sp.play (stream id) and report error if 0 */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
        this.handler = new Handler();
        this.rnd = new Random();
        this.sp = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
        this.sp.setOnLoadCompleteListener(this);

        try {
            this.buildSnd();
        } catch (IllegalAccessException e) {
            Log.e(HNY.TAG, "Oops: " + e.getMessage());
        }

        this.buildNumberPickers();
        this.buildMainButton();
        this.setState(HNY.IDLE);
   }

    @Override
    protected void onDestroy() {
        this.sp.release();
        super.onDestroy();
    }

    public void onLoadComplete(SoundPool sp, int sampleId, int status) {
        this.sndLoading--;
    }

    private void buildSnd()
        throws IllegalAccessException {

        this.snd = new HashMap<String, List<Integer>>();
        this.sndLoading = 0;

        for (Field f: R.raw.class.getFields()) {
            final String name = f.getName();
            final int split1 = name.indexOf('_') + 1;
            final int split2 = name.indexOf('_', split1);
            final String sndPx = name.substring(split1, split2);
            List<Integer> sndList;

            if (!this.snd.containsKey(sndPx)) {
                log("sound list: " + sndPx);
                sndList = new ArrayList<Integer>();
                this.snd.put(sndPx, sndList);
            } else {
                sndList = this.snd.get(sndPx);
            }

            this.sndLoading++;
            sndList.add(this.sp.load(this, f.getInt(null), 1));
        }
    }

    private void buildNumberPickers() {
        final int npId[][] = {
            { R.id.btn_inc_happy, R.id.btn_dec_happy, R.id.text_happy },
            { R.id.btn_inc_new, R.id.btn_dec_new, R.id.text_new },
            { R.id.btn_inc_year, R.id.btn_dec_year, R.id.text_year },
        };

        this.np = new ArrayList<MyNumberPicker>();

        int initValue = 1;

        for (int ids[]: npId)
            this.np.add(new MyNumberPicker(ids[0], ids[1], ids[2],
                                           1, 3, initValue++));
    }

    private void buildMainButton() {
        this.btnListener = new View.OnClickListener() {
                public void onClick(View v) {
                    synchronized(HNY.this) {
                        switch (HNY.this.state) {
                        case HNY.IDLE:
                            HNY.this.setState(HNY.RUNNING);
                            break;
                        case HNY.RUNNING:
                            HNY.this.setState(HNY.STOPPING);
                            break;
                        case HNY.STOPPING:
                            break;
                        };

                        if (HNY.this.state != HNY.RUNNING)
                            return;
                    }

                    if (HNY.this.seq != null) {
                        try {
                            HNY.this.seq.join();
                        } catch (InterruptedException e) {
                            log("Oops: " + e.getMessage());
                        }

                        HNY.this.seq = null;
                    }

                    HNY.this.seq = new SequenceThread();
                    HNY.this.seq.start();
                }
            };

        this.btn = (Button)this.findViewById(R.id.btn_control);
        this.btn.setOnClickListener(this.btnListener);
    }

    private void setState(int state) {
        synchronized (this) {
            log("state: " + this.state + " -> " + state);
            this.state = state;
        }

        this.handler.post(new Runnable() {
                public void run() {
                    synchronized (HNY.this) {
                        HNY.this.btn.setText(HNY.STATE_MSG[HNY.this.state]);
                    }
                }
            });
    }

    private int getState() {
        int state;

        synchronized (this) {
            state = this.state;
        }

        return state;
    }

    private class MyNumberPicker {
        private ImageButton btnInc;
        private ImageButton btnDec;
        private TextView valueText;
        private View.OnClickListener btnIncListener;
        private View.OnClickListener btnDecListener;
        private int min;
        private int max;
        private int value;

        public MyNumberPicker(int btnIncId, int btnDecId, int textId,
                              int min, int max, int init) {
            this.btnInc = (ImageButton)HNY.this.findViewById(btnIncId);
            this.btnIncListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        MyNumberPicker.this.inc();
                    }
                };
            this.btnInc.setOnClickListener(this.btnIncListener);

            this.btnDec = (ImageButton)HNY.this.findViewById(btnDecId);
            this.btnDecListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        MyNumberPicker.this.dec();
                    }
                };
            this.btnDec.setOnClickListener(this.btnDecListener);

            this.valueText = (TextView)HNY.this.findViewById(textId);
            this.min = min;
            this.max = max;
            this.setValue(init);
        }

        public void inc() {
            this.setValue(this.value + 1);
        }

        public void dec() {
            this.setValue(this.value - 1);
        }

        public void setValue(int value) {
            if ((value > this.max) || (value < this.min))
                return;

            this.value = value;

            HNY.this.handler.post(new Runnable() {
                public void run() {
                    final Integer value = MyNumberPicker.this.getValue();
                    MyNumberPicker.this.valueText.setText(value.toString());
                }
                });
        }

        public int getValue() {
            return this.value;
        }
    }

    private class SequenceThread extends Thread {
        public void run() {
            while (HNY.this.sndLoading != 0) {
                log("waiting for sounds to all be loaded...");
                this.doSleep(100);
            }

            /* Give the player a bit of time to start up first... */
            HNY.this.sp.play(HNY.this.snd.get("silence").get(0),
                             0.0f, 0.0f, 1, 0, 1.0f);
            this.doSleep(500);

            boolean doRun = true;

            while (doRun) {
                this.playRandom("voicehappy", HNY.this.np.get(0).getValue());
                this.doSleep(100);
                HNY.this.sp.play(HNY.this.snd.get("guitar").get(0),
                                 0.7f, 0.7f, 1, 0, 1.0f);
                this.doSleep(750);
                this.playRandom("voicenew", HNY.this.np.get(1).getValue());
                this.doSleep(850);
                this.playRandom("voiceyear", HNY.this.np.get(2).getValue());
                this.doSleep(1580);

                doRun = (HNY.this.getState() == HNY.RUNNING);
            }

            HNY.this.setState(HNY.IDLE);
        }

        private void playRandom(String sndName, int n) {
            final List<Integer> sndList = HNY.this.snd.get(sndName);
            List<Integer> ids;

            ids = new ArrayList<Integer>();

            for (int m = 0; m < sndList.size(); ++m)
                ids.add(sndList.get(m));

            for (int m = 0; m < n; ++m) {
                final int id = HNY.this.rnd.nextInt(ids.size());
                final float bal = HNY.this.rnd.nextFloat();
                sp.play(ids.get(id), bal, (1.0f - bal), 1, 0, 1.0f);
                ids.remove(id);
            }
        }

        private void doSleep(int ms) {
            try {
                this.sleep(ms);
            } catch (InterruptedException e) {
                log("Oops: " + e.getMessage());
            }
        }
    }

    private static void log(String msg) {
        Log.i(HNY.TAG, msg);
    }
}
