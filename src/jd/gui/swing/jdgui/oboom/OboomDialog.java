package jd.gui.swing.jdgui.oboom;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.AccountController;
import jd.gui.swing.dialog.AddAccountDialog;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
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
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class OboomDialog extends AbstractDialog<Integer> {

    private ExtTextField input;
    private LogSource    logger;

    public OboomDialog() {
        super(0, _GUI._.specialdeals_oboom_dialog_title(), new AbstractIcon("logo_oboom_small", 64), _GUI._.lit_continue(), _GUI._.lit_close());
        logger = LogController.getInstance().getLogger("OboomDeal");
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

            OboomDialog.track("RequestAccount");
            Browser br = new Browser();

            br.getPage("https://www.oboom.com/event/jdownloader?email=" + Encoding.urlEncode(email) + "&http_errors=0");

            if (br.containsHTML("403,\"E_PREMIUM\"")) {
                OboomDialog.track("Error_E_PREMIUM");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_e_premium());
                retry();
                return;
            } else if (br.containsHTML("403,\"Forbidden\"")) {
                OboomDialog.track("Error_Forbidden");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_forbidden());
                retry();
                return;
            } else if (br.containsHTML("403,\"EMAIL_FORBIDDEN\"")) {
                OboomDialog.track("Error_EMAIL_FORBIDDEN");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_email_forbidden());
                retry();
                return;
            } else if (br.containsHTML("403,\"USED\"")) {
                OboomDialog.track("Error_USED");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_used());
                retry();
                return;
            } else if (br.containsHTML("200,\"EXISTING\"")) {
                OboomDialog.track("OK_EXISTING");
                ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts("oboom.com");
                if (accounts != null) {
                    Account found = null;
                    for (Account acc : accounts) {
                        if (StringUtils.equalsIgnoreCase(acc.getUser(), email)) {
                            found = acc;
                            break;
                        }
                    }
                    if (found != null) {

                        try {
                            Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.specialdeals_oboom_dialog_request_success_title(), _GUI._.specialdeals_oboom_dialog_request_successupdate_msg(), new AbstractIcon("logo_oboom", 100), _GUI._.lit_close(), null);
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }
                        AccountController.getInstance().updateAccountInfo(found, true);

                    } else {

                        Dialog.getInstance().showConfirmDialog(0, _GUI._.specialdeals_oboom_dialog_request_success_title(), _GUI._.specialdeals_oboom_dialog_request_successupdate_input_msg(), new AbstractIcon("logo_oboom", 100), _GUI._.lit_continue(), null);

                        final Account ac = new Account(email, "");
                        ac.setHoster("oboom.com");
                        PluginForHost proto = HostPluginController.getInstance().get("oboom.com").getPrototype(null);
                        AddAccountDialog.showDialog(proto, ac);
                    }
                }
            } else {
                OboomDialog.track("OK_NEW");
                Object[] data = JSonStorage.restoreFromString(br.toString(), Object[].class);

                final Account ac = new Account(data[2].toString(), data[3].toString());
                ac.setHoster("oboom.com");
                AccountController.getInstance().addAccount(ac);

                Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.specialdeals_oboom_dialog_request_success_title(), _GUI._.specialdeals_oboom_dialog_request_success_msg(), new AbstractIcon("logo_oboom", 100), _GUI._.lit_continue(), null);

            }

            System.out.println(1);
        } catch (IOException e) {
            logger.log(e);

            OboomDialog.track("ERROR_EXCEPTION_" + e.getMessage());
        } catch (UpdateRequiredClassNotFoundException e) {

            logger.log(e);

            OboomDialog.track("ERROR_EXCEPTION_" + e.getMessage());
        } catch (DialogClosedException e) {
            logger.log(e);
        } catch (DialogCanceledException e) {
            logger.log(e);
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
        JLabel tocs = new JLabel("<html><font color=\"#999999\"><a font href=\"https://www.oboom.com/#agb\">" + _GUI._.specialdeals_oboom_tocs() + "</a></font></html>");
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
    protected DefaultButtonPanel createBottomButtonPanel() {
        DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[]", "0[grow,fill]0");

        return ret;
    }

    @Override
    public JComponent layoutDialogContent() {

        JPanel content = new JPanel(new MigLayout("ins 0, wrap 1", "[]"));
        content.add(header(_GUI._.specialdeals_oboom_dialog_conditions()), "gapleft 15,pushx,growx");
        content.add(new JLabel("<html>" + _GUI._.specialdeals_oboom_dialog_msg() + "</html>"), "gapleft 32,wmin 10");

        content.add(header(_GUI._.specialdeals_oboom_dialog_email()), "gapleft 15,spanx,pushx,growx,gaptop 15");
        input = new ExtTextField();
        input.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okButton.doClick();
            }
        });
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

    public static void track(final String string) {
        new Thread() {
            public void run() {
                try {
                    new Browser().getPage("http://stats.appwork.org/piwik/piwik.php?idsite=3&rec=1&action_name=specialdeals/oboom1/" + Encoding.urlEncode(string));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
