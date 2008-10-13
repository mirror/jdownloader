package jd.plugins.optional;
import java.util.ArrayList;

import jd.parser.Regex;
    public class PremInfo {
        public String username;
        public String password;
        public String hostname;
        public String id;

        public PremInfo(String[] strings) {
            try {
                id = strings[0];
                username = strings[1];
                password = strings[2];
                hostname = strings[3];
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        public static PremInfo[] getPremInfo(String xmlString)
        {
          
                String[][] m = new Regex(xmlString,
                        "<premacc id=\"(.*?)\">.*?<login>(.*?)</login>.*?<password>(.*?)</password>.*?<hoster>(.*?)</hoster>.*?</premacc>")
                .getMatches();
                ArrayList<PremInfo> premAccs = new ArrayList<PremInfo>();
                for (int i = 0; i < m.length; i++) {
                    premAccs.add(new PremInfo(m[i]));
                }
                return premAccs.toArray(new PremInfo[premAccs.size()]);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PremInfo) {
            PremInfo info = (PremInfo) obj;
                if (info.username.equalsIgnoreCase(username)
                        && info.password.equalsIgnoreCase(password)
                        && info.hostname.equalsIgnoreCase(hostname))
                    return true;
            }
            return false;
        }
    }