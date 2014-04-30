package jd.gui.swing.jdgui.oboom;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class OboomDialog extends AbstractDialog<Integer> {

    private ExtTextField input;

    public OboomDialog() {
        super(0, _GUI._.specialdeals_oboom_dialog_title(), new AbstractIcon("logo_oboom_small", 64), _GUI._.lit_continue(), _GUI._.lit_close());
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {

            final ProgressDialog prog = new ProgressDialog(new ProgressGetter() {

                @Override
                public void run() throws Exception {
                    requestAccount(input.getText());

                }

                @Override
                public String getString() {
                    return null;
                }

                @Override
                public int getProgress() {
                    return -1;
                }

                @Override
                public String getLabelString() {
                    return null;
                }
            }, UIOManager.BUTTONS_HIDE_OK, _GUI._.specialdeals_oboom_dialog_request_title(), _GUI._.specialdeals_oboom_dialog_request_msg(), new AbstractIcon("logo_oboom_small", 32), null, null) {
                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }
            };
            new Thread() {
                public void run() {
                    UIOManager.I().show(null, prog);

                };
            }.start();

        }
    }

    protected void requestAccount(String email) {
        try {
            Browser br = new Browser();

            br.getPage("https://www.oboom.com/event/jdownloader?email=" + Encoding.urlEncode(email) + "&http_errors=0");

            if (br.containsHTML("403,\"E_PREMIUM\"")) {
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_e_premium());
                retry();
                return;
            } else if (br.containsHTML("403,\"Forbidden\"")) {
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_forbidden());
                retry();
                return;
            } else if (br.containsHTML("403,\"EMAIL_FORBIDDEN\"")) {
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_email_forbidden());
                retry();
                return;
            } else if (br.containsHTML("403,\"USED\"")) {
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_used());
                retry();
                return;
            } else if (br.containsHTML("200,\"EXISTING\"")) {

            } else {
                Object[] data = JSonStorage.restoreFromString(br.toString(), Object[].class);
                // String[] data = br.getRegex("200,\"OK\",\"(.+?)\",\"(.+)\"").getRow(0);

                final Account ac = new Account(data[2].toString(), data[3].toString());
                ac.setHoster("oboom.com");
                AccountController.getInstance().addAccount(ac);
                try {
                    Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.specialdeals_oboom_dialog_request_success_title(), _GUI._.specialdeals_oboom_dialog_request_success_msg(), new AbstractIcon("logo_oboom", 50), _GUI._.lit_close(), null);
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
            }

            System.out.println(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void retry() {

        new Thread("OSR") {
            public void run() {

                OboomDialog d = new OboomDialog();

                UIOManager.I().show(null, d);
            }
        }.start();
    }

    private JComponent header(String lbl) {
        JLabel ret = SwingUtils.toBold(new JLabel(lbl));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    protected int getPreferredWidth() {
        return 650;
    }

    protected MigPanel createBottomPanel() {
        // TODO Auto-generated method stub
        MigPanel ret = new MigPanel("ins 0", "[]20[grow,fill][]", "[]");

        return ret;
    }

    @Override
    protected DefaultButtonPanel createBottomButtonPanel() {
        DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[]", "0[grow,fill]0");
        JLabel tocs = new JLabel("<html><a href=\"https://www.oboom.com/#agb\">" + _GUI._.specialdeals_oboom_tocs() + "</a></html>");
        tocs.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tocs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CrossSystem.openURL("https://www.oboom.com/#agb");
            }
        });
        ret.add(tocs, "");
        return ret;
    }

    @Override
    public JComponent layoutDialogContent() {

        JPanel content = new JPanel(new MigLayout("ins 0, wrap 1", "[]"));
        content.add(header(_GUI._.specialdeals_oboom_dialog_conditions()), "gapleft 15,pushx,growx");
        content.add(new JLabel("<html>" + _GUI._.specialdeals_oboom_dialog_msg() + "</html>"), "gapleft 32,wmin 10");

        content.add(header(_GUI._.specialdeals_oboom_dialog_email()), "gapleft 15,spanx,pushx,growx,gaptop 15");
        input = new ExtTextField();
        input.setHelpText(_GUI._.specialdeals_oboom_dialog_email_help());
        content.add(input, "gapleft 32,pushx,growx");

        getDialog().addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(final WindowEvent windowevent) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowGainedFocus(final WindowEvent windowevent) {
                final Component focusOwner = getDialog().getFocusOwner();
                if (focusOwner != null) {
                    // dialog component has already focus...
                    return;
                }
                /* we only want to force focus on first window open */
                getDialog().removeWindowFocusListener(this);
                input.requestFocus();
            }
        });
        return content;
    }

}
