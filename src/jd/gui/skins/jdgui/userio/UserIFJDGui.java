package jd.gui.skins.jdgui.userio;

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.controlling.JDLogger;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.skins.jdgui.JDGui;
import jd.gui.skins.jdgui.components.Balloon;
import jd.gui.skins.jdgui.swing.GuiRunnable;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.gui.userio.dialog.ContainerDialog;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTitledSeparator;

/*
 * UserIF welches auf der JDGui arbeitet
 */
public class UserIFJDGui extends UserIF {

    private JDGui gui;

    public UserIFJDGui(JDGui gui) {
        super();
        this.gui = gui;
    }

    @Override
    public void requestPanel(byte panelID) {
        gui.requestPanel(panelID);
    }

    @Override
    public void showAccountInformation(final PluginForHost pluginForHost, final Account account) {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                AccountInfo ai;
                try {
                    ai = pluginForHost.getAccountInformation(account);
                } catch (Exception e) {
                    account.setEnabled(false);
                    JDLogger.exception(e);
                    UserIO.getInstance().requestMessageDialog(JDL.LF("gui.accountcheck.pluginerror", "Plugin %s may be defect. Inform support!", pluginForHost.getPluginID()));
                    return null;
                }
                if (ai == null) {
                    UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.host.premium.info.error", "The %s plugin does not support the Accountinfo feature yet.", pluginForHost.getHost()));
                    return null;
                }
                if (!ai.isValid()) {
                    account.setEnabled(false);
                    UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.host.premium.info.notValid", "The account for '%s' isn't valid! Please check username and password!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }
                if (ai.isExpired()) {
                    account.setEnabled(false);
                    UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.host.premium.info.expired", "The account for '%s' is expired! Please extend the account or buy a new one!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }

                String def = JDL.LF("plugins.host.premium.info.title", "Accountinformation from %s for %s", account.getUser(), pluginForHost.getHost());
                String[] label = new String[] { JDL.L("plugins.host.premium.info.validUntil", "Valid until"), JDL.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDL.L("plugins.host.premium.info.files", "Files"), JDL.L("plugins.host.premium.info.premiumpoints", "PremiumPoints"), JDL.L("plugins.host.premium.info.usedSpace", "Used Space"), JDL.L("plugins.host.premium.info.cash", "Cash"), JDL.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left"), JDL.L("plugins.host.premium.info.status", "Info") };

                DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                String validUntil = (ai.isExpired() ? JDL.L("plugins.host.premium.info.expiredInfo", "[expired]") + " " : "") + formater.format(new Date(ai.getValidUntil())) + "";
                if (ai.getValidUntil() == -1) validUntil = null;
                String premiumPoints = ai.getPremiumPoints() + ((ai.getNewPremiumPoints() > 0) ? " [+" + ai.getNewPremiumPoints() + "]" : "");
                String[] data = new String[] { validUntil, Formatter.formatReadable(ai.getTrafficLeft()), ai.getFilesNum() + "", premiumPoints, Formatter.formatReadable(ai.getUsedSpace()), ai.getAccountBalance() < 0 ? null : (ai.getAccountBalance() / 100.0) + " €", Formatter.formatReadable(ai.getTrafficShareLeft()), ai.getStatus() };

                JPanel panel = new JPanel(new MigLayout("ins 5", "[right]10[grow,fill]10[]"));
                panel.add(new JXTitledSeparator("<html><b>" + def + "</b></html>"), "spanx, pushx, growx, gapbottom 15");

                for (int j = 0; j < data.length; j++) {
                    if (data[j] != null && !data[j].equals("-1") && !data[j].equals("-1 B")) {
                        panel.add(new JLabel(label[j]), "gapleft 20");

                        JTextField tf = new JTextField(data[j]);
                        tf.setBorder(null);
                        tf.setBackground(null);
                        tf.setEditable(false);
                        tf.setOpaque(false);

                        if (label[j].equals(JDL.L("plugins.host.premium.info.trafficLeft", "Traffic left"))) {
                            PieChartAPI freeTrafficChart = new PieChartAPI("", 150, 60);
                            freeTrafficChart.addEntity(new ChartAPIEntity(JDL.L("plugins.host.premium.info.freeTraffic", "Free"), ai.getTrafficLeft(), new Color(50, 200, 50)));
                            freeTrafficChart.addEntity(new ChartAPIEntity("", ai.getTrafficMax() - ai.getTrafficLeft(), new Color(150, 150, 150)));
                            freeTrafficChart.fetchImage();

                            panel.add(tf);
                            panel.add(freeTrafficChart, "spany, wrap");
                        } else {
                            panel.add(tf, "span 2, wrap");
                        }
                    }

                }
                new ContainerDialog(UserIO.NO_CANCEL_OPTION, def, panel, null, null);

                return null;
            }
        }.start();
    }

    @Override
    public void displayMiniWarning(String shortWarn, String longWarn) {
        /*
         * TODO: mal durch ein einheitliches notificationo system ersetzen,
         * welches an das eventsystem gekoppelt ist
         */
        Balloon.show(shortWarn, JDTheme.II("gui.images.warning", 32, 32), longWarn);
    }

    @Override
    public void setFrameStatus(int id) {
        // TODO Auto-generated method stub

    }

}
