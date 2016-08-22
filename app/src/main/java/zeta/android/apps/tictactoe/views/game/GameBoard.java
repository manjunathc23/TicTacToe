package zeta.android.apps.tictactoe.views.game;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import zeta.android.apps.tictactoe.R;

public class GameBoard extends View {

    public static final int INVALID = -1;

    private static final int MARGIN = 10;
    private static final int GRID_ROW_COLUMN = 3;

    private int mBoardBlinkTimes = 0;
    private boolean mBlinkBroadLineOff;

    private int mStartXY;
    private int mOffsetX;
    private int mOffsetY;

    private Paint mLinePaint;
    private Paint mBitmapPaint;
    private Paint mWiningPaint;

    private Bitmap mBitmapPlayer1;
    private Bitmap mBitmapPlayer2;

    @GamePlayer
    private int mWinnerPlayer = GamePlayer.NONE;
    @GamePlayer
    private int mCurrentPlayer = GamePlayer.NONE;

    private int mSelectedCell = INVALID;
    private int mWiningRow = INVALID;
    private int mWiningColumn = INVALID;
    private int mWiningDiagonal = INVALID;

    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();

    @GamePlayer
    private final int[] mPlayerGameData = new int[GRID_ROW_COLUMN * GRID_ROW_COLUMN];

    private static final int MSG_BOARD_BLINK = 1;
    private final Handler mHandler = new Handler(new MessageQueueHandler());
    private final long MESSAGE_BLINK_DELAY_MS = TimeUnit.MILLISECONDS.toMillis(250);

    private GameBoardListener mGameBoardListener;

    public interface GameBoardListener {
        void onGridSelected(int position);
    }

    public GameBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();
        initView();
        initDeveloperPreview();
    }

    public void setCellListener(GameBoardListener cellListener) {
        mGameBoardListener = cellListener;
    }

    @GamePlayer
    public int[] getPlayerGameData() {
        return mPlayerGameData;
    }

    public void setNextMove(int nextMoveIndex, @GamePlayer int value) {
        mPlayerGameData[nextMoveIndex] = value;
        invalidate();
    }

    @GamePlayer
    public int getCurrentPlayer() {
        return mCurrentPlayer;
    }

    public void setCurrentPlayer(@GamePlayer int gamePlayer) {
        mCurrentPlayer = gamePlayer;
        mSelectedCell = INVALID;
    }

    @GamePlayer
    public int getWinner() {
        return mWinnerPlayer;
    }

    public void setWinner(@GamePlayer int winner) {
        mWinnerPlayer = winner;
    }

    public void setGameFinished(int col, int row, int diagonal) {
        mWiningColumn = col;
        mWiningRow = row;
        mWiningDiagonal = diagonal;
    }

    public void resetGameBoard() {
        initializeSelectedCell();
        initializePlayerGameData();
        initializeWinningRowColumnDialogData();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int startXY = mStartXY;
        int offsetX = mOffsetX;
        int offsetY = mOffsetY;
        int var = startXY * GRID_ROW_COLUMN;

        //#1 Draw board line
        for (int i = 0, k = startXY; i < 2; i++, k += startXY) {
            if (mBlinkBroadLineOff) {
                continue;
            }
            canvas.drawLine(offsetX, offsetY + k, offsetX + var - 1, offsetY + k, mLinePaint);
            canvas.drawLine(offsetX + k, offsetY, offsetX + k, offsetY + var - 1, mLinePaint);
        }

        //#2 Player Moves
        for (int j = 0, k = 0, y = offsetY; j < 3; j++, y += startXY) {
            for (int i = 0, x = offsetX; i < 3; i++, k++, x += startXY) {
                mDstRect.offsetTo(MARGIN + x, MARGIN + y);
                @GamePlayer
                int player;
                player = mPlayerGameData[k];
                switch (player) {
                    case GamePlayer.USER:
                        if (mBitmapPlayer1 != null) {
                            canvas.drawBitmap(mBitmapPlayer1, mSrcRect, mDstRect, mBitmapPaint);
                        }
                        break;
                    case GamePlayer.COMPUTER:
                        if (mBitmapPlayer2 != null) {
                            canvas.drawBitmap(mBitmapPlayer2, mSrcRect, mDstRect, mBitmapPaint);
                        }
                        break;
                    case GamePlayer.NONE:
                        //No op
                        break;
                }
            }
        }

        //#2 Winning strike though

        if (mWiningRow >= 0) {
            int y = offsetY + mWiningRow * startXY + startXY / 2;
            canvas.drawLine(offsetX + MARGIN, y, offsetX + var - 1 - MARGIN, y, mWiningPaint);

        } else if (mWiningColumn >= 0) {
            int x = offsetX + mWiningColumn * startXY + startXY / 2;
            canvas.drawLine(x, offsetY + MARGIN, x, offsetY + var - 1 - MARGIN, mWiningPaint);

        } else if (mWiningDiagonal == 0) {
            // diagonal 0 is from (0,0) to (2,2)

            canvas.drawLine(offsetX + MARGIN, offsetY + MARGIN,
                    offsetX + var - 1 - MARGIN, offsetY + var - 1 - MARGIN, mWiningPaint);

        } else if (mWiningDiagonal == 1) {
            // diagonal 1 is from (0,2) to (2,0)
            canvas.drawLine(offsetX + MARGIN, offsetY + var - 1 - MARGIN,
                    offsetX + var - 1 - MARGIN, offsetY + MARGIN, mWiningPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int measuredHeightWidth = ((width == 0) ? height : ((height == 0) ? width : (width < height ? width : height)));
        //Square, height == width
        setMeasuredDimension(measuredHeightWidth, measuredHeightWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int startX = (w - 2 * MARGIN) / GRID_ROW_COLUMN;
        int startY = (h - 2 * MARGIN) / GRID_ROW_COLUMN;

        int size = startX < startY ? startX : startY;

        mStartXY = size;

        mOffsetX = (w - GRID_ROW_COLUMN * size) / 2;
        mOffsetY = (h - GRID_ROW_COLUMN * size) / 2;

        mDstRect.set(MARGIN, MARGIN, size - MARGIN, size - MARGIN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            int startXY = mStartXY;
            x = (x - MARGIN) / startXY;
            y = (y - MARGIN) / startXY;

            if (isEnabled() && x >= 0 && x < GRID_ROW_COLUMN && y >= 0 & y < GRID_ROW_COLUMN) {
                mSelectedCell = x + GRID_ROW_COLUMN * y;
                //Inform listener that selection is been made
                if (mGameBoardListener != null && (mPlayerGameData[mSelectedCell] == GamePlayer.NONE)) {
                    mGameBoardListener.onGridSelected(mSelectedCell);
                }
            }
            return true;
        }
        return false;
    }

    public void startBoardBlink(int times) {
        mBoardBlinkTimes = times;
        mBoardBlinkTimes += (times % 2 == 0) ? 2 : 1;
        sendBlinkMessageToQueue();
    }

    private void initView() {
        final Context context = getContext();
        int redColor = ContextCompat.getColor(context, R.color.zeta_md_red_A700);
        int yellowColor = ContextCompat.getColor(context, R.color.zeta_md_yellow_500);

        mBitmapPlayer1 = getBitmapInternal(R.drawable.ic_close_white_48dp);
        mBitmapPlayer2 = getBitmapInternal(R.drawable.ic_panorama_fish_eye_white_48dp);

        if (mBitmapPlayer1 != null) {
            mSrcRect.set(0, 0, mBitmapPlayer1.getWidth() - 1, mBitmapPlayer1.getHeight() - 1);
        }

        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(yellowColor);
        mLinePaint.setStrokeWidth(5);
        mLinePaint.setStyle(Style.STROKE);

        mWiningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWiningPaint.setColor(redColor);
        mWiningPaint.setStrokeWidth(10);
        mWiningPaint.setStyle(Style.STROKE);

        initializeSelectedCell();
        initializePlayerGameData();
        initializeWinningRowColumnDialogData();
    }

    private void initializePlayerGameData() {
        for (int i = 0; i < mPlayerGameData.length; i++) {
            mPlayerGameData[i] = GamePlayer.NONE;
        }
    }

    private void initializeWinningRowColumnDialogData() {
        mWiningColumn = INVALID;
        mWiningRow = INVALID;
        mWiningDiagonal = INVALID;
    }

    private void initializeSelectedCell() {
        mSelectedCell = INVALID;
    }

    private void initDeveloperPreview() {
        if (!isInEditMode()) {
            return;
        }
        //Just to show random moves on the board. This helps in preview in the studio editor
        Random rnd = new Random();
        for (int i = 0; i < mPlayerGameData.length; i++) {
            mPlayerGameData[i] = rnd.nextInt(GRID_ROW_COLUMN) % 2 == 0 ? GamePlayer.USER : GamePlayer.COMPUTER;
        }
    }

    //region get bitmap

    private Bitmap getBitmapInternal(int bmpResId) {
        Resources res = getResources();
        Bitmap bmp = decodeSampledBitmapFromResource(res, bmpResId, mStartXY, mStartXY);
        if (bmp == null) {
            return getBitmapForDeveloperPreview(bmpResId);
        }
        return bmp;
    }

    @Nullable
    private Bitmap getBitmapForDeveloperPreview(int bmpResId) {
        if (!isInEditMode()) {
            return null;
        }
        Drawable d = ContextCompat.getDrawable(getContext(), bmpResId);
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        d.setBounds(0, 0, w - 1, h - 1);
        d.draw(c);
        return bitmap;
    }

    /**
     * https://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */
    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        if (reqWidth == 0 || reqHeight == 0) {
            return 1;
        }
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    //endregion

    //region blinking

    private class MessageQueueHandler implements Callback {
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BOARD_BLINK:
                    if (mSelectedCell >= 0 && mWinnerPlayer != GamePlayer.NONE) {
                        return false;
                    }
                    if (mBoardBlinkTimes <= 0) {
                        removeBlinkMessageFromQueue();
                        return false;
                    }
                    mBlinkBroadLineOff = !mBlinkBroadLineOff;
                    mBoardBlinkTimes--;
                    sendBlinkMessageToQueue();
                    invalidate();
                    return true;
                default:
                    return false;
            }
        }
    }

    private void sendBlinkMessageToQueue() {
        Message message = mHandler.obtainMessage(MSG_BOARD_BLINK);
        mHandler.sendMessageDelayed(message, MESSAGE_BLINK_DELAY_MS);
    }

    private void removeBlinkMessageFromQueue() {
        mHandler.removeMessages(MSG_BOARD_BLINK);
    }

    //endregion
}
