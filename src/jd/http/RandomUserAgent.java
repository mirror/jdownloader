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

package jd.http;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import jd.nutils.Formatter;

public class RandomUserAgent {

    /**
     * Initial browser release date. Used for invalid randomDate() calls.
     */
    private static final String[] releaseDate = { "2005", "11", "11" };

    /**
     * Browser-Language
     */
    private static final ArrayList<String> langs = new ArrayList<String>();

    /**
     * Firefox branch revision, version and release date triplet. They MUST
     * match! As of Firefox version > 3.0 buildHour is appended. Use ".xx"
     */
    private static final ArrayList<String> ffVersionInfos = new ArrayList<String>();

    /**
     * Windows: .NET addon-string, to make the user-agent more random
     */
    private static final ArrayList<String> winAddons = new ArrayList<String>();

    /**
     * Linux: distribution addon-string, to make the user-agent more random
     */
    private static final ArrayList<String> linuxAddons = new ArrayList<String>();

    /**
     * Macintosh: version addon-string, to make the user-agent more random
     */
    private static final ArrayList<String> macAddons = new ArrayList<String>();

    /**
     * Information about the system: platform, OS, architecture
     */
    private static final ArrayList<System> system = new ArrayList<System>();

    /**
     * Fill in values in all constants
     */
    private static final void initData() {
        langs.add("en");
        langs.add("en-US");
        langs.add("en-GB");

        ffVersionInfos.add("1.8|1.5|2005.11.11");
        ffVersionInfos.add("1.8.1|2.0|2006.10.10");
        ffVersionInfos.add("1.9|3.0|2008.05.29");
        ffVersionInfos.add("1.9.0.5|3.0.5|2008.12.01.xx");
        ffVersionInfos.add("1.9.0.6|3.0.6|2009.01.19.xx");
        ffVersionInfos.add("1.9.0.7|3.0.7|2009.02.19.xx");
        ffVersionInfos.add("1.9.0.8|3.0.8|2009.03.26.xx");
        ffVersionInfos.add("1.9.0.9|3.0.9|2009.04.08.xx");
        ffVersionInfos.add("1.9.0.10|3.0.10|2009.04.23.xx");

        winAddons.add("");
        winAddons.add("GTB5");
        winAddons.add("(.NET CLR 1.1.4322)");
        winAddons.add("(.NET CLR 2.0.50727)");
        winAddons.add("(.NET CLR 3.0.4506.2152)");
        winAddons.add("(.NET CLR 3.5.30729)");

        linuxAddons.add(" ");
        linuxAddons.add("Ubuntu/8.04 (hardy)");
        linuxAddons.add("Ubuntu/8.10 (intrepid)");
        // linuxAddons.add("Ubuntu/9.04 (jaunty)");
        linuxAddons.add("Fedora/3.0.6-1.fc10");
        linuxAddons.add("Fedora/3.0.10-1.fc10");
        linuxAddons.add("(Gentoo)");
        linuxAddons.add("SUSE/3.0-1.2");
        linuxAddons.add("SUSE/3.0-1.1");
        linuxAddons.add("Red Hat/3.0.5-1.el5_2");

        macAddons.add("");
        macAddons.add("Mach-O");
        macAddons.add("10.4");
        macAddons.add("10.5");
        macAddons.add("10.6");

        system.clear();
        system.add(new System("Windows", "Windows", "NT 5.0|NT 5.1|NT 5.2|NT 6.0|NT 6.1", false, true));
        system.add(new System("X11", "Linux", "x86|x86_64|i586|i686", false, true));
        system.add(new System("Macintosh", "Mac OS X", "Intel|PPC|68K", true, true));
        system.add(new System("X11", "FreeBSD", "i386|amd64|sparc64|alpha", false, true));
        system.add(new System("X11", "OpenBSD", "i386|amd64|sparc64|alpha", false, true));
        system.add(new System("X11", "NetBSD", "i386|amd64|sparc64|alpha", false, true));
        system.add(new System("X11", "SunOS", "i86pc|sun4u", false, true));
    }

    /**
     * The main user-agent string generator
     * 
     * @return Random Firefox user-agent string
     */
    public static String generate() {
        initData();
        return generateFF();
    }

    /**
     * The Firefox user-agent string generator
     * 
     * @return Random Firefox user-agent string
     */
    public static String generateFF() {
        Random rand = new Random();

        String platform;
        String osAndArch;
        String winAddon = "";
        String linuxAddon = " ";
        String macAddon = "";

        /* Get system infos */
        int i = rand.nextInt(system.size());
        do {
            platform = system.get(i).platform;
            String osName = system.get(i).osName;
            String[] archs = system.get(i).archs.split("\\|");
            String arch = archs[rand.nextInt(archs.length)];
            boolean inverseOrder = system.get(i).inverseOrder;
            if (!inverseOrder)
                osAndArch = osName + " " + arch;
            else
                osAndArch = arch + " " + osName;
        } while (system.get(i).useIt == false);

        /* Get optional strings */
        if (system.get(i).osName.equalsIgnoreCase("Windows")) {
            winAddon = winAddons.get(rand.nextInt(winAddons.size()));
            if (winAddon != "") winAddon = " " + winAddon.trim();
        } else if (system.get(i).osName.equalsIgnoreCase("Linux")) {
            linuxAddon = linuxAddons.get(rand.nextInt(linuxAddons.size()));
            if (linuxAddon != " ") linuxAddon = " " + linuxAddon.trim() + " ";
        } else if (system.get(i).osName.equalsIgnoreCase("Mac OS X")) {
            macAddon = macAddons.get(rand.nextInt(macAddons.size()));
            if (macAddon != "") macAddon = " " + macAddon.trim();
        }

        /* Get Browser language */
        String lang = langs.get(rand.nextInt(langs.size()));

        /* Get Firefox branch revision and version */
        String[] tmpFFVersionInfos = ffVersionInfos.get(rand.nextInt(ffVersionInfos.size())).split("\\|");
        String ffRev = tmpFFVersionInfos[0];
        String ffVersion = tmpFFVersionInfos[1];
        String[] ffReleaseDate = tmpFFVersionInfos[2].split("\\.");

        return "Mozilla/5.0 (" + platform + "; U; " + osAndArch + macAddon + "; " + lang + "; rv:" + ffRev + ") Gecko/" + randomDate(ffReleaseDate) + linuxAddon + "Firefox/" + ffVersion + winAddon;
    }

    /**
     * Generates a random date in form of "YYYYMMDD[HH]" between releaseDate and
     * the current date
     * 
     * @return random date
     */
    private static String randomDate(String[] releaseDate) {
        String returnDate = RandomUserAgent.releaseDate[0] + RandomUserAgent.releaseDate[1] + RandomUserAgent.releaseDate[2];
        if (releaseDate == null || releaseDate.length < 3 || releaseDate.length > 4) return returnDate;

        Calendar rCal = new GregorianCalendar(Integer.parseInt(releaseDate[0]), Integer.parseInt(releaseDate[1]) - 1, Integer.parseInt(releaseDate[2]));
        long rTime = rCal.getTimeInMillis();
        long cTime = new GregorianCalendar().getTimeInMillis();

        Random rand = new Random();
        long randTime = rand.nextInt((int) ((cTime - rTime) / (60 * 1000))) + ((int) (rTime / (60 * 1000)));
        rCal.setTimeInMillis(randTime * (60 * 1000));

        int year = rCal.get(Calendar.YEAR);
        String month = Formatter.fillString((rCal.get(Calendar.MONTH) + 1) + "", "0", "", 2);
        String day = Formatter.fillString(rCal.get(Calendar.DAY_OF_MONTH) + "", "0", "", 2);
        String hour = "";
        if (releaseDate.length == 4) hour = Formatter.fillString(rand.nextInt(24) + "", "0", "", 2);
        returnDate = "" + year + month + day + hour;
        return returnDate;
    }

    private static class System {
        public String platform;
        public String osName;
        public String archs;
        public boolean inverseOrder;
        public boolean useIt;

        public System(String platform, String osName, String archs, boolean inverseOrder, boolean useIt) {
            this.platform = platform;
            this.osName = osName;
            this.archs = archs;
            this.inverseOrder = inverseOrder;
            this.useIt = useIt;
        }
    }

}
