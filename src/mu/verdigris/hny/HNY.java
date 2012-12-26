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

public class HNY extends Activity
    implements SoundPool.OnLoadCompleteListener {

    private static final String TAG = "HNY";
    private View.OnClickListener btnListener;
    private Activity that;
    private Random rnd;
    private SoundPool sp;
    private Map<String, List<Integer>> snd;
    private Thread seq;
    private boolean runSeq;

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

    @Override
    protected void onDestroy() {
        this.sp.release();
        super.onDestroy();
    }

    public void onLoadComplete(SoundPool sp, int sampleId, int status) {
        log("loaded: " + sampleId + ", status: " + status);
    }

    private void buildSnd()
        throws IllegalAccessException {

        this.snd = new HashMap<String, List<Integer>>();

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

            sndList.add(this.sp.load(this, f.getInt(null), 1));
        }
    }

    private class SequenceThread extends Thread {
        private void playRandom(String sndName, int n) {
            final List<Integer> sndList = HNY.this.snd.get(sndName);
            final int id1 = sndList.get(HNY.this.rnd.nextInt(sndList.size()));
            sp.play(id1, 0.9f, 0.9f, 1, 0, 1.0f);

            if ((n == 1) || (sndList.size() == 1))
                return;

            int id2;

            do {
                id2 = sndList.get(HNY.this.rnd.nextInt(sndList.size()));
            } while (id2 == id1);

            sp.play(id2, 0.9f, 0.9f, 1, 0, 1.0f);
        }

        private void playAll(String sndName) {
            for (int sndId: HNY.this.snd.get(sndName))
                sp.play(sndId, 0.9f, 0.9f, 1, 0, 1.0f);
        }

        public void run() {
            log("seq running");

            while (HNY.this.runSeq) {
                this.playRandom("voicehappy", 2);
                this.doSleep(100);
                sp.play(HNY.this.snd.get("guitar").get(0),
                        0.9f, 0.9f, 1, 0, 1.0f);
                this.doSleep(750);
                this.playRandom("voicenew", 1);
                this.doSleep(850);
                this.playRandom("voiceyear", 2);
                this.doSleep(1550);
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
