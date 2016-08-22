package zeta.android.apps.tictactoe;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import javax.annotation.ParametersAreNonnullByDefault;

import zeta.android.apps.tictactoe.media.SoundManager;

@ParametersAreNonnullByDefault
public class ZetaApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        //Load all sound upfront
        final Context context = getApplicationContext();
        final SoundManager instance = SoundManager.getInstance(context);
        instance.load(R.raw.gameover);
        instance.load(R.raw.gameovertie);
        instance.load(R.raw.notehigh);
        instance.load(R.raw.notelow);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //Do cleans ups
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        //Do cleans ups
    }
}
