package zeta.android.apps.tictactoe.views.game;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({GameState.IDLE,
        GameState.STARTED,
        GameState.WON,
        GameState.DRAW})
@Retention(RetentionPolicy.SOURCE)
public @interface GameState {
    int IDLE = 1;
    int STARTED = 2;
    int WON = 3;
    int DRAW = 4;
}

