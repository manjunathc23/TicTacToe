package zeta.android.apps.tictactoe.views.game;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({GamePlayer.USER,
        GamePlayer.COMPUTER,
        GamePlayer.NONE})
@Retention(RetentionPolicy.SOURCE)
public @interface GamePlayer {
    int NONE = 1;
    int USER = 2;
    int COMPUTER = 3;
}

