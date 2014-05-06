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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.AccountController;
import jd.gui.swing.dialog.AddAccountDialog;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class OboomDialog extends AbstractDialog<Integer> {
    
    private static boolean OFFER_IS_ACTIVE = readOfferActive();
    private ExtTextField   input;
    private LogSource      logger;
    
    public OboomDialog() {
        super(0, _GUI._.specialdeals_oboom_dialog_title(), new AbstractIcon("logo_oboom_small", 64), _GUI._.lit_continue(), _GUI._.lit_close());
        logger = LogController.getInstance().getLogger("OboomDeal");
    }
    
    private static boolean readOfferActive() {
        switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                return readRegistry() <= 0 && !Application.getTempResource("oboom1").exists();
            default:
                return !Application.getResource("cfg/deals.json").exists() && !Application.getTempResource("oboom1").exists();
        }
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
    
    public static final AtomicReference<String> KEY   = new AtomicReference<String>("afe38c");
    public static final AtomicReference<byte[]> VALUE = new AtomicReference<byte[]>(new byte[] { 77, 100, 51, 54, 56, 100, 65, 53, 57, 48, 54, 99, 11, 51, 56, 48, 100, 100, 49, 56, 55, 101, 97, 100, 53, 53, 53, 56, 99, 48, 102, 49, 99, 57, 100, 102, 98, 48, 53, 55, 54, 101, 57, 52, 49, 51, 50, 51, 48, 55, 54, 97, 55, 48, 99, 100, 98, 50, 54, 52, 98, 53, 48, 56 });
    
    private static int doLookup(final String hostName) throws NamingException {
        DirContext ictx = null;
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "4000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            ictx = new InitialDirContext(env);
            final Attributes attrs = ictx.getAttributes(hostName, new String[] { "MX" });
            final Attribute attr = attrs.get("MX");
            if (attr == null) {
                Socket socket = null;
                try {
                    /* last chance, no mx record -> let's check for smtp port open */
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(hostName, 25), 1000);
                    return 1;
                } catch (final Throwable ignore) {
                } finally {
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (final Throwable ignore) {
                    }
                }
                return 0;
            }
            return attr.size();
        } finally {
            try {
                if (ictx != null) {
                    ictx.close();
                }
            } catch (final Throwable ignore) {
            }
        }
    }
    
    protected static boolean validateEmail(final String email) {
        if (new Regex(email, "..*?@.*?\\..+").matches()) {
            try {
                final String host = new Regex(email, ".*?@(.+)").getMatch(0);
                if (StringUtils.isEmpty(host)) {
                    return false;
                } else {
                    return doLookup(host) > 0;
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    
    protected void requestAccount(String email) {
        try {
            Browser br = new Browser();
            
            br.getPage("http://stats.appwork.org/data/db/getDealStatus");
            
            if (!br.containsHTML("true") && Application.isJared(null)) {
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_disabled());
                new EDTRunner() {
                    
                    @Override
                    protected void runInEDT() {
                        MainTabbedPane.SPECIAL_DEALS_ENABLED.set(false);
                        MainTabbedPane.getInstance().repaint();
                    }
                };
                return;
            }
            if (!validateEmail(email)) {
                OboomDialog.track("InvalidEmail");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_invalidEmail());
                retry();
                return;
            }
            OboomDialog.track("RequestAccount");
            
            long time = System.currentTimeMillis();
            final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            
            final SecretKeySpec secret_key = new SecretKeySpec(VALUE.get(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String sig = HexFormatter.byteArrayToHex(sha256_HMAC.doFinal((KEY.get() + ":" + email + ":" + time).getBytes("UTF-8")));
            String url = "https://www.oboom.com/event/jdownloader/secure?email=" + Encoding.urlEncode(email) + "&rev=" + Encoding.urlEncode(KEY.get()) + "&sig=" + sig + "&ts=" + time + "&http_errors=0";
            br.getPage(url);
            
            if (br.containsHTML("403,\"E_PREMIUM\"")) {
                OboomDialog.track("Error_E_PREMIUM");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_e_premium());
                setOfferActive();
                return;
            } else if (br.containsHTML("403,\"Forbidden\"")) {
                OboomDialog.track("Error_Forbidden");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_forbidden());
                retry();
                return;
            } else if (br.containsHTML("403,\"EMAIL_FORBIDDEN\"")) {
                OboomDialog.track("Error_EMAIL_FORBIDDEN");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_email_forbidden());
                setOfferActive();
                return;
            } else if (br.containsHTML("403,\"USED\"")) {
                OboomDialog.track("Error_USED");
                Dialog.getInstance().showMessageDialog(0, _GUI._.lit_error_occured(), _GUI._.specialdeals_oboom_dialog_request_error_used());
                setOfferActive();
                return;
            } else if (br.containsHTML("200,\"EXISTING\"")) {
                setOfferActive();
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
                        found.setProperty("DEAL", System.currentTimeMillis());
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
                        ac.setProperty("DEAL", System.currentTimeMillis());
                        PluginForHost proto = HostPluginController.getInstance().get("oboom.com").getPrototype(null);
                        AddAccountDialog.showDialog(proto, ac);
                    }
                }
            } else {
                setOfferActive();
                OboomDialog.track("OK_NEW");
                Object[] data = JSonStorage.restoreFromString(br.toString(), Object[].class);
                final Account ac = new Account(data[2].toString(), data[3].toString());
                ac.setHoster("oboom.com");
                ac.setProperty("DEAL", System.currentTimeMillis());
                AccountController.getInstance().addAccount(ac);
                Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.specialdeals_oboom_dialog_request_success_title(), _GUI._.specialdeals_oboom_dialog_request_success_msg(), new AbstractIcon("logo_oboom", 100), _GUI._.lit_continue(), null);
            }
            
            System.out.println(1);
        } catch (DialogNoAnswerException e) {
            logger.log(e);
        } catch (Throwable e) {
            logger.log(e);
            OboomDialog.track("ERROR_EXCEPTION_" + e.getMessage());
        }
    }
    
    private void setOfferActive() {
        OFFER_IS_ACTIVE = false;
        switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                writeRegistry(1);
                break;
            default:
                
                if (!Application.getResource("cfg/deals.json").exists()) {
                    try {
                        IO.writeStringToFile(Application.getResource("cfg/deals.json"), System.currentTimeMillis() + "");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
        }
        
        MainTabbedPane.getInstance().repaint();
    }
    
    public static int readRegistry() {
        try {
            final String iconResult = IO.readInputStreamToString(Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\JDownloader\" /v \"deal1\"").getInputStream());
            final Matcher matcher = Pattern.compile("deal1\\s+REG_DWORD\\s+0x(.*)").matcher(iconResult);
            matcher.find();
            final String value = matcher.group(1);
            return Integer.parseInt(value, 16);
        } catch (Throwable e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    public static void writeRegistry(long value) {
        try {
            
            final Process p = Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\JDownloader\" /v \"deal1\" /t REG_DWORD /d 0x" + Long.toHexString(value) + " /f");
            IO.readInputStreamToString(p.getInputStream());
            final int exitCode = p.exitValue();
            if (exitCode == 0) {
                
            } else {
                throw new IOException("Reg add execution failed");
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
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
    
    public static boolean isOfferActive() {
        return OFFER_IS_ACTIVE;
    }
}
