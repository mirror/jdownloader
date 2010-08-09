//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.AbstractDialog;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

/**
 * Diese Klasse zeigt dem Nutzer Auswahldialoge beim Konvertieren von FLVs an <br>
 * TODO: Use {@link jd.gui.swing.dialog.ConvertDialog} instead. Just remaining
 * for backward compatibility. FIXME: Remove with next Major Update!
 */
public class ConvertDialog extends AbstractDialog {

    /**
     * Zeigt Auswahldialog an. wenn keepformat aktiviert ist wird gespeichtertes
     * Input zurückgegeben, es sei denn, andere Formate stehen zur Auswahl
     */
    public static ConversionMode displayDialog(final ArrayList<ConversionMode> modes, final String name) {
        if (modes.size() == 0) return null;
        if (modes.size() == 1) return modes.get(0);

        if (ConvertDialog.keepformat) {
            boolean newFormatChoosable = false;
            if (!ConvertDialog.forcekeep) {
                for (int i = 0; i < modes.size(); i++) {
                    if (!keeped_availablemodes.contains(modes.get(i))) {
                        newFormatChoosable = true;
                        break;
                    }
                }
            }
            if (!newFormatChoosable) {
                for (int i = 0; i < keeped.size(); i++) {
                    if (modes.contains(keeped.get(i))) return keeped.get(i);
                }
            }
        }

        GuiRunnable<ConversionMode> run = new GuiRunnable<ConversionMode>() {
            @Override
            public ConversionMode runSave() {
                return new ConvertDialog(modes, name).getReturnMode();
            }
        };
        return run.getReturnValue();
    }

    private static final long serialVersionUID = -9146764850581039090L;

    public static enum ConversionMode {
        AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }),
        VIDEOFLV("Video (FLV)", new String[] { ".flv" }),
        AUDIOMP3_AND_VIDEOFLV("Audio & Video (MP3 & FLV)", new String[] { ".mp3", ".flv" }),
        VIDEOMP4("Video (MP4)", new String[] { ".mp4" }),
        VIDEOWEBM("Video (Webm)", new String[] { ".webm" }),
        VIDEO3GP("Video (3GP)", new String[] { ".3gp" }),
        VIDEOPODCAST("Video (MP4-Podcast)", new String[] { ".mp4" }),
        VIDEOIPHONE("Video (iPhone)", new String[] { ".mp4" });

        private String text;
        private String[] ext;

        ConversionMode(String text, String[] ext) {
            this.text = text;
            this.ext = ext;
        }

        @Override
        public String toString() {
            return text;
        }

        public String getText() {
            return text;
        }

        public String getExtFirst() {
            return ext[0];
        }

    }

    private static boolean forcekeep = false;
    private static ArrayList<ConversionMode> keeped = new ArrayList<ConversionMode>();
    private static ArrayList<ConversionMode> keeped_availablemodes = new ArrayList<ConversionMode>();
    private static boolean keepformat = false;

    private static void setKeepformat(boolean newkeepformat) {
        if (newkeepformat == false) {
            ConvertDialog.keepformat = false;
            ConvertDialog.setForceKeep(false);
            ConvertDialog.keeped_availablemodes = new ArrayList<ConversionMode>();
            ConvertDialog.keeped = new ArrayList<ConversionMode>();
        } else {
            ConvertDialog.keepformat = true;
        }
    }

    private static void setForceKeep(boolean newforcekeep) {
        ConvertDialog.forcekeep = newforcekeep;
    }

    private static boolean hasKeeped() {
        if (keepformat) return !keeped.isEmpty();
        return false;
    }

    private static void addKeeped(ConversionMode FormatToAdd, ArrayList<ConversionMode> FormatsInList, boolean TopPriority) {
        ConvertDialog.keepformat = true;
        if (TopPriority) {
            ConvertDialog.keeped.add(0, FormatToAdd);
        } else {
            ConvertDialog.keeped.add(FormatToAdd);
        }

        for (int i = 0; i < FormatsInList.size(); i++) {
            if (!ConvertDialog.keeped_availablemodes.contains(FormatsInList.get(i))) {
                ConvertDialog.keeped_availablemodes.add(FormatsInList.get(i));
            }
        }
    }

    public static ArrayList<ConversionMode> getKeeped() {
        return ConvertDialog.keeped;
    }

    private ArrayList<ConversionMode> modes;

    private JCheckBox chkKeepFormat;
    private JCheckBox chkForceKeep;
    private JCheckBox chkTopPriority;
    private JComboBox cmbModes;

    private ConvertDialog(ArrayList<ConversionMode> modes, String name) {
        super(UserIO.NO_COUNTDOWN, JDL.L("convert.dialog.chooseformat", "Select a format:") + " [" + name + "]", UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null);

        this.modes = modes;

        init();
    }

    public JComponent contentInit() {
        chkKeepFormat = new JCheckBox(ConvertDialog.hasKeeped() ? JDL.L("convert.dialog.staykeepingformat", "Try to get preferred format") : JDL.L("convert.dialog.keepformat", "Use this format for this session"));
        chkKeepFormat.setSelected(ConvertDialog.hasKeeped());

        chkForceKeep = new JCheckBox(JDL.L("convert.dialog.forcekeep", "Keep force"));
        chkForceKeep.setSelected(ConvertDialog.forcekeep);

        chkTopPriority = new JCheckBox(JDL.L("convert.dialog.toppriority", "This format has highest priority"));

        cmbModes = new JComboBox(modes.toArray(new ConversionMode[modes.size()]));
        cmbModes.setSelectedIndex(0);

        JPanel panel = new JPanel(new MigLayout("ins 0"));
        if (ConvertDialog.hasKeeped()) panel.add(chkTopPriority);
        panel.add(chkKeepFormat);
        panel.add(chkForceKeep, "wrap");
        panel.add(cmbModes, "spanx");
        return panel;
    }

    private ConversionMode getReturnMode() {
        if (!UserIO.isOK(getReturnValue())) return null;

        ConversionMode selectedValue = (ConversionMode) cmbModes.getSelectedItem();
        if ((chkKeepFormat.isSelected()) || (chkForceKeep.isSelected())) {
            ConvertDialog.setKeepformat(true);
            ConvertDialog.addKeeped(selectedValue, modes, chkTopPriority.isSelected());
            ConvertDialog.setForceKeep(chkForceKeep.isSelected());
        } else {
            ConvertDialog.setKeepformat(false);
        }
        return selectedValue;
    }

}
