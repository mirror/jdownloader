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

import java.util.HashMap;
import java.util.Map.Entry;

import jd.controlling.JDLogger;
import jd.parser.Regex;

/**
 * Language and COUNRTYCode database
 * 
 * @author thomas
 * 
 */
public class JDGeoCode {
    /**
     * Returns the native language name for the givven code
     * 
     * @param code
     * @return
     */
    public static String getNativeLanguage(final String code) {
        final String[] ret = LANGUAGES.get(code);
        return ret == null ? null : ret[1];
    }

    /**
     * pasres a lng code like de-AT and returns an array languagecode,
     * countrycode,extension
     * 
     * 
     * de-AT -->[de,AT,null] de-2 -->[
     * 
     * @param lngCode
     * @return
     */
    public static String[] parseLanguageCode(final String lngCode) {
        String[] split = lngCode.split("\\-");
        if (split.length == 1) {
            final String languagecode = lngCode.toLowerCase().trim();
            return (languagecode.length() != 2) ? null : new String[] { lngCode, null, null };
        } else if (split.length == 2) {
            final boolean h = COUNTRIES.containsKey(split[1].toUpperCase());
            if (split[0].trim().length() != 2) {
                return null;
            }
            return new String[] { split[0].toLowerCase(), h ? split[1].toUpperCase() : null, h ? null : split[1] };
        } else {
            split = lngCode.split("\\-");
            final String split0 = split[0];
            final String split1 = split[1];

            if (split0.trim().length() != 2) return null;
            if (split1.trim().length() != 2) return null;
            return new String[] { split0.toLowerCase(), split1.toUpperCase(), split[2] };
        }
    }

    /**
     * Language codes according to ISO 639-1. Language Names and native names
     */
    public final static HashMap<String, String[]> LANGUAGES = new HashMap<String, String[]>();
    static {
        LANGUAGES.put("aa", new String[] { "Afar", "Afaraf" });
        LANGUAGES.put("ab", new String[] { "Abkhazian", "Аҧсуа" });
        LANGUAGES.put("ae", new String[] { "Avestan", "avesta" });
        LANGUAGES.put("af", new String[] { "Afrikaans", "Afrikaans" });
        LANGUAGES.put("ak", new String[] { "Akan", "Akan" });
        LANGUAGES.put("am", new String[] { "Amharic", "አማርኛ" });
        LANGUAGES.put("an", new String[] { "Aragonese", "Aragonés" });
        LANGUAGES.put("ar", new String[] { "Arabic", "العربية" });
        LANGUAGES.put("av", new String[] { "Avaric", "авар мацӀ; магӀарул мацӀ" });
        LANGUAGES.put("ay", new String[] { "Aymara", "aymar aru" });
        LANGUAGES.put("az", new String[] { "Azerbaijani", "azərbaycan dili" });
        LANGUAGES.put("ba", new String[] { "Bashkir", "башҡорт теле" });
        LANGUAGES.put("be", new String[] { "Belarusian", "Беларуская" });
        LANGUAGES.put("bg", new String[] { "Bulgarian", "български език" });
        LANGUAGES.put("bh", new String[] { "Bihari", "भोजपुरी" });
        LANGUAGES.put("bm", new String[] { "Bambara", "bamanankan" });
        LANGUAGES.put("bn", new String[] { "Bengali", "বাংলা" });
        LANGUAGES.put("bo", new String[] { "Tibetan", "བོད་ཡིག" });
        LANGUAGES.put("br", new String[] { "Breton", "brezhoneg" });
        LANGUAGES.put("bs", new String[] { "Bosnian", "bosanski jezik" });
        LANGUAGES.put("ca", new String[] { "Catalan", "Català" });
        LANGUAGES.put("ch", new String[] { "Chamorro", "Chamoru" });
        LANGUAGES.put("co", new String[] { "Corsican", "corsu; lingua corsa" });
        LANGUAGES.put("cr", new String[] { "Cree", "ᓀᐦᐃᔭᐍᐏᐣ" });
        LANGUAGES.put("cs", new String[] { "Czech", "česky; čeština" });
        LANGUAGES.put("cu", new String[] { "Church Slavic", "ѩзыкъ словѣньскъ" });
        LANGUAGES.put("cv", new String[] { "Chuvash", "чӑваш чӗлхи" });
        LANGUAGES.put("cy", new String[] { "Welsh", "Cymraeg" });
        LANGUAGES.put("da", new String[] { "Danish", "dansk" });
        LANGUAGES.put("de", new String[] { "German", "Deutsch" });
        LANGUAGES.put("dv", new String[] { "Divehi", "ދިވެހި" });
        LANGUAGES.put("dz", new String[] { "Dzongkha", "རྫོང་ཁ" });
        LANGUAGES.put("ee", new String[] { "Ewe", "Ɛʋɛgbɛ" });
        LANGUAGES.put("el", new String[] { "Greek", "Ελληνικά" });
        LANGUAGES.put("en", new String[] { "English", "English" });
        LANGUAGES.put("eo", new String[] { "Esperanto", "Esperanto" });
        LANGUAGES.put("es", new String[] { "Spanish", "Español" });
        LANGUAGES.put("et", new String[] { "Estonian", "eesti; eesti keel" });
        LANGUAGES.put("eu", new String[] { "Basque", "euskara; euskera" });
        LANGUAGES.put("fa", new String[] { "Persian", "فارسی" });
        LANGUAGES.put("ff", new String[] { "Fulah", "Fulfulde" });
        LANGUAGES.put("fi", new String[] { "Finnish", "suomi; suomen kieli" });
        LANGUAGES.put("fj", new String[] { "Fijian", "vosa Vakaviti" });
        LANGUAGES.put("fo", new String[] { "Faroese", "Føroyskt" });
        LANGUAGES.put("fr", new String[] { "French", "Français; langue française" });
        LANGUAGES.put("fy", new String[] { "Western Frisian", "Frysk" });
        LANGUAGES.put("ga", new String[] { "Irish", "Gaeilge" });
        LANGUAGES.put("gd", new String[] { "Scottish Gaelic", "Gàidhlig" });
        LANGUAGES.put("gl", new String[] { "Galician", "Galego" });
        LANGUAGES.put("gn", new String[] { "Guaraní", "Avañe'ẽ" });
        LANGUAGES.put("gu", new String[] { "Gujarati", "ગુજરાતી" });
        LANGUAGES.put("gv", new String[] { "Manx", "Gaelg; Gailck" });
        LANGUAGES.put("ha", new String[] { "Hausa", "هَوُسَ" });
        LANGUAGES.put("he", new String[] { "Hebrew", "עברית" });
        LANGUAGES.put("hi", new String[] { "Hindi", "हिन्दी; हिंदी" });
        LANGUAGES.put("ho", new String[] { "Hiri Motu", "Hiri Motu" });
        LANGUAGES.put("hr", new String[] { "Croatian", "Hrvatski" });
        LANGUAGES.put("ht", new String[] { "Haitian", "Kreyòl ayisyen" });
        LANGUAGES.put("hu", new String[] { "Hungarian", "Magyar" });
        LANGUAGES.put("hy", new String[] { "Armenian", "Հայերեն" });
        LANGUAGES.put("hz", new String[] { "Herero", "Otjiherero" });
        LANGUAGES.put("ia", new String[] { "Interlingua (International Auxiliary Language Association)", "Interlingua" });
        LANGUAGES.put("id", new String[] { "Indonesian", "Bahasa Indonesia" });
        LANGUAGES.put("ie", new String[] { "Interlingue", "Interlingue" });
        LANGUAGES.put("ig", new String[] { "Igbo", "Igbo" });
        LANGUAGES.put("ii", new String[] { "Sichuan Yi", "ꆇꉙ" });
        LANGUAGES.put("ik", new String[] { "Inupiaq", "Iñupiaq; Iñupiatun" });
        LANGUAGES.put("io", new String[] { "Ido", "Ido" });
        LANGUAGES.put("is", new String[] { "Icelandic", "Íslenska" });
        LANGUAGES.put("it", new String[] { "Italian", "Italiano" });
        LANGUAGES.put("iu", new String[] { "Inuktitut", "ᐃᓄᒃᑎᑐᑦ" });
        LANGUAGES.put("ja", new String[] { "Japanese", "Nihongo" });
        LANGUAGES.put("ka", new String[] { "Georgian", "ქართული" });
        LANGUAGES.put("kg", new String[] { "Kongo", "KiKongo" });
        LANGUAGES.put("ki", new String[] { "Kikuyu", "Gĩkũyũ" });
        LANGUAGES.put("kj", new String[] { "Kwanyama", "Kuanyama" });
        LANGUAGES.put("kk", new String[] { "Kazakh", "Қазақ тілі" });
        LANGUAGES.put("kl", new String[] { "Kalaallisut", "kalaallisut; kalaallit oqaasii" });
        LANGUAGES.put("km", new String[] { "Khmer", "ភាសាខ្មែរ" });
        LANGUAGES.put("kn", new String[] { "Kannada", "ಕನ್ನಡ" });
        LANGUAGES.put("ko", new String[] { "Korean", "Kanuri" });
        LANGUAGES.put("ks", new String[] { "Kashmiri", "коми кыв" });
        LANGUAGES.put("kw", new String[] { "Cornish", "Kernewek" });
        LANGUAGES.put("ky", new String[] { "Kirghiz", "кыргыз тили" });
        LANGUAGES.put("la", new String[] { "Latin", "latine; lingua latina" });
        LANGUAGES.put("lb", new String[] { "Luxembourgish", "Lëtzebuergesch" });
        LANGUAGES.put("lg", new String[] { "Ganda", "Luganda" });
        LANGUAGES.put("li", new String[] { "Limburgish", "Limburgs" });
        LANGUAGES.put("ln", new String[] { "Lingala", "Lingála" });
        LANGUAGES.put("lo", new String[] { "Lao", "ພາສາລາວ" });
        LANGUAGES.put("lt", new String[] { "Lithuanian", "lietuvių kalba" });
        LANGUAGES.put("lu", new String[] { "Luba-Katanga", "latviešu valoda" });
        LANGUAGES.put("mg", new String[] { "Malagasy", "Malagasy fiteny" });
        LANGUAGES.put("mh", new String[] { "Marshallese", "Kajin M̧ajeļ" });
        LANGUAGES.put("mi", new String[] { "Māori", "te reo Māori" });
        LANGUAGES.put("mk", new String[] { "Macedonian", "македонски јазик" });
        LANGUAGES.put("ml", new String[] { "Malayalam", "മലയാളം" });
        LANGUAGES.put("mn", new String[] { "Mongolian", "Монгол" });
        LANGUAGES.put("mr", new String[] { "Marathi", "मराठी" });
        LANGUAGES.put("ms", new String[] { "Malay", "Malti" });
        LANGUAGES.put("my", new String[] { "Burmese", "ဗမာစာ" });
        LANGUAGES.put("na", new String[] { "Nauru", "Ekakairũ Naoero" });
        LANGUAGES.put("nb", new String[] { "Norwegian Bokmål", "Norsk bokmål" });
        LANGUAGES.put("nd", new String[] { "North Ndebele", "isiNdebele" });
        LANGUAGES.put("ne", new String[] { "Nepali", "नेपाली" });
        LANGUAGES.put("ng", new String[] { "Ndonga", "Owambo" });
        LANGUAGES.put("nl", new String[] { "Dutch", "Nederlands" });
        LANGUAGES.put("nn", new String[] { "Norwegian Nynorsk", "Norsk nynorsk" });
        LANGUAGES.put("no", new String[] { "Norwegian", "Norsk" });
        LANGUAGES.put("nr", new String[] { "South Ndebele", "isiNdebele" });
        LANGUAGES.put("nv", new String[] { "Navajo", "Diné bizaad; Dinékʼehǰí" });
        LANGUAGES.put("ny", new String[] { "Chichewa", "chiCheŵa; chinyanja" });
        LANGUAGES.put("oc", new String[] { "Occitan", "Occitan" });
        LANGUAGES.put("oj", new String[] { "Ojibwa", "ᐊᓂᔑᓈᐯᒧᐎᓐ" });
        LANGUAGES.put("om", new String[] { "Oromo", "Afaan Oromoo" });
        LANGUAGES.put("or", new String[] { "Oriya", "ଓଡ଼ିଆ" });
        LANGUAGES.put("os", new String[] { "Ossetian", "Ирон æвзаг" });
        LANGUAGES.put("pa", new String[] { "Panjabi", "पाऴि" });
        LANGUAGES.put("pl", new String[] { "Polish", "polski" });
        LANGUAGES.put("ps", new String[] { "Pashto", "پښتو" });
        LANGUAGES.put("pt", new String[] { "Portuguese", "Português" });
        LANGUAGES.put("qu", new String[] { "Quechua", "Runa Simi; Kichwa" });
        LANGUAGES.put("rm", new String[] { "Raeto-Romance", "rumantsch grischun" });
        LANGUAGES.put("rn", new String[] { "Kirundi", "kiRundi" });
        LANGUAGES.put("ro", new String[] { "Romanian", "Română" });
        LANGUAGES.put("ru", new String[] { "Russian", "русский язык" });
        LANGUAGES.put("rw", new String[] { "Kinyarwanda", "Ikinyarwanda" });
        LANGUAGES.put("sa", new String[] { "Sanskrit", "संस्कृतम्" });
        LANGUAGES.put("sc", new String[] { "Sardinian", "sardu" });
        LANGUAGES.put("sd", new String[] { "Sindhi", "Davvisámegiella" });
        LANGUAGES.put("sg", new String[] { "Sango", "yângâ tî sängö" });
        LANGUAGES.put("si", new String[] { "Sinhala", "සිංහල" });
        LANGUAGES.put("sk", new String[] { "Slovak", "slovenčina" });
        LANGUAGES.put("sl", new String[] { "Slovenian", "slovenščina" });
        LANGUAGES.put("sm", new String[] { "Samoan", "gagana fa'a Samoa" });
        LANGUAGES.put("sn", new String[] { "Shona", "chiShona" });
        LANGUAGES.put("so", new String[] { "Somali", "Soomaaliga; af Soomaali" });
        LANGUAGES.put("sq", new String[] { "Albanian", "Shqip" });
        LANGUAGES.put("sr", new String[] { "Serbian", "српски језик" });
        LANGUAGES.put("ss", new String[] { "Swati", "SiSwati" });
        LANGUAGES.put("st", new String[] { "Southern Sotho", "Sesotho" });
        LANGUAGES.put("su", new String[] { "Sundanese", "Basa Sunda" });
        LANGUAGES.put("sv", new String[] { "Swedish", "svenska" });
        LANGUAGES.put("sw", new String[] { "Swahili", "Kiswahili" });
        LANGUAGES.put("ta", new String[] { "Tamil", "தமிழ்" });
        LANGUAGES.put("te", new String[] { "Telugu", "తెలుగు" });
        LANGUAGES.put("tg", new String[] { "Tajik", "ไทย" });
        LANGUAGES.put("ti", new String[] { "Tigrinya", "ትግርኛ" });
        LANGUAGES.put("tk", new String[] { "Turkmen", "Türkmen; Түркмен" });
        LANGUAGES.put("tl", new String[] { "Tagalog", "Tagalog" });
        LANGUAGES.put("tn", new String[] { "Tswana", "Setswana" });
        LANGUAGES.put("to", new String[] { "Tonga", "faka Tonga" });
        LANGUAGES.put("tr", new String[] { "Turkish", "Türkçe" });
        LANGUAGES.put("ts", new String[] { "Tsonga", "Xitsonga" });
        LANGUAGES.put("tt", new String[] { "Tatar", "Twi" });
        LANGUAGES.put("ty", new String[] { "Tahitian", "Reo Mā`ohi" });
        LANGUAGES.put("ug", new String[] { "Uighur", "Українська" });
        LANGUAGES.put("ur", new String[] { "Urdu", "اردو" });
        LANGUAGES.put("uk", new String[] { "Ukrainian", "Ukraïna" });
        LANGUAGES.put("uz", new String[] { "Uzbek", "Tshivenḓa" });
        LANGUAGES.put("vi", new String[] { "Vietnamese", "Tiếng Việt" });
        LANGUAGES.put("vo", new String[] { "Volapük", "Volapük" });
        LANGUAGES.put("wa", new String[] { "Walloon", "Walon" });
        LANGUAGES.put("wo", new String[] { "Wolof", "Wollof" });
        LANGUAGES.put("xh", new String[] { "Xhosa", "isiXhosa" });
        LANGUAGES.put("yi", new String[] { "Yiddish", "ייִדיש" });
        LANGUAGES.put("yo", new String[] { "Yoruba", "Yorùbá" });
        LANGUAGES.put("za", new String[] { "Zhuang", "Saɯ cueŋƅ; Saw cuengh" });
        LANGUAGES.put("zh", new String[] { "Chinese", "中文 (Zhōngwén), 汉语, 漢語" });
        LANGUAGES.put("zu", new String[] { "Zulu", "isiZulu" });
    }

    /**
     * COuntrycodes according to ISO 3166 (Topleveldomains)
     */
    public final static HashMap<String, String> COUNTRIES = new HashMap<String, String>();
    static {

        COUNTRIES.put("AD", "Andorra");
        COUNTRIES.put("AE", "United Arab Emirates");
        COUNTRIES.put("AF", "Afghanistan");
        COUNTRIES.put("AG", "Antigua And Barbuda");

        COUNTRIES.put("AI", "Anguilla");
        COUNTRIES.put("AL", "Albania");
        COUNTRIES.put("AM", "Armenia");
        COUNTRIES.put("AN", "Netherlands Antilles");
        COUNTRIES.put("AO", "Angola");
        COUNTRIES.put("AQ", "Antarctica");

        COUNTRIES.put("AR", "Argentina");
        COUNTRIES.put("AS", "American Samoa");
        COUNTRIES.put("AT", "Austria");
        COUNTRIES.put("AU", "Australia");
        COUNTRIES.put("AW", "Aruba");
        COUNTRIES.put("AZ", "Azerbaijan");

        COUNTRIES.put("BA", "Bosnia And Herzegowina");
        COUNTRIES.put("BB", "Barbados");
        COUNTRIES.put("BD", "Bangladesh");
        COUNTRIES.put("BE", "Belgium");
        COUNTRIES.put("BF", "Burkina Faso");
        COUNTRIES.put("BG", "Bulgaria");

        COUNTRIES.put("BH", "Bahrain");
        COUNTRIES.put("BI", "Burundi");
        COUNTRIES.put("BJ", "Benin");
        COUNTRIES.put("BM", "Bermuda");
        COUNTRIES.put("BN", "Brunei Darussalam");
        COUNTRIES.put("BO", "Bolivia");

        COUNTRIES.put("BR", "Brazil");
        COUNTRIES.put("BS", "Bahamas");
        COUNTRIES.put("BT", "Bhutan");
        COUNTRIES.put("BV", "Bouvet Island");
        COUNTRIES.put("BW", "Botswana");
        COUNTRIES.put("BY", "Belarus");

        COUNTRIES.put("BZ", "Belize");

        COUNTRIES.put("CA", "Canada");
        COUNTRIES.put("CC", "Cocos (Keeling) Islands");

        COUNTRIES.put("CF", "Central African Republic");
        COUNTRIES.put("CG", "Congo");
        COUNTRIES.put("CH", "Switzerland");
        COUNTRIES.put("CI", "Cote d'Ivoire");
        COUNTRIES.put("CK", "Cook Islands");
        COUNTRIES.put("CL", "Chile");

        COUNTRIES.put("CM", "Cameroon");
        COUNTRIES.put("CN", "China");
        COUNTRIES.put("CO", "Colombia");
        COUNTRIES.put("CR", "Costa Rica");
        COUNTRIES.put("CU", "Cuba");
        COUNTRIES.put("CV", "Cape Verde");

        COUNTRIES.put("CX", "Christmas Island");
        COUNTRIES.put("CY", "Cyprus");
        COUNTRIES.put("CZ", "Czech Republic");
        COUNTRIES.put("DE", "Germany");
        COUNTRIES.put("DJ", "Djibouti");
        COUNTRIES.put("DK", "Denmark");

        COUNTRIES.put("DM", "Dominica");
        COUNTRIES.put("DO", "Dominican Republic");
        COUNTRIES.put("DZ", "Algeria");
        COUNTRIES.put("EC", "Ecuador");
        COUNTRIES.put("EE", "Estonia");
        COUNTRIES.put("EG", "Egypt");

        COUNTRIES.put("EH", "Western Sahara");
        COUNTRIES.put("ER", "Eritrea");
        COUNTRIES.put("ES", "Spain");
        COUNTRIES.put("ET", "Ethiopia");
        COUNTRIES.put("EU", "Europe (SSGFI only)");
        COUNTRIES.put("FI", "Finland");

        COUNTRIES.put("FJ", "Fiji");
        COUNTRIES.put("FK", "Falkland Islands (Malvinas)");
        COUNTRIES.put("FM", "Micronesia, Federated States Of");
        COUNTRIES.put("FO", "Faroe Islands");
        COUNTRIES.put("FR", "France");
        COUNTRIES.put("FX", "France, Metropolitan");

        COUNTRIES.put("GA", "Gabon");
        COUNTRIES.put("GB", "United Kingdom (UK)");
        COUNTRIES.put("GD", "Grenada");
        COUNTRIES.put("GE", "Georgia");

        COUNTRIES.put("GF", "French Guiana");
        COUNTRIES.put("GH", "Ghana");
        COUNTRIES.put("GI", "Gibraltar");
        COUNTRIES.put("GL", "Greenland");
        COUNTRIES.put("GM", "Gambia");
        COUNTRIES.put("GN", "Guinea");

        COUNTRIES.put("GP", "Guadeloupe");
        COUNTRIES.put("GQ", "Equatorial Guinea");
        COUNTRIES.put("GR", "Greece");
        COUNTRIES.put("GS", "South Georgia And The South Sandwich Islands");
        COUNTRIES.put("GT", "Guatemala");
        COUNTRIES.put("GU", "Guam");

        COUNTRIES.put("GW", "Guinea-Bissau");
        COUNTRIES.put("GY", "Guyana");
        COUNTRIES.put("HK", "Hong Kong");
        COUNTRIES.put("HM", "Heard And Mc Donald Islands");
        COUNTRIES.put("HN", "Honduras");
        COUNTRIES.put("HR", "Croatia (local name: Hrvatska)");

        COUNTRIES.put("HT", "Haiti");
        COUNTRIES.put("HU", "Hungary");
        COUNTRIES.put("ID", "Indonesia");
        COUNTRIES.put("IE", "Ireland");
        COUNTRIES.put("II", "International (SSGFI only)");
        COUNTRIES.put("IL", "Israel");

        COUNTRIES.put("IN", "India");
        COUNTRIES.put("IO", "British Indian Ocean Territory");
        COUNTRIES.put("IQ", "Iraq");
        COUNTRIES.put("IR", "Iran (Islamic Republic Of)");
        COUNTRIES.put("IS", "Iceland");
        COUNTRIES.put("IT", "Italy");

        COUNTRIES.put("JM", "Jamaica");
        COUNTRIES.put("JO", "Jordan");
        COUNTRIES.put("JP", "Japan");
        COUNTRIES.put("KE", "Kenya");
        COUNTRIES.put("KG", "Kyrgyzstan");
        COUNTRIES.put("KH", "Cambodia");
        COUNTRIES.put("KI", "Kiribati");
        COUNTRIES.put("KM", "Comoros");

        COUNTRIES.put("KN", "Saint Kitts And Nevis");
        COUNTRIES.put("KP", "Korea, Democratic People's Republic Of");
        COUNTRIES.put("KR", "Korea, Republic Of");
        COUNTRIES.put("KW", "Kuwait");
        COUNTRIES.put("KY", "Cayman Islands");
        COUNTRIES.put("KZ", "Kazakhstan");

        COUNTRIES.put("LA", "Lao People's Democratic Republic");
        COUNTRIES.put("LB", "Lebanon");
        COUNTRIES.put("LC", "Saint Lucia");
        COUNTRIES.put("LI", "Liechtenstein");
        COUNTRIES.put("LK", "Sri Lanka");
        COUNTRIES.put("LR", "Liberia");

        COUNTRIES.put("LS", "Lesotho");
        COUNTRIES.put("LT", "Lithuania");
        COUNTRIES.put("LU", "Luxembourg");
        COUNTRIES.put("LV", "Latvia");
        COUNTRIES.put("LY", "Libyan Arab Jamahiriya");
        COUNTRIES.put("MA", "Morocco");

        COUNTRIES.put("MC", "Monaco");
        COUNTRIES.put("MD", "Moldova, Republic Of");
        COUNTRIES.put("MG", "Madagascar");
        COUNTRIES.put("MH", "Marshall Islands");
        COUNTRIES.put("MK", "Macedonia, The Former Yugoslav Republic Of");
        COUNTRIES.put("ML", "Mali");

        COUNTRIES.put("MM", "Myanmar");
        COUNTRIES.put("MN", "Mongolia");
        COUNTRIES.put("MO", "Macau");
        COUNTRIES.put("MP", "Northern Mariana Islands");
        COUNTRIES.put("MQ", "Martinique");
        COUNTRIES.put("MR", "Mauritania");

        COUNTRIES.put("MS", "Montserrat");
        COUNTRIES.put("MT", "Malta");
        COUNTRIES.put("MU", "Mauritius");
        COUNTRIES.put("MV", "Maldives");
        COUNTRIES.put("MW", "Malawi");
        COUNTRIES.put("MX", "Mexico");

        COUNTRIES.put("MY", "Malaysia");
        COUNTRIES.put("MZ", "Mozambique");

        COUNTRIES.put("NA", "Namibia");
        COUNTRIES.put("NC", "New Caledonia");

        COUNTRIES.put("NE", "Niger");
        COUNTRIES.put("NF", "Norfolk Island");
        COUNTRIES.put("NG", "Nigeria");
        COUNTRIES.put("NI", "Nicaragua");
        COUNTRIES.put("NL", "Netherlands");
        COUNTRIES.put("NO", "Norway");

        COUNTRIES.put("NP", "Nepal");
        COUNTRIES.put("NR", "Nauru");
        COUNTRIES.put("NU", "Niue");
        COUNTRIES.put("NZ", "New Zealand");
        COUNTRIES.put("OM", "Oman");

        COUNTRIES.put("PE", "Peru");
        COUNTRIES.put("PF", "French Polynesia");
        COUNTRIES.put("PG", "Papua New Guinea");
        COUNTRIES.put("PH", "Philippines");
        COUNTRIES.put("PK", "Pakistan");
        COUNTRIES.put("PL", "Poland");

        COUNTRIES.put("PM", "St. Pierre And Miquelon");
        COUNTRIES.put("PN", "Pitcairn");
        COUNTRIES.put("PR", "Puerto Rico");
        COUNTRIES.put("PT", "Portugal");
        COUNTRIES.put("PW", "Palau");
        COUNTRIES.put("PY", "Paraguay");

        COUNTRIES.put("QA", "Qatar");
        COUNTRIES.put("RE", "Reunion");
        COUNTRIES.put("RO", "Romania");
        COUNTRIES.put("RU", "Russian Federation");
        COUNTRIES.put("RW", "Rwanda");
        COUNTRIES.put("SA", "Saudi Arabia");

        COUNTRIES.put("SB", "Solomon Islands");
        COUNTRIES.put("SC", "Seychelles");
        COUNTRIES.put("SD", "Sudan");
        COUNTRIES.put("SE", "Sweden");
        COUNTRIES.put("SG", "Singapore");
        COUNTRIES.put("SH", "St. Helena");

        COUNTRIES.put("SI", "Slovenia");
        COUNTRIES.put("SJ", "Svalbard And Jan Mayen Islands");
        COUNTRIES.put("SK", "Slovakia (Slovak Republic)");
        COUNTRIES.put("SL", "Sierra Leone");
        COUNTRIES.put("SM", "San Marino");
        COUNTRIES.put("SN", "Senegal");

        COUNTRIES.put("SO", "Somalia");
        COUNTRIES.put("SR", "Suriname");
        COUNTRIES.put("ST", "Sao Tome And Principe");
        COUNTRIES.put("SV", "El Salvador");
        COUNTRIES.put("SY", "Syrian Arab Republic");
        COUNTRIES.put("SZ", "Swaziland");
        COUNTRIES.put("TC", "Turks And Caicos Islands");
        COUNTRIES.put("TD", "Chad");
        COUNTRIES.put("TF", "French Southern Territories");
        COUNTRIES.put("TG", "Togo");

        COUNTRIES.put("TH", "Thailand");
        COUNTRIES.put("TJ", "Tajikistan");
        COUNTRIES.put("TK", "Tokelau");
        COUNTRIES.put("TM", "Turkmenistan");
        COUNTRIES.put("TN", "Tunisia");
        COUNTRIES.put("TO", "Tonga");

        COUNTRIES.put("TP", "East Timor");
        COUNTRIES.put("TR", "Turkey");
        COUNTRIES.put("TT", "Trinidad And Tobago");
        COUNTRIES.put("TV", "Tuvalu");
        COUNTRIES.put("TW", "Taiwan, Province Of China");
        COUNTRIES.put("TZ", "Tanzania, United Republic Of");

        COUNTRIES.put("UA", "Ukraine");
        COUNTRIES.put("UG", "Uganda");
        COUNTRIES.put("UM", "United States Minor Outlying Islands");
        COUNTRIES.put("US", "United States");
        COUNTRIES.put("UY", "Uruguay");
        COUNTRIES.put("UZ", "Uzbekistan");

        COUNTRIES.put("VA", "Vatican City State (Holy See)");
        COUNTRIES.put("VC", "Saint Vincent And The Grenadines");
        COUNTRIES.put("VE", "Venezuela");
        COUNTRIES.put("VG", "Virgin Islands (British)");
        COUNTRIES.put("VI", "Virgin Islands (U.S.)");
        COUNTRIES.put("VN", "Viet Nam");

        COUNTRIES.put("VU", "Vanuatu");
        COUNTRIES.put("WF", "Wallis And Futuna Islands");
        COUNTRIES.put("WS", "Samoa");
        COUNTRIES.put("YE", "Yemen");
        COUNTRIES.put("YT", "Mayotte");
        COUNTRIES.put("YU", "Yugoslavia");

        COUNTRIES.put("ZA", "South Africa");
        COUNTRIES.put("ZM", "Zambia");
        COUNTRIES.put("ZR", "Zaire");
        COUNTRIES.put("ZW", "Zimbabwe");
    }

    public static final HashMap<String, String> EXTENSIONS = new HashMap<String, String>();
    static {
        EXTENSIONS.put("hans", "simplified");
        EXTENSIONS.put("hant", "traditional");
    }

    /**
     * Transforms a lng-country-ext code into its longer form
     * 
     * @param string
     * @return
     */
    public static String toLonger(final String string) {
        final String[] p = JDGeoCode.parseLanguageCode(string);
        if (p == null) { return null; }

        final String language = LANGUAGES.get(p[0])[0];
        final String country = COUNTRIES.get(p[1]);
        String extension = EXTENSIONS.get(p[2]);
        if (extension == null) {
            extension = p[2];
        }
        StringBuilder ret = new StringBuilder(language);
        if (country != null) {
            ret.append(" [" + country);
            if (extension != null) {
                ret.append(" | " + extension);
            }
            ret.append("]");
        } else if (extension != null) {
            ret.append(" [" + extension + "]");
        }
        return ret.toString();
    }

    public static String toLongerNative(final String string) {
        final String[] p = JDGeoCode.parseLanguageCode(string);
        try {
            final String language = LANGUAGES.get(p[0])[1];
            final String country = COUNTRIES.get(p[1]);
            String extension = EXTENSIONS.get(p[2]);
            if (extension == null) {
                extension = p[2];
            }

            StringBuilder ret = new StringBuilder(language);
            if (country != null) {
                ret.append(" [" + country);
                if (extension != null) {
                    ret.append(" | " + extension);
                }
                ret.append("]");
            } else if (extension != null) {
                ret.append(" [" + extension + "]");
            }

            return ret.toString();
        } catch (Exception e) {
            JDLogger.getLogger().severe("Bad languagecode: " + string);
            return toLongerNative("en");
        }
    }

    public static String longToShort(final String lng) {
        String[] row = new Regex(lng, "(.*?)\\[(.*)\\|(.*?)\\]").getRow(0);
        if (row != null) { return getLanguageCode(row[0].trim()) + "-" + getCountryCode(row[1].trim()) + "-" + getExtensionCode(row[2].trim()); }
        row = new Regex(lng, "(.*?)\\[(.*)\\]").getRow(0);
        if (row != null) {
            final String countryCode = getCountryCode(row[1].trim());

            return getLanguageCode(row[0].trim()) + "-" + (countryCode != null ? countryCode : getExtensionCode(row[1].trim()));
        }

        return getLanguageCode(lng.trim());
    }

    /**
     * Returns the languagecode for a native or english languagename
     * 
     * @param name
     * @return
     */
    public static String getLanguageCode(final String name) {
        for (final Entry<String, String[]> next : LANGUAGES.entrySet()) {
            if (next.getValue()[0].equalsIgnoreCase(name) || next.getValue()[1].equalsIgnoreCase(name)) { return next.getKey(); }
        }
        return null;
    }

    /**
     * Returns the extension code for the extensionname
     * 
     * @param name
     * @return
     */
    public static String getExtensionCode(final String name) {
        for (final Entry<String, String> next : EXTENSIONS.entrySet()) {
            if (next.getValue().equalsIgnoreCase(name)) { return next.getKey(); }
        }
        return name;
    }

    /**
     * Returns the countrycode for a givven countryname
     * 
     * @param name
     * @return
     */
    public static String getCountryCode(final String name) {
        for (final Entry<String, String> next : COUNTRIES.entrySet()) {
            if (next.getValue().equalsIgnoreCase(name)) { return next.getKey(); }
        }
        return null;
    }

}
