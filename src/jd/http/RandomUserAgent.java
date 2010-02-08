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

public final class RandomUserAgent {

    /**
     * Don't let anyone instantiate this class.
     */
    private RandomUserAgent() {
    }

    /**
     * Initial browser release date. Used for invalid randomDate() calls.
     */
    private static final String[] releaseDate = { "2005", "11", "11" };

    /**
     * Browser-Language
     */
    private static ArrayList<String> langs = new ArrayList<String>();

    /**
     * Firefox branch revision, version and release date triplet. They MUST
     * match! Release date in form of "YYYY.MM.DD[.HH]". As of Firefox version >
     * 3.0 buildHour is appended. Use ".xx"
     */
    private static ArrayList<String> ffVersionInfos = new ArrayList<String>();

    /**
     * Internet Explorer versions
     */
    private static ArrayList<String> ieVersions = new ArrayList<String>();

    /**
     * Windows Versions. E.g. NT 5.1 is Windows XP
     */
    private static String winVersions = "";

    /**
     * Windows: dotNet addon-strings, to make the user-agent more random
     */
    private static ArrayList<String[]> winAddons = new ArrayList<String[]>();

    /**
     * Linux: distribution addon-strings, to make the user-agent more random
     */
    private static ArrayList<String> linuxAddons = new ArrayList<String>();

    /**
     * Macintosh: version addon-strings, to make the user-agent more random
     */
    private static ArrayList<String> macAddons = new ArrayList<String>();

    /**
     * Information about the system: platform, OS, architecture
     */
    private static ArrayList<System> system = new ArrayList<System>();

    /**
     * Fill in the values (the "CONFIGURATION")
     */
    private static final void initData() {

        /* Used only in generateIE */
        ieVersions.add("6.0");
        ieVersions.add("7.0");
        ieVersions.add("8.0");

        /* Used in both generateIE and generateFF */
        winVersions = "NT 5.0|NT 5.1|NT 5.2|NT 6.0|NT 6.1";

        winAddons.add(new String[] { "", ".NET CLR 1.0.3705", ".NET CLR 1.1.4322" });
        winAddons.add(new String[] { "", ".NET CLR 2.0.40607", ".NET CLR 2.0.50727" });
        winAddons.add(new String[] { "", ".NET CLR 3.0.04506.648", ".NET CLR 3.0.4506.2152" });
        winAddons.add(new String[] { "", ".NET CLR 3.5.21022", ".NET CLR 3.5.30729" });

        /* Used only in generateFF */
        langs.add("en");
        langs.add("en-US");
        langs.add("en-GB");

        ffVersionInfos.add("1.8|1.5|2005.11.11");
        ffVersionInfos.add("1.8.1|2.0|2006.10.10");
        ffVersionInfos.add("1.9|3.0|2008.05.29.xx");
        ffVersionInfos.add("1.9.0.5|3.0.5|2008.12.01.xx");
        ffVersionInfos.add("1.9.0.6|3.0.6|2009.01.19.xx");
        ffVersionInfos.add("1.9.0.7|3.0.7|2009.02.19.xx");
        ffVersionInfos.add("1.9.0.8|3.0.8|2009.03.26.xx");
        ffVersionInfos.add("1.9.0.9|3.0.9|2009.04.08.xx");
        ffVersionInfos.add("1.9.0.10|3.0.10|2009.04.23.xx");

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

        system.add(new System("Windows", "Windows", winVersions, false, true));
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
     * @return Random Firefox or Internet Explorer user-agent string
     */
    public static String generate() {
        if ((new Random().nextInt() % 2) == 0)
            return generateFF();
        else
            return generateIE();
    }

    /**
     * The Firefox user-agent string generator
     * 
     * @return Random Firefox user-agent string
     */
    public static String generateFF() {
        initData();
        final Random rand = new Random();

        String platform = "";
        String osAndArch = "";
        String winAddon = "";
        String linuxAddon = " ";
        String macAddon = "";

        /* Get system infos */
        final int i = rand.nextInt(system.size());
        do {
            platform = system.get(i).platform;
            final String osName = system.get(i).osName;
            final String[] archs = system.get(i).archs.split("\\|");
            final String arch = archs[rand.nextInt(archs.length)];
            if (!system.get(i).inverseOrder)
                osAndArch = osName + " " + arch;
            else
                osAndArch = arch + " " + osName;
        } while (system.get(i).useIt == false);

        /* Get optional strings */
        if (system.get(i).osName.equalsIgnoreCase("Windows")) {
            winAddon = dotNetString();
            if (winAddon.trim().length() > 0) {
                winAddon = (" (" + winAddon.trim() + ")").replace("(; ", "(");
            }
        } else if (system.get(i).osName.equalsIgnoreCase("Linux")) {
            linuxAddon = linuxAddons.get(rand.nextInt(linuxAddons.size()));
            if (linuxAddon != " ") {
                linuxAddon = " " + linuxAddon.trim() + " ";
            }
        } else if (system.get(i).osName.equalsIgnoreCase("Mac OS X")) {
            macAddon = macAddons.get(rand.nextInt(macAddons.size()));
            if (macAddon != "") {
                macAddon = " " + macAddon.trim();
            }
        }

        /* Get Browser language */
        final String lang = langs.get(rand.nextInt(langs.size()));

        /* Get Firefox branch revision, version and release date */
        final String[] tmpFFVersionInfos = ffVersionInfos.get(rand.nextInt(ffVersionInfos.size())).split("\\|");
        final String ffRev = tmpFFVersionInfos[0];
        final String ffVersion = tmpFFVersionInfos[1];
        final String[] ffReleaseDate = tmpFFVersionInfos[2].split("\\.");

        return "Mozilla/5.0 (" + platform + "; U; " + osAndArch + macAddon + "; " + lang + "; rv:" + ffRev + ") Gecko/" + randomDate(ffReleaseDate) + linuxAddon + "Firefox/" + ffVersion + winAddon;
    }

    /**
     * The Internet Explorer user-agent string generator
     * 
     * @return Random Internet Explorer user-agent string
     */
    public static String generateIE() {
        initData();
        final Random rand = new Random();

        final String ieVersion = ieVersions.get(rand.nextInt(ieVersions.size()));
        final String winVersion = winVersions.split("\\|")[rand.nextInt(winVersions.split("\\|").length)];
        String trident = "";
        if (ieVersion.equalsIgnoreCase("8.0")) {
            trident = "; Trident/4.0";
        }

        return "Mozilla/4.0 (compatible; MSIE " + ieVersion + "; Windows " + winVersion + trident + dotNetString() + ")";
    }

    private static String dotNetString() {
        final Random rand = new Random();

        String dotNet10 = "; " + winAddons.get(0)[rand.nextInt(winAddons.get(0).length)];
        if (dotNet10.equalsIgnoreCase("; ")) {
            dotNet10 = "";
        }

        String dotNet20 = "; " + winAddons.get(1)[rand.nextInt(winAddons.get(1).length)];
        if (dotNet20.equalsIgnoreCase("; ")) {
            dotNet20 = "";
        }

        String dotNet30 = "";
        if (dotNet20.length() != 0) {
            dotNet30 = "; " + winAddons.get(2)[rand.nextInt(winAddons.get(2).length)];
        }
        if (dotNet30.equalsIgnoreCase("; ")) {
            dotNet30 = "";
        }

        String dotNet35 = "";
        if (dotNet30.length() != 0) {
            dotNet35 = "; " + winAddons.get(3)[rand.nextInt(winAddons.get(3).length)];
        }
        if (dotNet35.equalsIgnoreCase("; ")) {
            dotNet35 = "";
        }

        return dotNet10 + dotNet20 + dotNet30 + dotNet35;
    }

    /**
     * Generates a random date in form of "YYYYMMDD[HH]" between releaseDate and
     * the current date
     * 
     * @return random date
     */
    private static String randomDate(final String[] releaseDate) {
        String returnDate = RandomUserAgent.releaseDate[0] + RandomUserAgent.releaseDate[1] + RandomUserAgent.releaseDate[2];
        if (releaseDate == null || releaseDate.length < 3 || releaseDate.length > 4) return returnDate;

        final Calendar rCal = new GregorianCalendar(Integer.parseInt(releaseDate[0]), Integer.parseInt(releaseDate[1]) - 1, Integer.parseInt(releaseDate[2]));
        final long rTime = rCal.getTimeInMillis();
        final long cTime = new GregorianCalendar().getTimeInMillis();

        final Random rand = new Random();
        int temp = (int) ((cTime - rTime) / (60 * 1000)) + (int) (rTime / (60 * 1000));
        if (temp < 0) {
            temp = -temp;
        }
        final long randTime = rand.nextInt(temp);
        rCal.setTimeInMillis(randTime * (60 * 1000));

        final int year = rCal.get(Calendar.YEAR);
        final String month = Formatter.fillString((rCal.get(Calendar.MONTH) + 1) + "", "0", "", 2);
        final String day = Formatter.fillString(rCal.get(Calendar.DAY_OF_MONTH) + "", "0", "", 2);
        String hour = "";
        if (releaseDate.length == 4) {
            hour = Formatter.fillString(rand.nextInt(24) + "", "0", "", 2);
        }
        returnDate = "" + year + month + day + hour;
        return returnDate;
    }

    private static class System {
        public String platform;
        public String osName;
        public String archs;
        public boolean inverseOrder;
        public boolean useIt;

        public System(final String platform, final String osName, final String archs, final boolean inverseOrder, final boolean useIt) {
            this.platform = platform;
            this.osName = osName;
            this.archs = archs;
            this.inverseOrder = inverseOrder;
            this.useIt = useIt;
        }
    }

}
