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

package jd.utils;

import java.io.File;

import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.io.JDIO;
import jd.nutils.svn.Subversion;

import org.appwork.utils.Application;

public class UpdateDevWorkspace {

    private static void updateSVN(final String svnUrl, final String path) throws Exception {
        final Subversion svn = new Subversion(svnUrl);

        final File dir = Application.getRessource(path);
        if (!dir.exists()) dir.mkdirs();
        try {
            svn.cleanUp(dir, true);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        svn.getBroadcaster().addListener(new MessageListener() {

            public void onMessage(MessageEvent event) {
                System.out.println(event.getMessage());
            }

        });

        try {
            svn.update(dir, null);
            svn.revert(dir);
        } catch (final Exception e) {
            e.printStackTrace();
            JDIO.removeDirectoryOrFile(dir);
            svn.update(dir, null);
        }
    }

    public static void main(String[] args) {
        /*this sets correct homedirectory*/
        JDUtilities.getJDHomeDirectoryFromEnvironment();
        System.out.println("Update resources at " +  Application.getRessource(""));

        try {
            updateSVN("svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/libs/", "libs");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        try {
            updateSVN("svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/", "jd");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        try {
            updateSVN("svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/tools/", "tools");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
