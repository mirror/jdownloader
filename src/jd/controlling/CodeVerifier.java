package jd.controlling;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Hash;
import org.appwork.utils.locale.Loc;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

/**
 * Checks files and resources, if they are valid. If not, user is asked if he
 * wants to import them
 * 
 * @author thomas
 */
public class CodeVerifier {
    public enum State {
        /** not tested */
        NOT_VERIFIED,
        /** asked user, and he trusted the file */
        USER_DECIDED_TRUSTED,
        /** asked user, and he decided not to trust the file */
        USER_DECIDED_NOT_TRUSTED,
        /** whitelist (server) ok */
        TRUSTED;
    }

    private final Storage             storage;
    private final Browser             br;

    private static final CodeVerifier INSTANCE = new CodeVerifier();

    public static CodeVerifier getInstance() {
        return INSTANCE;
    }

    private CodeVerifier() {
        this.storage = JSonStorage.getStorage("CodeVerifier");
        this.br = new Browser();
    }

    /**
     * calls jd server to verify the hash
     * 
     * @param hash
     * @param file
     * @return
     * @throws IOException
     */
    private State checkWhitelist(final String hash, final File file) throws IOException {
        // perhaps we should allow paralell requests. but we cannot use one
        // single Browser instance then
        synchronized (this.br) {
            this.br.getPage("http://service.jdownloader.org/verify/" + hash + "/" + file.getName());
            return State.valueOf(this.br.toString().trim());
        }
    }

    public boolean isJarAllowed(final File file) throws NoSuchAlgorithmException, IOException {
        if (true) return true;

        final String hash = Hash.getMD5(file);
        if (hash == null) { return false; }

        State state = this.storage.get(hash, State.NOT_VERIFIED);
        if (state == State.NOT_VERIFIED) {
            try {
                state = this.checkWhitelist(hash, file);
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        if (state != State.TRUSTED && state != State.USER_DECIDED_TRUSTED) {
            final ConfirmDialog dialog = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, "Unknown author", Loc.LF("jd.controlling.CodeVerifier.isJarAllowed.message", "The file %s is not an offical part of JDownloader.\r\n\r\nDo only accept and load it, if you trust the author or content provider!\r\nLoad this file?", file.getName()), null, null, null) {

                private static final long serialVersionUID = 1L;

                @Override
                protected String getDontShowAgainKey() {
                    // override to have an hash dependend don't
                    // show_again_trigger key
                    return "ABSTRACTDIALOG_DONT_SHOW_AGAIN_" + hash;
                }
            };

            Integer ret;
            try {
                ret = Dialog.getInstance().showDialog(dialog);
                ret = Dialog.getInstance().showConfirmDialog(0, Loc.LF("jd.controlling.CodeVerifier.isJarAllowed.rlymessage", "The untrusted file %s will be loaded now.\r\nAre you sure that you want to load this file?", file.getName()));

            } catch (DialogClosedException e) {
                state = State.USER_DECIDED_NOT_TRUSTED;
                this.storage.put(hash, state);
            } catch (DialogCanceledException e) {
                state = State.USER_DECIDED_NOT_TRUSTED;
                this.storage.put(hash, state);
            }

            return state == State.USER_DECIDED_TRUSTED || state == State.TRUSTED;
        }
        return false;
    }

}
