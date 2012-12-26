package mu.verdigris.hny;

import java.lang.reflect.Field;
import java.lang.IllegalAccessException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class HNY extends Activity {

    private static final String TAG = "HNY";
    private View.OnClickListener btnListener;
    private Activity that;
    private Random rnd;
    private SoundPool sp;
    private List<Integer> snd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
        this.that = this;
        this.rnd = new Random();
        this.sp = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
        this.snd = new ArrayList<Integer>();

        try {
            for (Field f: R.raw.class.getFields()) {
                log(f.getName() + ": " + f.getInt(null));
                this.snd.add(this.sp.load(this, f.getInt(null), 1));
            }
        } catch (IllegalAccessException e) {
            Log.e(HNY.TAG, "Oops: " + e.getMessage());
        }

        this.btnListener = new View.OnClickListener() {
                public void onClick(View v) {
                    final int sndIndex = rnd.nextInt(snd.size());
                    final int streamId =
                        sp.play(snd.get(sndIndex), 0.9f, 0.9f, 1, 0, 1.0f);

                    if (streamId == 0)
                        Log.e(HNY.TAG, "Failed to play sound: " + sndIndex);
                }
            };

        ((Button)this.findViewById(R.id.btn_snd_happy))
            .setOnClickListener(this.btnListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.sp.release();
    }

    private static void log(String msg) {
        Log.i(HNY.TAG, msg);
    }
}
