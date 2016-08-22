package zeta.android.apps.tictactoe.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;

public class SoundManager implements SoundPool.OnLoadCompleteListener {
    private static final String TAG = SoundManager.class.getSimpleName();

    /**
     * SoundPool left volume
     */
    private static final float LEFT_VOLUME = 1.0f;

    /**
     * SoundPool right volume
     */
    private static final float RIGHT_VOLUME = 1.0f;

    /**
     * All sounds will have equal priority
     */
    private static final int STREAM_PRIORITY = 0;

    /**
     * Potential LOOP_MODE
     */
    private static final int MODE_NO_LOOP = 0;

    /**
     * Potential LOOP_MODE
     */
    @SuppressWarnings("unused")
    private static final int MODE_LOOP_FOREVER = -1;

    /**
     * Whether sounds should loop
     */
    private static final int LOOP_MODE = MODE_NO_LOOP;

    /**
     * SoundPool playback rate
     */
    private static final float PLAYBACK_RATE = 1.0f;

    /**
     * Inner SoundManager instance
     */
    private static SoundManager sInstance = null;

    /**
     * Mapping of resource ids to sound ids returned by load()
     */
    private HashMap<Integer, Integer> mSoundMap = new HashMap<>();

    /**
     * SoundPool instance
     */
    private SoundPool mSoundPool;

    /**
     * Application Context
     */
    private Context mContext;

    /**
     * Maximum concurrent streams that can play
     */
    private static final int MAX_STREAMS = 2;

    /**
     * Private constructor for singleton
     */
    private SoundManager(Context context) {
        mContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSoundPool = createNewSoundPool();
        } else {
            mSoundPool = createOldSoundPool();
        }
        mSoundPool.setOnLoadCompleteListener(this);
    }

    /**
     * Static access to internal instance
     */
    public static SoundManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SoundManager(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Loads a sound. Called automatically by play() if not already loaded
     */
    public void load(int id) {
        mSoundMap.put(id, mSoundPool.load(mContext, id, 1));
    }

    /**
     * Test if sound is loaded, call with id from R.raw
     *
     * @param resourceId : resource id of the sound file
     * @return true|false
     */
    public boolean isSoundLoaded(int resourceId) {
        return mSoundMap.containsKey(resourceId);
    }

    /**
     * Unload sound, prints warning if sound is not loaded
     */
    public void unload(int id) {
        if (mSoundMap.containsKey(id)) {
            int soundId = mSoundMap.remove(id);
            mSoundPool.unload(soundId);
        } else {
            Log.w(TAG, "sound: " + id + " is not loaded!");
        }
    }

    public void play(int resourceId) {
        if (isSoundLoaded(resourceId)) {
            mSoundPool.play(mSoundMap.get(resourceId), LEFT_VOLUME, RIGHT_VOLUME, STREAM_PRIORITY, LOOP_MODE, PLAYBACK_RATE);
        } else {
            load(resourceId);
        }
    }

    /**
     * If the sound is being loaded for the first time, we should wait until it
     * is completely loaded to play it.
     */
    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        mSoundPool.play(sampleId, LEFT_VOLUME, RIGHT_VOLUME, STREAM_PRIORITY, LOOP_MODE, PLAYBACK_RATE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected SoundPool createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        return new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();

    }

    @SuppressWarnings("deprecation")
    protected SoundPool createOldSoundPool() {
        return new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
    }

}