package jd.controlling.reconnect;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessCallBackAdapter implements ProcessCallBack {
    private AtomicBoolean methodConfirmEnabled = new AtomicBoolean(true);

    @Override
    public void setProgress(final Object caller, final int percent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStatus(final Object caller, final Object statusObject) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStatusString(final Object caller, final String string) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.event.ProcessCallBack#setMethodConfirmEnabled(boolean)
     */
    @Override
    public void setMethodConfirmEnabled(boolean b) {
        methodConfirmEnabled.set(b);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.event.ProcessCallBack#isMethodConfirmEnabled()
     */
    @Override
    public boolean isMethodConfirmEnabled() {

        return methodConfirmEnabled.get();
    }

}
