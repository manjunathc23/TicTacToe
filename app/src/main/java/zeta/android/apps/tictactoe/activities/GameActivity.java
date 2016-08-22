package zeta.android.apps.tictactoe.activities;

import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RawRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import zeta.android.apps.tictactoe.R;
import zeta.android.apps.tictactoe.media.SoundManager;
import zeta.android.apps.tictactoe.views.common.BaseViews;
import zeta.android.apps.tictactoe.views.game.GameBoard;
import zeta.android.apps.tictactoe.views.game.GamePlayer;

public class GameActivity extends AppCompatActivity {

    private static final int MSG_COMPUTER_TURN = 1;
    private static final long COMPUTER_DELAY_MS = TimeUnit.MILLISECONDS.toMillis(500);
    private static final long MUSIC_DELAY = TimeUnit.MILLISECONDS.toMillis(500);

    private static int sTieCount = 0;
    private static int sPlayerWinCount = 0;
    private static int sComputerWinCount = 0;

    static class Views extends BaseViews {
        @Bind(R.id.game_title_with_drawables_left_right)
        TextView gameTitle;

        @Bind(R.id.player_turn)
        TextView playerTurn;

        @Bind(R.id.player_user_count)
        TextView playerUserCount;

        @Bind(R.id.player_tie_count)
        TextView playerTieCount;

        @Bind(R.id.player_computer_count)
        TextView playerComputerCount;

        @Bind(R.id.game_board)
        GameBoard gameBoard;

        @Bind(R.id.zeta_game_reset_play_again_btn)
        Button resetButton;

        @SuppressWarnings("ConstantConditions")
        Views(AppCompatActivity root) {
            super(root.findViewById(R.id.game_root_view));
        }
    }

    private Views mViews;
    private Random mRandom = new Random();
    private Handler mHandler = new Handler(new MessageHandlerCallback());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppThemeFullScreen);
        configureTaskDescription();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        mViews = new Views(this);
        registerClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeGame(GamePlayer.USER);
        updateCounters();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterClickListeners();
        mViews.clear();
        mViews = null;
    }

    private void initializeGame(@GamePlayer int gamePlayer) {
        mViews.gameBoard.setCurrentPlayer(gamePlayer);
        switch (gamePlayer) {
            case GamePlayer.COMPUTER:
                mViews.gameTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tictactoegamegrey, 0,
                        R.drawable.ic_computer_black_48dp, 0);
                requestComputersTurn();
                break;
            case GamePlayer.NONE:
            case GamePlayer.USER:
                mViews.gameTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tictactoegamegrey, 0,
                        R.drawable.ic_person_outline_black_48dp, 0);
                break;
        }
    }

    @GamePlayer
    private int toggleGamePlayer(@GamePlayer int currentPlayer) {
        return currentPlayer == GamePlayer.COMPUTER ? GamePlayer.USER : GamePlayer.COMPUTER;
    }

    private void showGameDraw() {
        mViews.gameBoard.startBoardBlink(4);
        setWinnerState(GamePlayer.NONE);
        mViews.gameBoard.postDelayed(() -> {
            if (mViews == null) {
                return;
            }
            playMusic(R.raw.gameovertie);
        }, MUSIC_DELAY);
    }

    private void finishTurn() {
        @GamePlayer
        int currentPlayer = mViews.gameBoard.getCurrentPlayer();
        if (!checkGameFinished(currentPlayer)) {
            currentPlayer = selectTurn(toggleGamePlayer(currentPlayer));
            if (currentPlayer == GamePlayer.COMPUTER) {
                mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
            }
        }
    }

    @GamePlayer
    private int selectTurn(@GamePlayer int player) {
        mViews.gameBoard.setCurrentPlayer(player);
        switch (player) {
            case GamePlayer.USER:
                mViews.gameBoard.setEnabled(true);
                mViews.playerTurn.setText(getString(R.string.game_players_turn));
                mViews.gameTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tictactoegamegrey,
                        0, R.drawable.ic_person_outline_black_48dp, 0);
                break;
            default:
            case GamePlayer.COMPUTER:
            case GamePlayer.NONE:
                mViews.playerTurn.setText(getString(R.string.game_computers_turn));
                mViews.gameBoard.setEnabled(false);
                mViews.gameTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tictactoegamegrey,
                        0, R.drawable.ic_computer_black_48dp, 0);
                break;
        }
        return player;
    }

    private boolean checkGameFinished(@GamePlayer int gamePlayer) {
        boolean isBoardFull = true;
        int col = GameBoard.INVALID;
        int row = GameBoard.INVALID;
        int diagonal = GameBoard.INVALID;
        final int[] gameData = mViews.gameBoard.getPlayerGameData();

        // check rows
        for (int j = 0, k = 0; j < 3; j++, k += 3) {
            if (gameData[k] != GamePlayer.NONE && gameData[k] == gameData[k + 1] && gameData[k] == gameData[k + 2]) {
                row = j;
            }
            if (isBoardFull && (gameData[k] == GamePlayer.NONE || gameData[k + 1] == GamePlayer.NONE || gameData[k + 2] == GamePlayer.NONE)) {
                isBoardFull = false;
            }
        }

        // check columns
        for (int i = 0; i < 3; i++) {
            if (gameData[i] != GamePlayer.NONE && gameData[i] == gameData[i + 3] && gameData[i] == gameData[i + 6]) {
                col = i;
            }
        }

        // check diagonals
        if (gameData[0] != GamePlayer.NONE && gameData[0] == gameData[1 + 3] && gameData[0] == gameData[2 + 6]) {
            diagonal = 0;
        } else if (gameData[2] != GamePlayer.NONE && gameData[2] == gameData[1 + 3] && gameData[2] == gameData[6]) {
            diagonal = 1;
        }

        if (col != GameBoard.INVALID || row != GameBoard.INVALID || diagonal != GameBoard.INVALID) {
            setGameOver(gamePlayer, col, row, diagonal);
            return true;
        }

        // if we get here, there's no winner but the board is isBoardFull.
        if (isBoardFull) {
            showGameDraw();
            return true;
        }
        return false;
    }

    private void setGameOver(@GamePlayer int gamePlayer, int col, int row, int diagonal) {
        mViews.gameBoard.setEnabled(false);
        setWinnerState(gamePlayer);
        mViews.gameBoard.setGameFinished(col, row, diagonal);
        mViews.gameBoard.setWinner(gamePlayer);
        //Reset current player
        mViews.gameBoard.setCurrentPlayer(GamePlayer.NONE);
        mViews.gameBoard.postDelayed(() -> {
            if (mViews == null) {
                return;
            }
            playMusic(R.raw.gameover);
        }, MUSIC_DELAY);
    }

    private void setWinnerState(@GamePlayer int gamePlayer) {
        int resId;
        switch (gamePlayer) {
            case GamePlayer.COMPUTER:
                sComputerWinCount++;
                resId = R.string.game_computer_wins;
                break;
            case GamePlayer.USER:
                sPlayerWinCount++;
                resId = R.string.game_player_wins;
                break;
            default:
            case GamePlayer.NONE:
                sTieCount++;
                resId = R.string.game_draw;
        }

        mViews.playerTurn.setText(getString(resId));
        updateCounters();
    }

    private void updateCounters() {
        mViews.playerTieCount.setText(String.valueOf(sTieCount));
        mViews.playerUserCount.setText(String.valueOf(sPlayerWinCount));
        mViews.playerComputerCount.setText(String.valueOf(sComputerWinCount));
    }

    private void resetCounters() {
        sTieCount = 0;
        sPlayerWinCount = 0;
        sComputerWinCount = 0;
        mViews.playerTieCount.setText(String.valueOf(sTieCount));
        mViews.playerUserCount.setText(String.valueOf(sPlayerWinCount));
        mViews.playerComputerCount.setText(String.valueOf(sComputerWinCount));
    }

    private void requestComputersTurn() {
        mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
    }

    private void registerClickListeners() {
        mViews.gameBoard.setCellListener(new GameBoardListener());
        mViews.resetButton.setOnClickListener(view -> {
            resetCounters();
            mViews.gameBoard.resetGameBoard();
            mViews.gameBoard.setEnabled(false);
            mViews.gameBoard.postDelayed(() -> {
                if (mViews == null) {
                    return;
                }
                finishTurn();
            }, COMPUTER_DELAY_MS);

        });
    }

    private void unRegisterClickListeners() {
        mViews.gameBoard.setCellListener(null);
        mViews.resetButton.setOnClickListener(null);
    }

    private void configureTaskDescription() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int topBarColor = ContextCompat.getColor(this, R.color.zeta_md_grey_800);
            ActivityManager.TaskDescription taskDescription =
                    new ActivityManager.TaskDescription(null, null, topBarColor);
            setTaskDescription(taskDescription);
        }
    }

    private class GameBoardListener implements GameBoard.GameBoardListener {
        public void onGridSelected(int position) {
            //Custom view should be told what to be done.
            @GamePlayer
            final int currentPlayer = mViews.gameBoard.getCurrentPlayer();
            if (currentPlayer == GamePlayer.USER) {
                if (position >= 0) {
                    mViews.gameBoard.setNextMove(position, currentPlayer);
                    playMusic(R.raw.notelow);
                    finishTurn();
                }
            }
        }
    }

    private class MessageHandlerCallback implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_COMPUTER_TURN) {

                @GamePlayer
                int[] data = mViews.gameBoard.getPlayerGameData();
                int nextMoveIndex = getNextMoveIndex(data);
                mViews.gameBoard.setNextMove(nextMoveIndex, mViews.gameBoard.getCurrentPlayer());
                playMusic(R.raw.notehigh);
                finishTurn();
                return true;
            }
            return false;
        }
    }

    //This is super simple next move index generator.
    //Replace this with more elegant AI for HIGH, MID, LOW level of complexity
    private int getNextMoveIndex(int[] data) {
        int nextMove = 0;
        int used = 0;
        while (used != 0x1F) {
            int index = mRandom.nextInt(9);
            if (((used >> index) & 1) == 0) {
                used |= 1 << index;
                if (data[index] == GamePlayer.NONE) {
                    nextMove = index;
                    break;
                }
            }
        }
        return nextMove;
    }

    private void playMusic(@RawRes int music) {
        SoundManager.getInstance(getApplicationContext()).play(music);
    }

}
