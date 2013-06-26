package org.jdownloader.updatev2;

import java.util.logging.Level;

import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class RlyExitListener implements ShutdownVetoListener {
    private static final RlyExitListener INSTANCE = new RlyExitListener();

    /**
     * get the only existing instance of RlyExitListener. This is a singleton
     * 
     * @return
     */
    public static RlyExitListener getInstance() {
        return RlyExitListener.INSTANCE;
    }

    private int     dialogFlags;
    private String  text;
    private String  title;
    private boolean enabled;

    /**
     * Create a new instance of RlyExitListener. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private RlyExitListener() {
        this.dialogFlags = UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN;
        this.title = _UPDATE._.rlyexit_title();

        this.text = _UPDATE._.rlyexit();
        this.enabled = false;
    }

    public int getDialogFlags() {
        return this.dialogFlags;
    }

    public String getText() {
        return this.text;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void onShutdown(final boolean silent) {
    }

    @Override
    public void onShutdownVeto(final ShutdownVetoException[] vetos) {

    }

    @Override
    public void onShutdownVetoRequest(final ShutdownVetoException[] vetos) throws ShutdownVetoException {
        if (vetos.length > 0 || !this.isEnabled()) {
            /* we already abort shutdown, no need to ask again */
            return;
        }
        try {
            Dialog.getInstance().showConfirmDialog(this.dialogFlags, this.title, this.text, null, null, null);
            return;
        } catch (final DialogClosedException e) {
            Log.exception(Level.WARNING, e);
        } catch (final DialogCanceledException e) {
            Log.exception(Level.WARNING, e);

        }

        throw new ShutdownVetoException("User aborted!", this);
    }

    @Override
    public void onSilentShutdownVetoRequest(final ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {
    }

    public void setDialogFlags(final int dialogFlags) {
        this.dialogFlags = dialogFlags;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setText(String text) {
        if (text == null) {

            text = _UPDATE._.rlyexit();
        }
        this.text = text;
    }

    public void setTitle(final String text) {
        if (text == null) {
            this.title = _UPDATE._.rlyexit_title();

        }
        this.title = text;
    }

    @Override
    public long getShutdownVetoPriority() {
        return Long.MIN_VALUE;
    }

}
