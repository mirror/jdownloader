package jd.controlling.reconnect;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessCallBackAdapter implements ProcessCallBack {
    private AtomicBoolean methodConfirmEnabled = new AtomicBoolean(true);

    @Override
    public void setProgress(final Object caller, final int percent) {
    }

    @Override
    public void setStatus(final Object caller, final Object statusObject) {
    }

    @Override
    public void setStatusString(final Object caller, final String string) {
    }


    @Override
    public void setMethodConfirmEnabled(boolean b) {
        methodConfirmEnabled.set(b);
    }

    @Override
    public boolean isMethodConfirmEnabled() {
        return methodConfirmEnabled.get();
    }
}
