package zeta.android.apps.tictactoe.views.common;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.view.View;

import butterknife.ButterKnife;

public class BaseViews {

    private View mRoot;

    protected BaseViews(@NonNull View root) {
        mRoot = root;
        ButterKnife.bind(this, root);
    }

    public View getRootView() {
        return mRoot;
    }

    @CallSuper
    public void clear() {
        ButterKnife.unbind(mRoot);
        mRoot = null;
    }
}
