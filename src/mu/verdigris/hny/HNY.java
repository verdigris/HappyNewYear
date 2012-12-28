package mu.verdigris.hny;

import java.lang.reflect.Field;
import java.lang.IllegalAccessException;
import java.lang.InterruptedException;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

public class HNY extends Activity
    implements SoundPool.OnLoadCompleteListener {

    private static final String TAG = "HNY";
    private View.OnClickListener btnListener;
    private Activity that;
    private Random rnd;
    private SoundPool sp;
    private Map<String, List<Integer>> snd;
    private int sndLoading;
    private Thread seq;
    private boolean runSeq;
    private List<NumberPicker> np;

    /* ToDo: check return value of sp.play (stream id) and report error if 0 */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
        this.that = this;
        this.rnd = new Random();
        this.sp = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
        this.sp.setOnLoadCompleteListener(this);
        this.runSeq = false;

        try {
            this.buildSnd();
        } catch (IllegalAccessException e) {
            Log.e(HNY.TAG, "Oops: " + e.getMessage());
        }

        this.buildNumberPickers();
        this.buildMainButton();
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
        final int npId[] = {
            R.id.np_voices_happy, R.id.np_voices_new, R.id.np_voices_year };
        this.np = new ArrayList<NumberPicker>();

        for (int id: npId) {
            final NumberPicker np = (NumberPicker)this.findViewById(id);
            np.setMinValue(1);
            np.setMaxValue(3);
            np.setValue(2);
            this.np.add(np);
        }
    }

    private void buildMainButton() {
        this.btnListener = new View.OnClickListener() {
                public void onClick(View v) {
                    HNY.this.runSeq = !HNY.this.runSeq;
                    log("seq run: " + HNY.this.runSeq);

                    if (HNY.this.seq != null) {
                        try {
                            /* Note: That's not so great... */
                            HNY.this.seq.join();
                        } catch (InterruptedException e) {
                            log("Oops: " + e.getMessage());
                        }

                        HNY.this.seq = null;
                    }

                    if (HNY.this.runSeq) {
                        HNY.this.seq = new SequenceThread();
                        HNY.this.seq.start();
                    }
                }
            };

        ((Button)this.findViewById(R.id.btn_snd_happy))
            .setOnClickListener(this.btnListener);
    }

    private class SequenceThread extends Thread {
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

        public void run() {
            while (HNY.this.sndLoading != 0) {
                log("waiting for sounds to all be loaded...");
                this.doSleep(100);
            }

            log("seq running");

            /* Give the player a bit of time to start up first... */
            HNY.this.sp.play(HNY.this.snd.get("silence").get(0),
                             0.0f, 0.0f, 1, 0, 1.0f);
            this.doSleep(500);

            while (HNY.this.runSeq) {
                this.playRandom("voicehappy", HNY.this.np.get(0).getValue());
                this.doSleep(100);
                HNY.this.sp.play(HNY.this.snd.get("guitar").get(0),
                                 0.7f, 0.7f, 1, 0, 1.0f);
                this.doSleep(750);
                this.playRandom("voicenew", HNY.this.np.get(1).getValue());
                this.doSleep(850);
                this.playRandom("voiceyear", HNY.this.np.get(2).getValue());
                this.doSleep(1580);
            }

            log("seq done");
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
