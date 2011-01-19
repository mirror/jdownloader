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

import org.appwork.utils.AwReg;

/**
 * Language and COUNRTYCode database
 * 
 * @author thomas
 * 
 */
public class JDGeoCode {
    /**
     * Language codes according to ISO 639-1. Language Names and native names
     */
    public final static HashMap<String, String[]> LANGUAGES  = new HashMap<String, String[]>();

    static {
        JDGeoCode.LANGUAGES.put("aa", new String[] { "Afar", "Afaraf" });
        JDGeoCode.LANGUAGES.put("ab", new String[] { "Abkhazian", "Аҧсуа" });
        JDGeoCode.LANGUAGES.put("ae", new String[] { "Avestan", "avesta" });
        JDGeoCode.LANGUAGES.put("af", new String[] { "Afrikaans", "Afrikaans" });
        JDGeoCode.LANGUAGES.put("ak", new String[] { "Akan", "Akan" });
        JDGeoCode.LANGUAGES.put("am", new String[] { "Amharic", "አማርኛ" });
        JDGeoCode.LANGUAGES.put("an", new String[] { "Aragonese", "Aragonés" });
        JDGeoCode.LANGUAGES.put("ar", new String[] { "Arabic", "العربية" });
        JDGeoCode.LANGUAGES.put("av", new String[] { "Avaric", "авар мацӀ; магӀарул мацӀ" });
        JDGeoCode.LANGUAGES.put("ay", new String[] { "Aymara", "aymar aru" });
        JDGeoCode.LANGUAGES.put("az", new String[] { "Azerbaijani", "azərbaycan dili" });
        JDGeoCode.LANGUAGES.put("ba", new String[] { "Bashkir", "башҡорт теле" });
        JDGeoCode.LANGUAGES.put("be", new String[] { "Belarusian", "Беларуская" });
        JDGeoCode.LANGUAGES.put("bg", new String[] { "Bulgarian", "български език" });
        JDGeoCode.LANGUAGES.put("bh", new String[] { "Bihari", "भोजपुरी" });
        JDGeoCode.LANGUAGES.put("bm", new String[] { "Bambara", "bamanankan" });
        JDGeoCode.LANGUAGES.put("bn", new String[] { "Bengali", "বাংলা" });
        JDGeoCode.LANGUAGES.put("bo", new String[] { "Tibetan", "བོད་ཡིག" });
        JDGeoCode.LANGUAGES.put("br", new String[] { "Breton", "brezhoneg" });
        JDGeoCode.LANGUAGES.put("bs", new String[] { "Bosnian", "bosanski jezik" });
        JDGeoCode.LANGUAGES.put("ca", new String[] { "Catalan", "Català" });
        JDGeoCode.LANGUAGES.put("ch", new String[] { "Chamorro", "Chamoru" });
        JDGeoCode.LANGUAGES.put("co", new String[] { "Corsican", "corsu; lingua corsa" });
        JDGeoCode.LANGUAGES.put("cr", new String[] { "Cree", "ᓀᐦᐃᔭᐍᐏᐣ" });
        JDGeoCode.LANGUAGES.put("cs", new String[] { "Czech", "česky; čeština" });
        JDGeoCode.LANGUAGES.put("cu", new String[] { "Church Slavic", "ѩзыкъ словѣньскъ" });
        JDGeoCode.LANGUAGES.put("cv", new String[] { "Chuvash", "чӑваш чӗлхи" });
        JDGeoCode.LANGUAGES.put("cy", new String[] { "Welsh", "Cymraeg" });
        JDGeoCode.LANGUAGES.put("da", new String[] { "Danish", "dansk" });
        JDGeoCode.LANGUAGES.put("de", new String[] { "German", "Deutsch" });
        JDGeoCode.LANGUAGES.put("dv", new String[] { "Divehi", "ދިވެހި" });
        JDGeoCode.LANGUAGES.put("dz", new String[] { "Dzongkha", "རྫོང་ཁ" });
        JDGeoCode.LANGUAGES.put("ee", new String[] { "Ewe", "Ɛʋɛgbɛ" });
        JDGeoCode.LANGUAGES.put("el", new String[] { "Greek", "Ελληνικά" });
        JDGeoCode.LANGUAGES.put("en", new String[] { "English", "English" });
        JDGeoCode.LANGUAGES.put("eo", new String[] { "Esperanto", "Esperanto" });
        JDGeoCode.LANGUAGES.put("es", new String[] { "Spanish", "Español" });
        JDGeoCode.LANGUAGES.put("et", new String[] { "Estonian", "eesti; eesti keel" });
        JDGeoCode.LANGUAGES.put("eu", new String[] { "Basque", "euskara; euskera" });
        JDGeoCode.LANGUAGES.put("fa", new String[] { "Persian", "فارسی" });
        JDGeoCode.LANGUAGES.put("ff", new String[] { "Fulah", "Fulfulde" });
        JDGeoCode.LANGUAGES.put("fi", new String[] { "Finnish", "suomi; suomen kieli" });
        JDGeoCode.LANGUAGES.put("fj", new String[] { "Fijian", "vosa Vakaviti" });
        JDGeoCode.LANGUAGES.put("fo", new String[] { "Faroese", "Føroyskt" });
        JDGeoCode.LANGUAGES.put("fr", new String[] { "French", "Français; langue française" });
        JDGeoCode.LANGUAGES.put("fy", new String[] { "Western Frisian", "Frysk" });
        JDGeoCode.LANGUAGES.put("ga", new String[] { "Irish", "Gaeilge" });
        JDGeoCode.LANGUAGES.put("gd", new String[] { "Scottish Gaelic", "Gàidhlig" });
        JDGeoCode.LANGUAGES.put("gl", new String[] { "Galician", "Galego" });
        JDGeoCode.LANGUAGES.put("gn", new String[] { "Guaraní", "Avañe'ẽ" });
        JDGeoCode.LANGUAGES.put("gu", new String[] { "Gujarati", "ગુજરાતી" });
        JDGeoCode.LANGUAGES.put("gv", new String[] { "Manx", "Gaelg; Gailck" });
        JDGeoCode.LANGUAGES.put("ha", new String[] { "Hausa", "هَوُسَ" });
        JDGeoCode.LANGUAGES.put("he", new String[] { "Hebrew", "עברית" });
        JDGeoCode.LANGUAGES.put("hi", new String[] { "Hindi", "हिन्दी; हिंदी" });
        JDGeoCode.LANGUAGES.put("ho", new String[] { "Hiri Motu", "Hiri Motu" });
        JDGeoCode.LANGUAGES.put("hr", new String[] { "Croatian", "Hrvatski" });
        JDGeoCode.LANGUAGES.put("ht", new String[] { "Haitian", "Kreyòl ayisyen" });
        JDGeoCode.LANGUAGES.put("hu", new String[] { "Hungarian", "Magyar" });
        JDGeoCode.LANGUAGES.put("hy", new String[] { "Armenian", "Հայերեն" });
        JDGeoCode.LANGUAGES.put("hz", new String[] { "Herero", "Otjiherero" });
        JDGeoCode.LANGUAGES.put("ia", new String[] { "Interlingua (International Auxiliary Language Association)", "Interlingua" });
        JDGeoCode.LANGUAGES.put("id", new String[] { "Indonesian", "Bahasa Indonesia" });
        JDGeoCode.LANGUAGES.put("ie", new String[] { "Interlingue", "Interlingue" });
        JDGeoCode.LANGUAGES.put("ig", new String[] { "Igbo", "Igbo" });
        JDGeoCode.LANGUAGES.put("ii", new String[] { "Sichuan Yi", "ꆇꉙ" });
        JDGeoCode.LANGUAGES.put("ik", new String[] { "Inupiaq", "Iñupiaq; Iñupiatun" });
        JDGeoCode.LANGUAGES.put("io", new String[] { "Ido", "Ido" });
        JDGeoCode.LANGUAGES.put("is", new String[] { "Icelandic", "Íslenska" });
        JDGeoCode.LANGUAGES.put("it", new String[] { "Italian", "Italiano" });
        JDGeoCode.LANGUAGES.put("iu", new String[] { "Inuktitut", "ᐃᓄᒃᑎᑐᑦ" });
        JDGeoCode.LANGUAGES.put("ja", new String[] { "Japanese", "Nihongo" });
        JDGeoCode.LANGUAGES.put("ka", new String[] { "Georgian", "ქართული" });
        JDGeoCode.LANGUAGES.put("kg", new String[] { "Kongo", "KiKongo" });
        JDGeoCode.LANGUAGES.put("ki", new String[] { "Kikuyu", "Gĩkũyũ" });
        JDGeoCode.LANGUAGES.put("kj", new String[] { "Kwanyama", "Kuanyama" });
        JDGeoCode.LANGUAGES.put("ku", new String[] { "Kurdish", "كوردی" });
        JDGeoCode.LANGUAGES.put("kk", new String[] { "Kazakh", "Қазақ тілі" });
        JDGeoCode.LANGUAGES.put("kl", new String[] { "Kalaallisut", "kalaallisut; kalaallit oqaasii" });
        JDGeoCode.LANGUAGES.put("km", new String[] { "Khmer", "ភាសាខ្មែរ" });
        JDGeoCode.LANGUAGES.put("kn", new String[] { "Kannada", "ಕನ್ನಡ" });
        JDGeoCode.LANGUAGES.put("ko", new String[] { "Korean", "Kanuri" });
        JDGeoCode.LANGUAGES.put("ks", new String[] { "Kashmiri", "коми кыв" });
        JDGeoCode.LANGUAGES.put("kw", new String[] { "Cornish", "Kernewek" });
        JDGeoCode.LANGUAGES.put("ky", new String[] { "Kirghiz", "кыргыз тили" });
        JDGeoCode.LANGUAGES.put("la", new String[] { "Latin", "latine; lingua latina" });
        JDGeoCode.LANGUAGES.put("lb", new String[] { "Luxembourgish", "Lëtzebuergesch" });
        JDGeoCode.LANGUAGES.put("lg", new String[] { "Ganda", "Luganda" });
        JDGeoCode.LANGUAGES.put("li", new String[] { "Limburgish", "Limburgs" });
        JDGeoCode.LANGUAGES.put("ln", new String[] { "Lingala", "Lingála" });
        JDGeoCode.LANGUAGES.put("lo", new String[] { "Lao", "ພາສາລາວ" });
        JDGeoCode.LANGUAGES.put("lt", new String[] { "Lithuanian", "lietuvių kalba" });
        JDGeoCode.LANGUAGES.put("lu", new String[] { "Luba-Katanga", "latviešu valoda" });
        JDGeoCode.LANGUAGES.put("mg", new String[] { "Malagasy", "Malagasy fiteny" });
        JDGeoCode.LANGUAGES.put("mh", new String[] { "Marshallese", "Kajin M̧ajeļ" });
        JDGeoCode.LANGUAGES.put("mi", new String[] { "Māori", "te reo Māori" });
        JDGeoCode.LANGUAGES.put("mk", new String[] { "Macedonian", "македонски јазик" });
        JDGeoCode.LANGUAGES.put("ml", new String[] { "Malayalam", "മലയാളം" });
        JDGeoCode.LANGUAGES.put("mn", new String[] { "Mongolian", "Монгол" });
        JDGeoCode.LANGUAGES.put("mr", new String[] { "Marathi", "मराठी" });
        JDGeoCode.LANGUAGES.put("mt", new String[] { "Maltese", "Malti" });
        JDGeoCode.LANGUAGES.put("ms", new String[] { "Malay", "بهاس ملايو" });
        JDGeoCode.LANGUAGES.put("my", new String[] { "Burmese", "ဗမာစာ" });
        JDGeoCode.LANGUAGES.put("na", new String[] { "Nauru", "Ekakairũ Naoero" });
        JDGeoCode.LANGUAGES.put("nb", new String[] { "Norwegian Bokmål", "Norsk bokmål" });
        JDGeoCode.LANGUAGES.put("nd", new String[] { "North Ndebele", "isiNdebele" });
        JDGeoCode.LANGUAGES.put("ne", new String[] { "Nepali", "नेपाली" });
        JDGeoCode.LANGUAGES.put("ng", new String[] { "Ndonga", "Owambo" });
        JDGeoCode.LANGUAGES.put("nl", new String[] { "Dutch", "Nederlands" });
        JDGeoCode.LANGUAGES.put("nn", new String[] { "Norwegian Nynorsk", "Norsk nynorsk" });
        JDGeoCode.LANGUAGES.put("no", new String[] { "Norwegian", "Norsk" });
        JDGeoCode.LANGUAGES.put("nr", new String[] { "South Ndebele", "isiNdebele" });
        JDGeoCode.LANGUAGES.put("nv", new String[] { "Navajo", "Diné bizaad; Dinékʼehǰí" });
        JDGeoCode.LANGUAGES.put("ny", new String[] { "Chichewa", "chiCheŵa; chinyanja" });
        JDGeoCode.LANGUAGES.put("oc", new String[] { "Occitan", "Occitan" });
        JDGeoCode.LANGUAGES.put("oj", new String[] { "Ojibwa", "ᐊᓂᔑᓈᐯᒧᐎᓐ" });
        JDGeoCode.LANGUAGES.put("om", new String[] { "Oromo", "Afaan Oromoo" });
        JDGeoCode.LANGUAGES.put("or", new String[] { "Oriya", "ଓଡ଼ିଆ" });
        JDGeoCode.LANGUAGES.put("os", new String[] { "Ossetian", "Ирон æвзаг" });
        JDGeoCode.LANGUAGES.put("pa", new String[] { "Panjabi", "पाऴि" });
        JDGeoCode.LANGUAGES.put("pl", new String[] { "Polish", "polski" });
        JDGeoCode.LANGUAGES.put("ps", new String[] { "Pashto", "پښتو" });
        JDGeoCode.LANGUAGES.put("pt", new String[] { "Portuguese", "Português" });
        JDGeoCode.LANGUAGES.put("qu", new String[] { "Quechua", "Runa Simi; Kichwa" });
        JDGeoCode.LANGUAGES.put("rm", new String[] { "Raeto-Romance", "rumantsch grischun" });
        JDGeoCode.LANGUAGES.put("rn", new String[] { "Kirundi", "kiRundi" });
        JDGeoCode.LANGUAGES.put("ro", new String[] { "Romanian", "Română" });
        JDGeoCode.LANGUAGES.put("ru", new String[] { "Russian", "русский язык" });
        JDGeoCode.LANGUAGES.put("rw", new String[] { "Kinyarwanda", "Ikinyarwanda" });
        JDGeoCode.LANGUAGES.put("sa", new String[] { "Sanskrit", "संस्कृतम्" });
        JDGeoCode.LANGUAGES.put("sc", new String[] { "Sardinian", "sardu" });
        JDGeoCode.LANGUAGES.put("sd", new String[] { "Sindhi", "Davvisámegiella" });
        JDGeoCode.LANGUAGES.put("sg", new String[] { "Sango", "yângâ tî sängö" });
        JDGeoCode.LANGUAGES.put("si", new String[] { "Sinhala", "සිංහල" });
        JDGeoCode.LANGUAGES.put("sk", new String[] { "Slovak", "slovenčina" });
        JDGeoCode.LANGUAGES.put("sl", new String[] { "Slovenian", "slovenščina" });
        JDGeoCode.LANGUAGES.put("sm", new String[] { "Samoan", "gagana fa'a Samoa" });
        JDGeoCode.LANGUAGES.put("sn", new String[] { "Shona", "chiShona" });
        JDGeoCode.LANGUAGES.put("so", new String[] { "Somali", "Soomaaliga; af Soomaali" });
        JDGeoCode.LANGUAGES.put("sq", new String[] { "Albanian", "Shqip" });
        JDGeoCode.LANGUAGES.put("sr", new String[] { "Serbian", "српски језик" });
        JDGeoCode.LANGUAGES.put("ss", new String[] { "Swati", "SiSwati" });
        JDGeoCode.LANGUAGES.put("st", new String[] { "Southern Sotho", "Sesotho" });
        JDGeoCode.LANGUAGES.put("su", new String[] { "Sundanese", "Basa Sunda" });
        JDGeoCode.LANGUAGES.put("sv", new String[] { "Swedish", "svenska" });
        JDGeoCode.LANGUAGES.put("sw", new String[] { "Swahili", "Kiswahili" });
        JDGeoCode.LANGUAGES.put("ta", new String[] { "Tamil", "தமிழ்" });
        JDGeoCode.LANGUAGES.put("te", new String[] { "Telugu", "తెలుగు" });
        JDGeoCode.LANGUAGES.put("tg", new String[] { "Tajik", "ไทย" });
        JDGeoCode.LANGUAGES.put("ti", new String[] { "Tigrinya", "ትግርኛ" });
        JDGeoCode.LANGUAGES.put("tk", new String[] { "Turkmen", "Türkmen; Түркмен" });
        JDGeoCode.LANGUAGES.put("tl", new String[] { "Tagalog", "Tagalog" });
        JDGeoCode.LANGUAGES.put("tn", new String[] { "Tswana", "Setswana" });
        JDGeoCode.LANGUAGES.put("to", new String[] { "Tonga", "faka Tonga" });
        JDGeoCode.LANGUAGES.put("tr", new String[] { "Turkish", "Türkçe" });
        JDGeoCode.LANGUAGES.put("ts", new String[] { "Tsonga", "Xitsonga" });
        JDGeoCode.LANGUAGES.put("tt", new String[] { "Tatar", "Twi" });
        JDGeoCode.LANGUAGES.put("ty", new String[] { "Tahitian", "Reo Mā`ohi" });
        JDGeoCode.LANGUAGES.put("ug", new String[] { "Uighur", "Українська" });
        JDGeoCode.LANGUAGES.put("ur", new String[] { "Urdu", "اردو" });
        JDGeoCode.LANGUAGES.put("uk", new String[] { "Ukrainian", "Ukraïna" });
        JDGeoCode.LANGUAGES.put("uz", new String[] { "Uzbek", "Tshivenḓa" });
        JDGeoCode.LANGUAGES.put("vi", new String[] { "Vietnamese", "Tiếng Việt" });
        JDGeoCode.LANGUAGES.put("vo", new String[] { "Volapük", "Volapük" });
        JDGeoCode.LANGUAGES.put("wa", new String[] { "Walloon", "Walon" });
        JDGeoCode.LANGUAGES.put("wo", new String[] { "Wolof", "Wollof" });
        JDGeoCode.LANGUAGES.put("xh", new String[] { "Xhosa", "isiXhosa" });
        JDGeoCode.LANGUAGES.put("yi", new String[] { "Yiddish", "ייִדיש" });
        JDGeoCode.LANGUAGES.put("yo", new String[] { "Yoruba", "Yorùbá" });
        JDGeoCode.LANGUAGES.put("za", new String[] { "Zhuang", "Saɯ cueŋƅ; Saw cuengh" });
        JDGeoCode.LANGUAGES.put("zh", new String[] { "Chinese", "中文 (Zhōngwén), 汉语, 漢語" });
        JDGeoCode.LANGUAGES.put("zu", new String[] { "Zulu", "isiZulu" });
    }

    /**
     * COuntrycodes according to ISO 3166 (Topleveldomains)
     */
    public final static HashMap<String, String>   COUNTRIES  = new HashMap<String, String>();
    static {

        JDGeoCode.COUNTRIES.put("AD", "Andorra");
        JDGeoCode.COUNTRIES.put("AE", "United Arab Emirates");
        JDGeoCode.COUNTRIES.put("AF", "Afghanistan");
        JDGeoCode.COUNTRIES.put("AG", "Antigua And Barbuda");

        JDGeoCode.COUNTRIES.put("AI", "Anguilla");
        JDGeoCode.COUNTRIES.put("AL", "Albania");
        JDGeoCode.COUNTRIES.put("AM", "Armenia");
        JDGeoCode.COUNTRIES.put("AN", "Netherlands Antilles");
        JDGeoCode.COUNTRIES.put("AO", "Angola");
        JDGeoCode.COUNTRIES.put("AQ", "Antarctica");

        JDGeoCode.COUNTRIES.put("AR", "Argentina");
        JDGeoCode.COUNTRIES.put("AS", "American Samoa");
        JDGeoCode.COUNTRIES.put("AT", "Austria");
        JDGeoCode.COUNTRIES.put("AU", "Australia");
        JDGeoCode.COUNTRIES.put("AW", "Aruba");
        JDGeoCode.COUNTRIES.put("AZ", "Azerbaijan");

        JDGeoCode.COUNTRIES.put("BA", "Bosnia And Herzegowina");
        JDGeoCode.COUNTRIES.put("BB", "Barbados");
        JDGeoCode.COUNTRIES.put("BD", "Bangladesh");
        JDGeoCode.COUNTRIES.put("BE", "Belgium");
        JDGeoCode.COUNTRIES.put("BF", "Burkina Faso");
        JDGeoCode.COUNTRIES.put("BG", "Bulgaria");

        JDGeoCode.COUNTRIES.put("BH", "Bahrain");
        JDGeoCode.COUNTRIES.put("BI", "Burundi");
        JDGeoCode.COUNTRIES.put("BJ", "Benin");
        JDGeoCode.COUNTRIES.put("BM", "Bermuda");
        JDGeoCode.COUNTRIES.put("BN", "Brunei Darussalam");
        JDGeoCode.COUNTRIES.put("BO", "Bolivia");

        JDGeoCode.COUNTRIES.put("BR", "Brazil");
        JDGeoCode.COUNTRIES.put("BS", "Bahamas");
        JDGeoCode.COUNTRIES.put("BT", "Bhutan");
        JDGeoCode.COUNTRIES.put("BV", "Bouvet Island");
        JDGeoCode.COUNTRIES.put("BW", "Botswana");
        JDGeoCode.COUNTRIES.put("BY", "Belarus");

        JDGeoCode.COUNTRIES.put("BZ", "Belize");

        JDGeoCode.COUNTRIES.put("CA", "Canada");
        JDGeoCode.COUNTRIES.put("CC", "Cocos (Keeling) Islands");

        JDGeoCode.COUNTRIES.put("CF", "Central African Republic");
        JDGeoCode.COUNTRIES.put("CG", "Congo");
        JDGeoCode.COUNTRIES.put("CH", "Switzerland");
        JDGeoCode.COUNTRIES.put("CI", "Cote d'Ivoire");
        JDGeoCode.COUNTRIES.put("CK", "Cook Islands");
        JDGeoCode.COUNTRIES.put("CL", "Chile");

        JDGeoCode.COUNTRIES.put("CM", "Cameroon");
        JDGeoCode.COUNTRIES.put("CN", "China");
        JDGeoCode.COUNTRIES.put("CO", "Colombia");
        JDGeoCode.COUNTRIES.put("CR", "Costa Rica");
        JDGeoCode.COUNTRIES.put("CU", "Cuba");
        JDGeoCode.COUNTRIES.put("CV", "Cape Verde");

        JDGeoCode.COUNTRIES.put("CX", "Christmas Island");
        JDGeoCode.COUNTRIES.put("CY", "Cyprus");
        JDGeoCode.COUNTRIES.put("CZ", "Czech Republic");
        JDGeoCode.COUNTRIES.put("DE", "Germany");
        JDGeoCode.COUNTRIES.put("DJ", "Djibouti");
        JDGeoCode.COUNTRIES.put("DK", "Denmark");

        JDGeoCode.COUNTRIES.put("DM", "Dominica");
        JDGeoCode.COUNTRIES.put("DO", "Dominican Republic");
        JDGeoCode.COUNTRIES.put("DZ", "Algeria");
        JDGeoCode.COUNTRIES.put("EC", "Ecuador");
        JDGeoCode.COUNTRIES.put("EE", "Estonia");
        JDGeoCode.COUNTRIES.put("EG", "Egypt");

        JDGeoCode.COUNTRIES.put("EH", "Western Sahara");
        JDGeoCode.COUNTRIES.put("ER", "Eritrea");
        JDGeoCode.COUNTRIES.put("ES", "Spain");
        JDGeoCode.COUNTRIES.put("ET", "Ethiopia");
        JDGeoCode.COUNTRIES.put("EU", "Europe (SSGFI only)");
        JDGeoCode.COUNTRIES.put("FI", "Finland");

        JDGeoCode.COUNTRIES.put("FJ", "Fiji");
        JDGeoCode.COUNTRIES.put("FK", "Falkland Islands (Malvinas)");
        JDGeoCode.COUNTRIES.put("FM", "Micronesia, Federated States Of");
        JDGeoCode.COUNTRIES.put("FO", "Faroe Islands");
        JDGeoCode.COUNTRIES.put("FR", "France");
        JDGeoCode.COUNTRIES.put("FX", "France, Metropolitan");

        JDGeoCode.COUNTRIES.put("GA", "Gabon");
        JDGeoCode.COUNTRIES.put("GB", "United Kingdom (UK)");
        JDGeoCode.COUNTRIES.put("GD", "Grenada");
        JDGeoCode.COUNTRIES.put("GE", "Georgia");

        JDGeoCode.COUNTRIES.put("GF", "French Guiana");
        JDGeoCode.COUNTRIES.put("GH", "Ghana");
        JDGeoCode.COUNTRIES.put("GI", "Gibraltar");
        JDGeoCode.COUNTRIES.put("GL", "Greenland");
        JDGeoCode.COUNTRIES.put("GM", "Gambia");
        JDGeoCode.COUNTRIES.put("GN", "Guinea");

        JDGeoCode.COUNTRIES.put("GP", "Guadeloupe");
        JDGeoCode.COUNTRIES.put("GQ", "Equatorial Guinea");
        JDGeoCode.COUNTRIES.put("GR", "Greece");
        JDGeoCode.COUNTRIES.put("GS", "South Georgia And The South Sandwich Islands");
        JDGeoCode.COUNTRIES.put("GT", "Guatemala");
        JDGeoCode.COUNTRIES.put("GU", "Guam");

        JDGeoCode.COUNTRIES.put("GW", "Guinea-Bissau");
        JDGeoCode.COUNTRIES.put("GY", "Guyana");
        JDGeoCode.COUNTRIES.put("HK", "Hong Kong");
        JDGeoCode.COUNTRIES.put("HM", "Heard And Mc Donald Islands");
        JDGeoCode.COUNTRIES.put("HN", "Honduras");
        JDGeoCode.COUNTRIES.put("HR", "Croatia (local name: Hrvatska)");

        JDGeoCode.COUNTRIES.put("HT", "Haiti");
        JDGeoCode.COUNTRIES.put("HU", "Hungary");
        JDGeoCode.COUNTRIES.put("ID", "Indonesia");
        JDGeoCode.COUNTRIES.put("IE", "Ireland");
        JDGeoCode.COUNTRIES.put("II", "International (SSGFI only)");
        JDGeoCode.COUNTRIES.put("IL", "Israel");

        JDGeoCode.COUNTRIES.put("IN", "India");
        JDGeoCode.COUNTRIES.put("IO", "British Indian Ocean Territory");
        JDGeoCode.COUNTRIES.put("IQ", "Iraq");
        JDGeoCode.COUNTRIES.put("IR", "Iran (Islamic Republic Of)");
        JDGeoCode.COUNTRIES.put("IS", "Iceland");
        JDGeoCode.COUNTRIES.put("IT", "Italy");

        JDGeoCode.COUNTRIES.put("JM", "Jamaica");
        JDGeoCode.COUNTRIES.put("JO", "Jordan");
        JDGeoCode.COUNTRIES.put("JP", "Japan");
        JDGeoCode.COUNTRIES.put("KE", "Kenya");
        JDGeoCode.COUNTRIES.put("KG", "Kyrgyzstan");
        JDGeoCode.COUNTRIES.put("KH", "Cambodia");
        JDGeoCode.COUNTRIES.put("KI", "Kiribati");
        JDGeoCode.COUNTRIES.put("KM", "Comoros");

        JDGeoCode.COUNTRIES.put("KN", "Saint Kitts And Nevis");
        JDGeoCode.COUNTRIES.put("KP", "Korea, Democratic People's Republic Of");
        JDGeoCode.COUNTRIES.put("KR", "Korea, Republic Of");
        JDGeoCode.COUNTRIES.put("KW", "Kuwait");
        JDGeoCode.COUNTRIES.put("KY", "Cayman Islands");
        JDGeoCode.COUNTRIES.put("KZ", "Kazakhstan");

        JDGeoCode.COUNTRIES.put("LA", "Lao People's Democratic Republic");
        JDGeoCode.COUNTRIES.put("LB", "Lebanon");
        JDGeoCode.COUNTRIES.put("LC", "Saint Lucia");
        JDGeoCode.COUNTRIES.put("LI", "Liechtenstein");
        JDGeoCode.COUNTRIES.put("LK", "Sri Lanka");
        JDGeoCode.COUNTRIES.put("LR", "Liberia");

        JDGeoCode.COUNTRIES.put("LS", "Lesotho");
        JDGeoCode.COUNTRIES.put("LT", "Lithuania");
        JDGeoCode.COUNTRIES.put("LU", "Luxembourg");
        JDGeoCode.COUNTRIES.put("LV", "Latvia");
        JDGeoCode.COUNTRIES.put("LY", "Libyan Arab Jamahiriya");
        JDGeoCode.COUNTRIES.put("MA", "Morocco");

        JDGeoCode.COUNTRIES.put("MC", "Monaco");
        JDGeoCode.COUNTRIES.put("MD", "Moldova, Republic Of");
        JDGeoCode.COUNTRIES.put("MG", "Madagascar");
        JDGeoCode.COUNTRIES.put("MH", "Marshall Islands");
        JDGeoCode.COUNTRIES.put("MK", "Macedonia, The Former Yugoslav Republic Of");
        JDGeoCode.COUNTRIES.put("ML", "Mali");

        JDGeoCode.COUNTRIES.put("MM", "Myanmar");
        JDGeoCode.COUNTRIES.put("MN", "Mongolia");
        JDGeoCode.COUNTRIES.put("MO", "Macau");
        JDGeoCode.COUNTRIES.put("MP", "Northern Mariana Islands");
        JDGeoCode.COUNTRIES.put("MQ", "Martinique");
        JDGeoCode.COUNTRIES.put("MR", "Mauritania");

        JDGeoCode.COUNTRIES.put("MS", "Montserrat");
        JDGeoCode.COUNTRIES.put("MT", "Malta");
        JDGeoCode.COUNTRIES.put("MU", "Mauritius");
        JDGeoCode.COUNTRIES.put("MV", "Maldives");
        JDGeoCode.COUNTRIES.put("MW", "Malawi");
        JDGeoCode.COUNTRIES.put("MX", "Mexico");

        JDGeoCode.COUNTRIES.put("MY", "Malaysia");
        JDGeoCode.COUNTRIES.put("MZ", "Mozambique");

        JDGeoCode.COUNTRIES.put("NA", "Namibia");
        JDGeoCode.COUNTRIES.put("NC", "New Caledonia");

        JDGeoCode.COUNTRIES.put("NE", "Niger");
        JDGeoCode.COUNTRIES.put("NF", "Norfolk Island");
        JDGeoCode.COUNTRIES.put("NG", "Nigeria");
        JDGeoCode.COUNTRIES.put("NI", "Nicaragua");
        JDGeoCode.COUNTRIES.put("NL", "Netherlands");
        JDGeoCode.COUNTRIES.put("NO", "Norway");

        JDGeoCode.COUNTRIES.put("NP", "Nepal");
        JDGeoCode.COUNTRIES.put("NR", "Nauru");
        JDGeoCode.COUNTRIES.put("NU", "Niue");
        JDGeoCode.COUNTRIES.put("NZ", "New Zealand");
        JDGeoCode.COUNTRIES.put("OM", "Oman");

        JDGeoCode.COUNTRIES.put("PE", "Peru");
        JDGeoCode.COUNTRIES.put("PF", "French Polynesia");
        JDGeoCode.COUNTRIES.put("PG", "Papua New Guinea");
        JDGeoCode.COUNTRIES.put("PH", "Philippines");
        JDGeoCode.COUNTRIES.put("PK", "Pakistan");
        JDGeoCode.COUNTRIES.put("PL", "Poland");

        JDGeoCode.COUNTRIES.put("PM", "St. Pierre And Miquelon");
        JDGeoCode.COUNTRIES.put("PN", "Pitcairn");
        JDGeoCode.COUNTRIES.put("PR", "Puerto Rico");
        JDGeoCode.COUNTRIES.put("PT", "Portugal");
        JDGeoCode.COUNTRIES.put("PW", "Palau");
        JDGeoCode.COUNTRIES.put("PY", "Paraguay");

        JDGeoCode.COUNTRIES.put("QA", "Qatar");
        JDGeoCode.COUNTRIES.put("RE", "Reunion");
        JDGeoCode.COUNTRIES.put("RO", "Romania");
        JDGeoCode.COUNTRIES.put("RU", "Russian Federation");
        JDGeoCode.COUNTRIES.put("RW", "Rwanda");
        JDGeoCode.COUNTRIES.put("SA", "Saudi Arabia");

        JDGeoCode.COUNTRIES.put("SB", "Solomon Islands");
        JDGeoCode.COUNTRIES.put("SC", "Seychelles");
        JDGeoCode.COUNTRIES.put("SD", "Sudan");
        JDGeoCode.COUNTRIES.put("SE", "Sweden");
        JDGeoCode.COUNTRIES.put("SG", "Singapore");
        JDGeoCode.COUNTRIES.put("SH", "St. Helena");

        JDGeoCode.COUNTRIES.put("SI", "Slovenia");
        JDGeoCode.COUNTRIES.put("SJ", "Svalbard And Jan Mayen Islands");
        JDGeoCode.COUNTRIES.put("SK", "Slovakia (Slovak Republic)");
        JDGeoCode.COUNTRIES.put("SL", "Sierra Leone");
        JDGeoCode.COUNTRIES.put("SM", "San Marino");
        JDGeoCode.COUNTRIES.put("SN", "Senegal");

        JDGeoCode.COUNTRIES.put("SO", "Somalia");
        JDGeoCode.COUNTRIES.put("SR", "Suriname");
        JDGeoCode.COUNTRIES.put("ST", "Sao Tome And Principe");
        JDGeoCode.COUNTRIES.put("SV", "El Salvador");
        JDGeoCode.COUNTRIES.put("SY", "Syrian Arab Republic");
        JDGeoCode.COUNTRIES.put("SZ", "Swaziland");
        JDGeoCode.COUNTRIES.put("TC", "Turks And Caicos Islands");
        JDGeoCode.COUNTRIES.put("TD", "Chad");
        JDGeoCode.COUNTRIES.put("TF", "French Southern Territories");
        JDGeoCode.COUNTRIES.put("TG", "Togo");

        JDGeoCode.COUNTRIES.put("TH", "Thailand");
        JDGeoCode.COUNTRIES.put("TJ", "Tajikistan");
        JDGeoCode.COUNTRIES.put("TK", "Tokelau");
        JDGeoCode.COUNTRIES.put("TM", "Turkmenistan");
        JDGeoCode.COUNTRIES.put("TN", "Tunisia");
        JDGeoCode.COUNTRIES.put("TO", "Tonga");

        JDGeoCode.COUNTRIES.put("TP", "East Timor");
        JDGeoCode.COUNTRIES.put("TR", "Turkey");
        JDGeoCode.COUNTRIES.put("TT", "Trinidad And Tobago");
        JDGeoCode.COUNTRIES.put("TV", "Tuvalu");
        JDGeoCode.COUNTRIES.put("TW", "Taiwan");
        JDGeoCode.COUNTRIES.put("TZ", "Tanzania, United Republic Of");

        JDGeoCode.COUNTRIES.put("UA", "Ukraine");
        JDGeoCode.COUNTRIES.put("UG", "Uganda");
        JDGeoCode.COUNTRIES.put("UM", "United States Minor Outlying Islands");
        JDGeoCode.COUNTRIES.put("US", "United States");
        JDGeoCode.COUNTRIES.put("UY", "Uruguay");
        JDGeoCode.COUNTRIES.put("UZ", "Uzbekistan");

        JDGeoCode.COUNTRIES.put("VA", "Vatican City State (Holy See)");
        JDGeoCode.COUNTRIES.put("VC", "Saint Vincent And The Grenadines");
        JDGeoCode.COUNTRIES.put("VE", "Venezuela");
        JDGeoCode.COUNTRIES.put("VG", "Virgin Islands (British)");
        JDGeoCode.COUNTRIES.put("VI", "Virgin Islands (U.S.)");
        JDGeoCode.COUNTRIES.put("VN", "Viet Nam");

        JDGeoCode.COUNTRIES.put("VU", "Vanuatu");
        JDGeoCode.COUNTRIES.put("WF", "Wallis And Futuna Islands");
        JDGeoCode.COUNTRIES.put("WS", "Samoa");
        JDGeoCode.COUNTRIES.put("YE", "Yemen");
        JDGeoCode.COUNTRIES.put("YT", "Mayotte");
        JDGeoCode.COUNTRIES.put("YU", "Yugoslavia");

        JDGeoCode.COUNTRIES.put("ZA", "South Africa");
        JDGeoCode.COUNTRIES.put("ZM", "Zambia");
        JDGeoCode.COUNTRIES.put("ZR", "Zaire");
        JDGeoCode.COUNTRIES.put("ZW", "Zimbabwe");
    }

    public static final HashMap<String, String>   EXTENSIONS = new HashMap<String, String>();
    static {
        JDGeoCode.EXTENSIONS.put("hans", "simplified");
        JDGeoCode.EXTENSIONS.put("hant", "traditional");
    }

    /**
     * Returns the countrycode for a givven countryname
     * 
     * @param name
     * @return
     */
    public static String getCountryCode(final String name) {
        for (final Entry<String, String> next : JDGeoCode.COUNTRIES.entrySet()) {
            if (next.getValue().equalsIgnoreCase(name)) { return next.getKey(); }
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
        for (final Entry<String, String> next : JDGeoCode.EXTENSIONS.entrySet()) {
            if (next.getValue().equalsIgnoreCase(name)) { return next.getKey(); }
        }
        return name;
    }

    /**
     * Returns the languagecode for a native or english languagename
     * 
     * @param name
     * @return
     */
    public static String getLanguageCode(final String name) {
        for (final Entry<String, String[]> next : JDGeoCode.LANGUAGES.entrySet()) {
            if (next.getValue()[0].equalsIgnoreCase(name) || next.getValue()[1].equalsIgnoreCase(name)) { return next.getKey(); }
        }
        return null;
    }

    /**
     * Returns the native language name for the givven code
     * 
     * @param code
     * @return
     */
    public static String getNativeLanguage(final String code) {
        final String[] ret = JDGeoCode.LANGUAGES.get(code);
        return ret == null ? null : ret[1];
    }

    public static String longToShort(final String lng) {
        String[] row = new AwReg(lng, "(.*?)\\[(.*)\\|(.*?)\\]").getRow(0);
        if (row != null) { return JDGeoCode.getLanguageCode(row[0].trim()) + "-" + JDGeoCode.getCountryCode(row[1].trim()) + "-" + JDGeoCode.getExtensionCode(row[2].trim()); }
        row = new AwReg(lng, "(.*?)\\[(.*)\\]").getRow(0);
        if (row != null) {
            final String countryCode = JDGeoCode.getCountryCode(row[1].trim());

            return JDGeoCode.getLanguageCode(row[0].trim()) + "-" + (countryCode != null ? countryCode : JDGeoCode.getExtensionCode(row[1].trim()));
        }

        return JDGeoCode.getLanguageCode(lng.trim());
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
            return languagecode.length() != 2 ? null : new String[] { lngCode, null, null };
        } else if (split.length == 2) {
            final boolean h = JDGeoCode.COUNTRIES.containsKey(split[1].toUpperCase());
            if (split[0].trim().length() != 2) { return null; }
            return new String[] { split[0].toLowerCase(), h ? split[1].toUpperCase() : null, h ? null : split[1] };
        } else {
            split = lngCode.split("\\-");
            final String split0 = split[0];
            final String split1 = split[1];

            if (split0.trim().length() != 2) { return null; }
            if (split1.trim().length() != 2) { return null; }
            return new String[] { split0.toLowerCase(), split1.toUpperCase(), split[2] };
        }
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

        final String language = JDGeoCode.LANGUAGES.get(p[0])[0];
        final String country = JDGeoCode.COUNTRIES.get(p[1]);
        String extension = JDGeoCode.EXTENSIONS.get(p[2]);
        if (extension == null) {
            extension = p[2];
        }
        final StringBuilder ret = new StringBuilder(language);
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
            final String language = JDGeoCode.LANGUAGES.get(p[0])[1];
            final String country = JDGeoCode.COUNTRIES.get(p[1]);
            String extension = JDGeoCode.EXTENSIONS.get(p[2]);
            if (extension == null) {
                extension = p[2];
            }

            final StringBuilder ret = new StringBuilder(language);
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
        } catch (final Exception e) {
            JDLogger.getLogger().severe("Bad languagecode: " + string);
            return JDGeoCode.toLongerNative("en");
        }
    }

}
