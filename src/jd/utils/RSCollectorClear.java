package jd.utils;

import java.io.IOException;
import java.util.Date;

import jd.http.Browser;

public class RSCollectorClear {

    /**
     * ACHTUNG!!! Dieses script löscht alle files auf einem collectors account die länger als 7 tage nicht geladen wurden!
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Browser br = new Browser();
        String id = "";
        String pass = "";
        br.getPage("http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=listfiles_v1&type=col&login=" + id + "&password=" + pass + "&realfolder=0&fields=filename,killcode,downloads,lastdownload");

        String[][] matches = br.getRegex("(\\d+),(.*?),(\\d+),(\\d+),(\\d+)").getMatches();
        String del = "";
        for (String[] match : matches) {

            String fileID = match[0];
            String fileName = match[1];
//            String killCode = match[2];
            int downloads = Integer.parseInt(match[3]);
            long lastdlTime = Integer.parseInt(match[4]) * 1000l;
            if ((System.currentTimeMillis() - lastdlTime) > 1000 * 60 * 60 * 24 * 7) {
                System.out.println("Delete: " + fileName + " - " + downloads + " - " + new Date(lastdlTime));
                del += "," + fileID;
            }

        }
        br.getPage("http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=deletefiles_v1&type=col&login=" + id + "&password=" + pass + "&files=" + del.substring(1));

 

    }

}
