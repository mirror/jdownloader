package org.jdownloader.gui.donate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jd.http.Browser;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class DonateFeedback {

    public static void reportFailed(Throwable e) {
        try {
            InputDialog d = null;

            d = new InputDialog(Dialog.STYLE_LARGE, _GUI._.DonateFeedback_reportFailed_title_(), _GUI._.DonateFeedback_reportFailed_title_message(), null, new AbstractIcon(IconKey.ICON_QUESTION, 32), _GUI._.lit_send(), null);

            InputDialogInterface response = UIOManager.I().show(InputDialogInterface.class, d);

            response.throwCloseExceptions();

            String txt = response.getText();
            if (e != null) {
                txt += "\r\n\r\nException: \r\n" + Exceptions.getStackTrace(e);
            }
            if (StringUtils.isNotEmpty(txt)) {
                Browser br = new Browser();
                br.postPageRaw("http://update3.jdownloader.org/jdserv/RedirectInterface/submitKayakoTicket", URLEncode.encodeRFC2396("noreply@jdownloader.org") + "&" + URLEncode.encodeRFC2396("Unknown User") + "&" + URLEncode.encodeRFC2396("Failed Transaction Feedback") + "&" + URLEncode.encodeRFC2396(txt));
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    public static void reportCanceled() {
        try {
            InputDialog d = null;

            d = new InputDialog(Dialog.STYLE_LARGE, _GUI._.DonateFeedback_reportCanceled_title_(), _GUI._.DonateFeedback_reportCanceled_title_message(), null, new AbstractIcon(IconKey.ICON_QUESTION, 32), _GUI._.lit_send(), null);

            InputDialogInterface response = UIOManager.I().show(InputDialogInterface.class, d);

            response.throwCloseExceptions();

            String txt = response.getText();

            if (StringUtils.isNotEmpty(txt)) {
                Browser br = new Browser();
                br.postPageRaw("http://update3.jdownloader.org/jdserv/RedirectInterface/submitKayakoTicket", URLEncode.encodeRFC2396("noreply@jdownloader.org") + "&" + URLEncode.encodeRFC2396("Unknown User") + "&" + URLEncode.encodeRFC2396("Canceled Transaction Feedback") + "&" + URLEncode.encodeRFC2396(txt));
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
