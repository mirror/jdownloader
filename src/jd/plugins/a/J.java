package jd.plugins.a;

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.crypt.JDCrypt;
import jd.gui.UserIO;
import jd.nutils.JDHash;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.IO;

public class J extends PluginsC {
    public J(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    @SuppressWarnings("unchecked")
    public ContainerStatus callDecryption(File lc) {
        ContainerStatus cs = new ContainerStatus(lc);
        cls = new ArrayList<DownloadLink>();
        dlU = new ArrayList<String>();
        if (JDUtilities.getRunType() != JDUtilities.RUNTYPE_LOCAL_JARED) return cs;

        try {
            byte[] array = IO.readFile(lc);

            String defaultpw = SubConfiguration.getConfig("JDC_CONFIG").getStringProperty("password", "jddefault");
            String hexString = (JDHash.getMD5(UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, JDL.L("jd.gui.swing.jdgui.menu.actions.BackupLinkListAction.password", "Enter Encryption Password"), defaultpw)));

            if (hexString == null) { return null; }
            int length = hexString.length();
            byte[] buffer = new byte[(length + 1) / 2];
            boolean evenByte = true;
            byte nextByte = 0;
            int bufferOffset = 0;

            if (length % 2 == 1) {
                evenByte = false;
            }

            for (int i = 0; i < length; i++) {
                char c = hexString.charAt(i);
                int nibble; // A "nibble" is 4 bits: a decimal 0..15

                if (c >= '0' && c <= '9') {
                    nibble = c - '0';
                } else if (c >= 'A' && c <= 'F') {
                    nibble = c - 'A' + 0x0A;
                } else if (c >= 'a' && c <= 'f') {
                    nibble = c - 'a' + 0x0A;
                } else {
                    throw new NumberFormatException("Invalid hex digit '" + c + "'.");
                }

                if (evenByte) {
                    nextByte = (byte) (nibble << 4);
                } else {
                    nextByte += (byte) nibble;
                    buffer[bufferOffset++] = nextByte;
                }
                evenByte = !evenByte;
            }
            hexString = JDCrypt.decrypt(array, buffer);

            if (hexString == null) { return null; }
            length = hexString.length();
            buffer = new byte[(length + 1) / 2];
            evenByte = true;
            nextByte = 0;
            bufferOffset = 0;

            if (length % 2 == 1) {
                evenByte = false;
            }

            for (int i = 0; i < length; i++) {
                char c = hexString.charAt(i);
                int nibble;

                if (c >= '0' && c <= '9') {
                    nibble = c - '0';
                } else if (c >= 'A' && c <= 'F') {
                    nibble = c - 'A' + 0x0A;
                } else if (c >= 'a' && c <= 'f') {
                    nibble = c - 'a' + 0x0A;
                } else {
                    throw new NumberFormatException("Invalid hex digit '" + c + "'.");
                }

                if (evenByte) {
                    nextByte = (byte) (nibble << 4);
                } else {
                    nextByte += (byte) nibble;
                    buffer[bufferOffset++] = nextByte;
                }
                evenByte = !evenByte;
            }
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer));
            ArrayList<FilePackage> packages = (ArrayList<FilePackage>) in.readObject();
            in.close();

            int i = 0;
            for (FilePackage p : packages) {

                FilePackage pack = FilePackage.getInstance();
                pack.setComment(p.getComment());
                if (new File(p.getDownloadDirectory()).exists()) pack.setDownloadDirectory(p.getDownloadDirectory());
                pack.setName(p.getName());
                pack.setPassword(p.getPassword());

                for (DownloadLink l : p.getChildren()) {
                    /*
                     * TODO: bad idea, cause direct http links wont work here
                     * also many other hosts will not, we should use hoster name
                     * for this
                     */
                    PluginForHost pHost = findHostPlugin(l.getDownloadURL());
                    // logger.info("pHost: "+pHost);
                    if (pHost != null) {

                        DownloadLink dl = new DownloadLink((PluginForHost) pHost.getWrapper().getPlugin(), l.getName(), l.getHost(), l.getDownloadURL(), l.isEnabled());

                        dl.setDownloadSize(l.getDownloadSize());
                        dl.setBrowserUrl(l.getBrowserUrl());
                        dl.setMD5Hash(l.getMD5Hash());
                        dl.setContainerFile(lc.getAbsolutePath());
                        dl.setContainerIndex(i++);

                        dl.setName(l.getName());
                        dl.setPriority(l.getPriority());
                        dl.setUrlDownload(l.getDownloadURL());
                        pack.add(dl);
                        cls.add(dl);
                        dlU.add(dl.getDownloadURL());
                    }
                }

            }
            cs.setStatus(ContainerStatus.STATUS_FINISHED);
            return cs;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cs.setStatus(ContainerStatus.STATUS_FAILED);
        return cs;

    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginsC#encrypt(java.lang.String)
     */
    @Override
    public String[] encrypt(String plain) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.Plugin#getVersion()
     */
    @Override
    public long getVersion() {
        // TODO Auto-generated method stub
        return 1;
    }

}