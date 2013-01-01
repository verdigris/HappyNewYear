package mu.verdigris.hny;

import java.lang.IllegalAccessException;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.reflect.Field;
import java.lang.Thread;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
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
        R.string.play, R.string.stop, R.string.stopping };
    private Random rnd;
    private Handler handler;
    private Button btn;
    private View.OnClickListener btnListener;
    private Button infoBtn;
    private View.OnClickListener infoBtnListener;
    private List<MyNumberPicker> np;
    private List<VibesButton> vb;
    private SoundPool sp;
    private Map<String, Map<String, Integer>> snd;
    private int sndLoading;
    private Thread seq;
    private int state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
        this.rnd = new Random();
        this.handler = new Handler();
        this.sp = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
        this.sp.setOnLoadCompleteListener(this);

        try {
            this.buildSnd();
        } catch (IllegalAccessException e) {
            Log.e(HNY.TAG, "Oops: " + e.getMessage());
        }

        this.buildNumberPickers();
        this.buildVibesButtons();
        this.buildMainButton();
        this.buildInfoButton();
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

        this.snd = new HashMap<String, Map<String, Integer>>();
        this.sndLoading = 0;

        for (Field f: R.raw.class.getFields()) {
            final String name = f.getName();
            final int split1 = name.indexOf('_') + 1;
            final int split2 = name.indexOf('_', split1);
            final String sndPx = name.substring(split1, split2);
            final String sndLabel = name.substring(split2 + 1);
            Map<String, Integer> sndList;

            if (!this.snd.containsKey(sndPx)) {
                sndList = new HashMap<String, Integer>();
                this.snd.put(sndPx, sndList);
            } else {
                sndList = this.snd.get(sndPx);
            }

            this.sndLoading++;
            sndList.put(sndLabel, this.sp.load(this, f.getInt(null), 1));
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
                                           0, 3, initValue++));
    }

    private void buildVibesButtons() {
        final int btnId[] = {
            R.id.btn_vibes_happy, R.id.btn_vibes_new, R.id.btn_vibes_year
        };
        final boolean btnStatus[] = {
            false, false, true
        };

        this.vb = new ArrayList<VibesButton>();

        for (int i = 0; i < 3; ++i) {
            final ImageButton btn = (ImageButton)this.findViewById(btnId[i]);
            this.vb.add(new VibesButton(btn, btnStatus[i]));
        }
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

    private void buildInfoButton() {
        this.infoBtnListener = new View.OnClickListener() {
                public void onClick(View v) {
                    final AlertDialog.Builder builder =
                        new AlertDialog.Builder(HNY.this);
                    final LayoutInflater inflater =
                        HNY.this.getLayoutInflater();
                    builder.setView(inflater.inflate(R.layout.info, null));
                    final AlertDialog dialog = builder.create();
                    dialog.show();

                    final ImageButton linkBtn =
                        (ImageButton)dialog.findViewById(R.id.info_btn_link);
                    linkBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                final String url = "http://verdigris.mu/";
                                final Intent intent =
                                    new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(url));
                                startActivity(intent);
                            }
                        });

                    final Button closeBtn =
                        (Button)dialog.findViewById(R.id.btn_info_close);
                    closeBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                }
            };

        this.infoBtn = (Button)this.findViewById(R.id.btn_info);
        this.infoBtn.setOnClickListener(this.infoBtnListener);
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
        final int state;

        synchronized (this) {
            state = this.state;
        }

        return state;
    }

    private class MyNumberPicker {
        private final ImageButton btnInc;
        private final ImageButton btnDec;
        private final TextView valueText;
        private final View.OnClickListener btnIncListener;
        private final View.OnClickListener btnDecListener;
        private final int min;
        private final int max;
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
                    final MyNumberPicker np = MyNumberPicker.this;
                    final Integer value = np.getValue();
                    np.valueText.setText(value.toString());
                }
                });
        }

        public int getValue() {
            return this.value;
        }
    }

    private class VibesButton {
        private static final int IMG_ON = R.drawable.vibes;
        private static final int IMG_OFF = R.drawable.vibes_off;
        private final ImageButton btn;
        private final View.OnClickListener btnListener;
        boolean status;

        public VibesButton(ImageButton btn, boolean initStatus) {
            this.btn = btn;
            this.setStatus(initStatus);

            this.btnListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        VibesButton.this.toggle();
                    }
                };
            this.btn.setOnClickListener(this.btnListener);
        }

        synchronized public boolean isOn() {
            return this.status;
        }

        synchronized public void toggle() {
            this.setStatus(!this.status);
        }

        private void setStatus(boolean status) {
            this.status = status;
            HNY.this.handler.post(new Runnable() {
                    public void run() {
                        final VibesButton vb = VibesButton.this;
                        final int img = vb.isOn() ? IMG_ON : IMG_OFF;
                        vb.btn.setImageResource(img);
                    }
                });
        }
    }

    private class SequenceThread extends Thread {
        private static final String arpDm7 = "dfac";
        private static final String arpG7 = "gbdf";
        private static final String arpC7M = "cegb";
        private static final String arpEnd = "edgb";

        public void run() {
            while (HNY.this.sndLoading != 0) {
                log("waiting for sounds to all be loaded...");
                this.doSleep(100);
            }

            /* Give the player a bit of time to start up first... */
            this.playFirst("silence", 0.0f);
            this.doSleep(500);

            boolean doRun = true;

            while (doRun) {
                this.playChord("voicehappy", HNY.this.np.get(0).getValue());
                this.doSleep(100);
                this.playFirst("guitar", 0.7f);
                this.playArpeggio("vibes", arpDm7, 188, 2, 0.2f,
                                  HNY.this.vb.get(0).isOn());
                this.doSleep(370);
                this.playChord("voicenew", HNY.this.np.get(1).getValue());
                this.playArpeggio("vibes", arpG7, 94, 3, 0.2f,
                                  HNY.this.vb.get(1).isOn());
                this.doSleep(550);
                this.playChord("voiceyear", HNY.this.np.get(2).getValue());
                this.doSleep(750);
                this.playArpeggio("vibes", arpC7M, 47, 2, 0.3f,
                                  HNY.this.vb.get(2).isOn());
                this.doSleep(200);
                this.playArpeggio("vibes", arpEnd, 188, 2, 0.3f,
                                  HNY.this.vb.get(2).isOn());
                this.doSleep(100);

                /* Sleep a bit more to compensate the sound pool latency */

                int totalVoices = 0;

                for (HNY.MyNumberPicker np: HNY.this.np)
                    totalVoices += np.getValue();

                this.doSleep((9 - totalVoices) * 3);

                doRun = (HNY.this.getState() == HNY.RUNNING);
            }

            if (HNY.this.vb.get(2).isOn())
                this.playArpeggio("vibes", arpC7M, 94, 4, 0.3f, true);

            HNY.this.setState(HNY.IDLE);
        }

        private void playFirst(String sndName, float vol) {
            final Map<String, Integer> sndList = HNY.this.snd.get(sndName);
            final int sndId = sndList.get(sndList.keySet().toArray()[0]);
            HNY.this.sp.play(sndId, vol, vol, 1, 0, 1.0f);
        }

        private void playChord(String sndName, int n, float vol) {
            final Map<String, Integer> sndList = HNY.this.snd.get(sndName);
            final int nn = Math.min(n, sndList.size());
            List<Integer> ids = new ArrayList<Integer>(sndList.values());

            for (int m = 0; m < nn; ++m) {
                final int randId = HNY.this.rnd.nextInt(ids.size());
                final int sndId = ids.get(randId);
                final float bal = HNY.this.rnd.nextFloat() * vol;
                sp.play(sndId, bal, (vol - bal), 1, 0, 1.0f);
                ids.remove(randId);
            }
        }

        private void playChord(String sndName, int n) {
            this.playChord(sndName, n, 1.0f);
        }

        private void playArpeggio(String sndName, String arp, int t,
                                  int n, float vol, boolean enabled) {
            final Map<String, Integer> sndList = HNY.this.snd.get(sndName);
            final int nn = Math.min(n, sndList.size());
            final float volK = enabled ? (vol / 2) : 0.0f;
            List<Integer> ids = new ArrayList<Integer>();

            for (String label: sndList.keySet()) {
                final char noteArray[] = {label.charAt(0)};
                final CharBuffer note = CharBuffer.wrap(noteArray);

                if (arp.contains(note))
                    ids.add(sndList.get(label));
            }

            for (int m = 0; m < nn; ++m) {
                final int randId = HNY.this.rnd.nextInt(ids.size());
                final int sndId = ids.get(randId);
                final float noteVol = volK + HNY.this.rnd.nextFloat() * volK;
                final float bal = HNY.this.rnd.nextFloat() * noteVol;
                sp.play(sndId, bal, (noteVol - bal), 1, 0, 1.0f);
                ids.remove(randId);
                this.doSleep(t);
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
