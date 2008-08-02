package jd.utils;

import java.io.File;
import java.util.regex.Pattern;

import jd.parser.Regex;

public final class Sniffy {
    public static Pattern[] blackList = new Pattern[] {

    Pattern.compile("(ethersnoop|tshark|ethereal|evilmonkey|wireshark|pcap|sniff|clearsight|httpwatch|caplon|tcpdump|netcor|ettercap|etherpeek|omnipeek|gigapeek|landecoder|network monitor|netspector|netvcr|networkactiv|optiview|tracecommander|websensor|webprobe)", Pattern.CASE_INSENSITIVE)

    };
    public static Pattern[] whiteList = new Pattern[] { Pattern.compile("Sniffy", Pattern.CASE_INSENSITIVE) };

    public static boolean hasSniffer() {
        if (OSDetector.isWindows()) { return hasWinSnifer();

        }
        return false;
    }

    public static boolean hasWinSnifer() {
        try {
            File reader = JDUtilities.getResourceFile("tools/windows/p.exe");

            String hash = JDUtilities.getLocalHash(reader);
            if (!hash.equals("3c2298676457b5c49e55dbee3451c4b1")) {
                System.out.println("p Manipulated");
                return true;
            }

            Executer exec = new Executer(reader.toString());

            exec.setWaitTimeout(10);
            exec.start();
            exec.waitTimeout();
            String list = exec.getStream() + " \r\n " + exec.getErrorStream();
            for (Pattern white : whiteList) {

                list = white.matcher(list).replaceAll("");

            }
            for (Pattern black : blackList) {
                Regex r;
                if ((r=new Regex(list, black)).matches()) {
                    String[][] m = r.getMatches();
                    return true; }
            }

      
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
