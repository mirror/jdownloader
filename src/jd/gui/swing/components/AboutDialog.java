//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDImage;
import jd.nutils.io.JDIO;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class AboutDialog extends AbstractDialog {

    public static void main(String[] args) {
        UserIO.setInstance(UserIOGui.getInstance());
        new AboutDialog();
    }

    private static final String JDL_PREFIX = "jd.gui.swing.components.AboutDialog.";
    private final boolean SHOW_MAIL = false;

    public AboutDialog() {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_OK_OPTION | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "title", "About JDownloader"), null, null, null);

        init();
    }

    private static final long serialVersionUID = -7647771640756844691L;

    public JComponent contentInit() {
        JPanel cp = new JPanel(new MigLayout("ins 10 10 0 10, wrap 2"));

        cp.add(new JLabel(JDImage.getImageIcon("logo/jd_logo_128_128")), "spany 4, gapright 10");

        JLabel lbl;
        cp.add(lbl = new JLabel(JDL.L(JDL_PREFIX + "name", "JDownloader")), "gaptop 15");
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize() * 2.0f));

        cp.add(new JLabel(JDL.LF(JDL_PREFIX + "version", "Version %s", JDUtilities.getRevision())));

        cp.add(new JLabel("© AppWork UG (haftungsbeschränkt) 2007-2009"), "gaptop 5");

        JButton btn;
        cp.add(btn = Factory.createButton(JDL.L(JDL_PREFIX + "license", "Show license"), JDTheme.II("gui.images.premium", 16, 16), new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String license = JDIO.getLocalFile(JDUtilities.getResourceFile("licenses/jdownloader.license"));
                UserIO.getInstance().requestMessageDialog(JDL.L(JDL_PREFIX + "license.title", "JDownloader License"), license);
            }

        }), "gaptop 15, split 3");
        btn.setBorder(null);

        cp.add(new JLink(JDL.L(JDL_PREFIX + "homepage", "Homepage"), JDL.L("gui.dialog.about.homeurl", "http://www.jdownloader.org/home?lng=en")), "gapleft 10");
        cp.add(new JLink(JDL.L(JDL_PREFIX + "forum", "Support board"), JDL.L("gui.dialog.about.forumurl", "http://board.jdownloader.org")), "gapleft 10");

        cp.add(new JSeparator(), "growx, spanx, gaptop 10");
        JPanel contribs = new JPanel(new MigLayout("ins 10 10 0 10", "[]20[]"));
        contribs.add(getEntryPanel(JDL.L(JDL_PREFIX + "developers", "Developers"), getDevelopers()), "w 200!, h 200!");
        contribs.add(getEntryPanel(JDL.L(JDL_PREFIX + "othercontributers", "Other contributers"), getSupport()), "w 200!, h 200!");
        cp.add(contribs, "spanx 2");

        return cp;
    }

    protected void packed() {
        this.remove(countDownLabel);
        this.pack();
        // this.setSize(800, 450);
        this.setDefaultCloseOperation(AbstractDialog.DISPOSE_ON_CLOSE);
    }

    private JComponent getEntryPanel(String title, Entry[] entries) {
        JPanel cp = new JPanel(new MigLayout("ins 0, wrap 1"));

        JLabel lbl;
        cp.add(lbl = new JLabel(title));
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize() * 1.5f));
        cp.add(new JSeparator(), "growx, spanx");

        JPanel sp = new JPanel(new MigLayout("ins 0 15 0 0, wrap 1"));
        for (Entry entry : entries) {
            sp.add(entry.toJComponent());
        }
        cp.add(new JScrollPane(sp), "w 200!");

        return cp;
    }

    private Entry[] getDevelopers() {
        ArrayList<Entry> devs = new ArrayList<Entry>();

        devs.add(new Entry("coalado", "coalado@jdownloader.org", "JDownloader core, Framework, OCR, Swing GUI, Reconnect, Container, Homepage, Project Administration"));

        devs.add(new Entry("Botzi", "botzi@jdownloader.org", "Hoster / Decrypter / Addons, Bugfixing, Database backend, No Support"));
        devs.add(new Entry("DwD", "dwd@jdownloader.org", "Hoster, OCR, Decrypter, Extractor, Reconnect"));
        devs.add(new Entry("jiaz", "jiaz@jdownloader.org", "JDownloader core, Framework, Addons/Plugins, Support, Server Administration"));
        devs.add(new Entry("Greeny", "greeny@jdownloader.org", "Swing GUI, Bugfixing, LangFileEditor, Addons/Plugins, Support"));
        devs.add(new Entry("scr4ve", "scr4ve@jdownloader.org", "Security Stuff, Addons, Decrypter-Plugins, Support, Bugfixing"));

        devs.add(new Entry("gocsp", "gocsp@jdownloader.org", "Mac Developer"));
        devs.add(new Entry("gluewurm", null, "Developing innovative ideas, Bugfixing, Technical-Feasibility-Advisor"));
        devs.add(new Entry("jago", "jago@jdownloader.org", "Swing GUI"));
        devs.add(new Entry("djuzi", "djuzi@jdownloader.org", "Hoster / Decrypter, Bugfixing, Localizing, Polish Translation"));
        devs.add(new Entry("eXecuTe", "jd.execute@gmail.com", "Command Line Support, Some Plugins & Addons"));
        devs.add(new Entry("ManiacMansion", "ManiacMansion@jdownloader.org", "OCR/AntiCaptcha, Hoster / Decrypter, Bugfixing"));

        devs.add(new Entry("Sheadox", "sheadox@jdownloader.org", "Hoster / Decrypter, Support"));
        devs.add(new Entry("Viperb0y", "support@jdownloader.org", "Hoster / Decrypter, Support, Bugfixing"));
        devs.add(new Entry("Gamewalker", null, "Hoster / Decrypter, Support"));
        devs.add(new Entry("Gigant", "gigant@jdownloader.org", "Hoster / Decrypter, Support, Bugfixing"));

        return devs.toArray(new Entry[] {});
    }

    private Entry[] getSupport() {
        ArrayList<Entry> devs = new ArrayList<Entry>();

        devs.add(new Entry("Trazo", "ancoar@gmail.com", "Logo Design (v3)"));
        devs.add(new Entry("Freeloader", null, "Turkish Translation, Homepage Translation"));
        devs.add(new Entry("Muelas", null, "Spanish Translation, Homepage Translation"));
        devs.add(new Entry("Thartist", null, "Spanish Translation, Homepage Translation"));
        devs.add(new Entry("Firx", null, "Russian Translation"));
        devs.add(new Entry("Now Y-Dr", null, "Spanish Translation"));
        devs.add(new Entry("Jaak", null, "Dutch Translation"));
        devs.add(new Entry("Moktar", null, "Arabic Translation"));
        devs.add(new Entry("Sna696", null, "Italian Translation"));
        devs.add(new Entry("Giandena", null, "Italian Translation"));
        devs.add(new Entry("nguyenkimvy", null, "Vietnamese Translation"));
        devs.add(new Entry("Mark James", null, "Silky & Flag Icons from http://www.famfamfam.com"));

        return devs.toArray(new Entry[] {});
    }

    private class Entry implements Comparable<Entry> {

        private String name;
        private String mail;
        private String desc;

        public Entry(String name, String mail, String desc) {
            this.name = name;
            this.mail = mail;
            this.desc = desc;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }

        public int compareTo(Entry o) {
            return name.compareToIgnoreCase(o.getName());
        }

        public JComponent toJComponent() {
            JComponent comp;
            if (SHOW_MAIL && mail != null && !mail.equals("-")) {
                comp = new JLink(name, "mailto:" + mail);
            } else {
                comp = new JLabel(name);
            }
            comp.setToolTipText(desc);
            return comp;
        }

    }

}
