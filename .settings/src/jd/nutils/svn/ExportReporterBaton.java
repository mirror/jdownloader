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

package jd.nutils.svn;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;

public class ExportReporterBaton implements ISVNReporterBaton {

    private long exportRevision;

    public ExportReporterBaton(long revision) {
        exportRevision = revision;
    }

    public void report(ISVNReporter reporter) throws SVNException {
        try {
            /*
             * Here empty working copy is reported.
             * 
             * ISVNReporter includes methods that allows to report mixed-rev
             * working copy and even let server know that some files or
             * directories are locally missing or locked.
             */
            reporter.setPath("", null, exportRevision, SVNDepth.INFINITY, true);

            /*
             * Don't forget to finish the report!
             */
            reporter.finishReport();
        } catch (SVNException svne) {
            reporter.abortReport();
            System.out.println("Report failed.");
        }
    }
}
