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

package jd.gui.skins.simple;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse zeigt dem Nutzer Auswahldialoge beim Konvertieren von FLVs an
 */
public class ConvertDialog extends JFrame {

    private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;

    public static enum ConversionMode {
        AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }), VIDEOFLV("Video (FLV)", new String[] { ".flv" }), AUDIOMP3_AND_VIDEOFLV("Audio & Video (MP3 & FLV)", new String[] { ".mp3", ".flv" }), VIDEOMP4("Video (MP4)", new String[] { ".mp4" }), VIDEO3GP("Video (3GP)", new String[] { ".3gp" })
        /** width=804, hight=640 bitrate=480 */
        , VIDEOPODCAST("Video (MP4-Podcast)", new String[] { ".mp4" })
        /** width=426, hight=320 bitrate=404 */
        , VIDEOIPHONE("Video (iPhone)", new String[] { ".mp4" });

        String text;
        String[] ext;

        ConversionMode(String text, String[] ext) {
            this.text = text;
            this.ext = ext;
        }

        @Override
        public String toString() {
            return text;
        }

        public String GetText() {
            return text;
        }

        public String[] GetExtArray() {
            return ext;
        }

        public String GetExt(int i) {
            return ext[i];
        }

        public String GetExtFirst() {
            return ext[0];
        }

    };

    private static boolean forcekeep = false;
    private static ArrayList<ConversionMode> keeped = new ArrayList<ConversionMode>();
    private static ArrayList<ConversionMode> keeped_availablemodes = new ArrayList<ConversionMode>();
    private static boolean keepformat = false;

    public static boolean isKeepformat() {
        return keepformat;
    }

    private static void setKeepformat(boolean newkeepformat) {

        if (newkeepformat == false) {
            ConvertDialog.keepformat = false;
            ConvertDialog.setForceKeep(false);
            ConvertDialog.keeped_availablemodes = new ArrayList<ConversionMode>();
            ;
            ConvertDialog.keeped = new ArrayList<ConversionMode>();
            ;
        } else {
            ConvertDialog.keepformat = true;
        }
    }

    private static void setForceKeep(boolean newforcekeep) {
        ConvertDialog.forcekeep = newforcekeep;
    }

    private static boolean hasKeeped() {
        if (keepformat) { return (!keeped.isEmpty()); }
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

    /**
     * Zeigt Auswahldialog an. wenn keepformat aktiviert ist wird gespeichtertes
     * Input zurückgegeben, es sei denn, andere Formate stehen zur Auswahl
     */
    public static ConversionMode DisplayDialog(ArrayList<ConversionMode> displaymodes, String name) {
        logger.fine(displaymodes.size() + " Convertmodi zur Auswahl.");

        if (displaymodes.size() == 1) // Bei einer einzigen Auswahl
        { return displaymodes.get(0); } // diese zurückgeben

        if (ConvertDialog.keepformat) {
            boolean newFormatChoosable = false;
            if (!ConvertDialog.forcekeep) {
                for (int i = 0; i < displaymodes.size(); i++) {
                    if (!keeped_availablemodes.contains(displaymodes.get(i))) // Neues
                                                                              // Element
                                                                              // in
                                                                              // der
                                                                              // Liste
                    {
                        newFormatChoosable = true;
                        break;
                    }
                }
            }
            if (!newFormatChoosable) // Wenn kein neues Format verfügbar ist
                                     // oder forcekeep = 1
            { // (newFormatChoosable kann bei forcekeep=true nicht auf true
              // gesetzt werden)

                for (int i = 0; i < keeped.size(); i++) {
                    if (displaymodes.contains(keeped.get(i))) { return keeped.get(i); }
                }
            }
        }

        // -.-.-.-.-.-.-.-.-.-.

        JPanel boxes = new JPanel();
        JCheckBox CheckBoxKeepFormat = new JCheckBox(JDLocale.L("convert.dialog.keepformat", "Format für diese Sitzung beibehalten"));
        JCheckBox CheckBoxForceKeep = new JCheckBox(JDLocale.L("convert.dialog.forcekeep", "Beibehalten erzwingen"));
        JCheckBox CheckBoxTopPriority = new JCheckBox(JDLocale.L("convert.dialog.toppriority", "Diese Auswahl vorherigen immer vorziehen"));

        CheckBoxForceKeep.setSelected(ConvertDialog.forcekeep);
        CheckBoxKeepFormat.setSelected(ConvertDialog.hasKeeped());

        if (ConvertDialog.hasKeeped()) {
            CheckBoxKeepFormat.setText(JDLocale.L("convert.dialog.staykeepingformat", "Formate weiterhin beibehalten"));
            boxes.add(CheckBoxTopPriority);
        }

        boxes.add(CheckBoxKeepFormat);
        boxes.add(CheckBoxForceKeep);

        ConversionMode selectedValue = (ConversionMode) JOptionPane.showInputDialog(null, boxes, /*
                                                                                                  * Enthält
                                                                                                  * die
                                                                                                  * Checkboxen
                                                                                                  */
        JDLocale.L("convert.dialog.chooseformat", "Wähle das Dateiformat:") + " [" + name + "]", JOptionPane.QUESTION_MESSAGE, null, displaymodes.toArray(), displaymodes.get(0));

        if ((CheckBoxKeepFormat.isSelected()) || (CheckBoxForceKeep.isSelected())) {
            ConvertDialog.setKeepformat(true);
            ConvertDialog.addKeeped(selectedValue, displaymodes, CheckBoxTopPriority.isSelected());
            ConvertDialog.setForceKeep(CheckBoxForceKeep.isSelected());
        } else {
            ConvertDialog.setKeepformat(false);
        }

        return selectedValue;
    }

    /*
     * public static void main(String [ ] args) { ArrayList<ConversionMode> opts
     * = new ArrayList<ConversionMode>(); opts.add(ConversionMode.AUDIOMP3);
     * opts.add(ConversionMode.VIDEOMP4);
     * System.out.println("1: "+ConvertDialog.DisplayDialog(opts, "GUI-Test"));
     * System.out.println("2: "+ConvertDialog.DisplayDialog(opts, "GUI-Test2"));
     * opts.remove(ConversionMode.VIDEOMP4);
     * System.out.println("3: "+ConvertDialog.DisplayDialog(opts, "GUI-Test3"));
     * opts.add(ConversionMode.VIDEOIPHONE);
     * System.out.println("4: "+ConvertDialog.DisplayDialog(opts,
     * "GUI-Test 4")); opts.add(ConversionMode.VIDEOMP4);
     * System.out.println("5: "+ConvertDialog.DisplayDialog(opts,
     * "GUI-Test 5")); opts.remove(ConversionMode.VIDEOIPHONE);
     * System.out.println("6: "+ConvertDialog.DisplayDialog(opts,
     * "GUI-Test 6")); }
     */

}
