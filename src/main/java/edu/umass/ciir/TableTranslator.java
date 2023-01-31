package edu.umass.ciir;

import cz.jirutka.unidecode.Unidecode;

import java.io.*;
import java.util.*;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A translator that uses a translation table.
 */
public class TableTranslator implements TranslatorInterface {
    private Map<String, List<TranslatedTerm>> translationTable = new HashMap<>();
    private Unidecode unidecode = Unidecode.toAscii();
    private List<String> stopWords = new ArrayList<>();
    private boolean lowercaseNeeded = true;
    private boolean numberFilteringNeeded = false;
    private String targetLanguage;

    public TableTranslator() { }

    private final boolean containsDigit(String s) {
        boolean containsDigit = false;
        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) {
                if (containsDigit = Character.isDigit(c)) {
                    break;
                }
            }
        }
        return containsDigit;
    }

    /**
     * A translator that uses a translation table.
     * The table is a text file that has lines like this: source_word[TAB]target_word[TAB]probability
     * where probability is a float like 0.000545658.
     */
    public TableTranslator(String programDirectory, String targetLanguage) {
        String ttable;
        this.targetLanguage = targetLanguage;
        switch (targetLanguage) {
            case "FARSI":
//            ttable = "translation_tables/en-fa-3-col-ttable-no-normal.txt";
//            this.lowercaseNeeded = true;
                ttable = "translation_tables/CCAligned.en-fa.fw.actual.ti.final";
                this.lowercaseNeeded = false;
                break;
            case "ARABIC":
                this.lowercaseNeeded = true;
                ttable = "translation_tables/unidirectional-with-null-en-ar.simple-tok.txt";
                break;
            case "RUSSIAN":
                this.lowercaseNeeded = true;
                this.numberFilteringNeeded = true;
//                ttable = "translation_tables/berk-v0.2-ttables-en-ru.txt";
                ttable = "translation_tables/combined-en-ru.txt";
                break;
            case "CHINESE":
                this.lowercaseNeeded = true;
                ttable = "translation_tables/combined-en-zh.txt";
                break;
            case "KOREAN":
                this.lowercaseNeeded = true;
                ttable = "translation_tables/combined-en-ko.txt";
                break;
            default:
                throw new BetterQueryBuilderException("Unsupported language: " + targetLanguage);
        }
        String translationTableFileName = programDirectory + ttable;

        File f = new File(translationTableFileName);
        if (f.exists()) {

            BufferedReader br;

            try {
                br = new BufferedReader(new FileReader(translationTableFileName));
            } catch (FileNotFoundException e) {
                throw new BetterQueryBuilderException("Could not read translation table file "
                        + translationTableFileName);
            }

            String line;
            while(true) {
                try {
                    if ((line = br.readLine()) == null)
                        break;
                } catch (IOException e) {
                    throw new BetterQueryBuilderException("IO error while reading translation table file "
                            + translationTableFileName);
                }
                String[] tokens = line.split("[\t ]");
                String source = tokens[0];
                String target = tokens[1];
                Double prob = Double.valueOf(tokens[2]);
                /* The Russian translation table has lots of top translations that end in a number and are
                   not good translations. Any translation that has a number in it anywhere can be discarded.
                 */
                if (numberFilteringNeeded) {
                    if (containsDigit(target)) {
                        continue;
                    }
                    if (target.contains("#")) {
                        continue;
                    }
                    if (target.contains("&")) {
                        continue;
                    }
                }

                if (!translationTable.containsKey(source)) {
                    List<TranslatedTerm> lst = new ArrayList<>();
                    lst.add(new TranslatedTerm(target,prob));
                    translationTable.put(source, lst);
                } else {
                    List<TranslatedTerm> lst = translationTable.get(source);
                    lst.add(new TranslatedTerm(target,prob));
                }
            }
        } else {
            throw new BetterQueryBuilderException("Translation table file "
                    + translationTableFileName + " not found");
        }
        loadStopWords();
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    private void loadStopWords () {
        stopWords.add("the");
        stopWords.add("The");
        stopWords.add("var");
        stopWords.add("HTTPS");
        stopWords.add("https");
        stopWords.add("HTTP");
        stopWords.add("http");
        stopWords.add("HTML");
        stopWords.add("html");
        stopWords.add("JSON");
        stopWords.add("com");
        stopWords.add("www");
        stopWords.add("div");
        stopWords.add("rgb");
        stopWords.add("mp4");
        stopWords.add("Doc");
        stopWords.add("POST");
        stopWords.add("GIF");
        stopWords.add("NULL");
        stopWords.add("APP");
        stopWords.add("null");
        stopWords.add("src");
    }

    /*
    public static String filterCertainCharactersPostTranslation(String q) {
        if (q == null || q.length() == 0) {
            return q;
        }
        else {
            q = q.replaceAll("#", " ");  // can cause unknown NodeType in Galago
       OLD     q = q.replaceAll("\\.", " ");  // with the periods the queries hang
            q = q.replaceAll("@", " ");  // Galago has @ multi-word token thing
            q = q.replaceAll("\"", " "); // remove potential of mis-matched quotes
            q = q.replaceAll("“", " ");  // causes Galago to silently run an empty query
            q = q.replaceAll("”", " ");
            return q;
        }
    }
     */

    public static String filterCertainCharactersPostTranslation(String q) {
        if (q == null || q.length() == 0) {
            return q;
        }
        else {
            q = q.replaceAll("#", " ");  // can cause unknown NodeType in Galago
            q = q.replaceAll("\\.", " ");  // with the periods the queries hang
            q = q.replaceAll("@", " ");  // Galago has @ multi-word token thing
            q = q.replaceAll("\"", " "); // remove potential of mis-matched quotes
            q = q.replaceAll("“", " ");  // causes Galago to silently run an empty query
            q = q.replaceAll("”", " ");

//            q = q.replaceAll("\\(\\)", " ");
            q = q.replaceAll("\\(", " ");
            q = q.replaceAll("\\)", " ");
            q = q.replaceAll(":", " ");
            q = q.replaceAll("-", " ");
            q = q.replaceAll("\\/", " ");
            q = q.replaceAll("\\\\", " ");
            return q;
        }
    }

    private boolean isAllNumeric(String term) {
        if (term == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(term);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private boolean isAllKorean(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            i += Character.charCount(codepoint);
            if (Character.UnicodeScript.of(codepoint) != Character.UnicodeScript.HANGUL) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllChinese(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            i += Character.charCount(codepoint);
            if (Character.UnicodeScript.of(codepoint) != Character.UnicodeScript.HAN) {
                return false;
            }
        }
        return true;
    }

    private boolean isKoreanHangul(int codepoint) {
        return (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HANGUL);
    }
    /**
     * Translate a string consisting of space-delimited words.
     * @param text the text to be translated
     * @return the translated text
     */
    @Override
    public String getTranslation(String text) {
        String newText = "";
        for (String originalTerm : text.split("[‘'’´!”“\"\\[\\]@ — .();?{}:,/\n\t-]")) {
            if (originalTerm.length() > 2 && !stopWords.contains(originalTerm) && !(isAllNumeric(originalTerm))) {
                String decodedTerm;
                if (lowercaseNeeded) {
                    decodedTerm = unidecode.decode(originalTerm).toLowerCase();
                } else {
                    decodedTerm = unidecode.decode(originalTerm);
                }
//                System.out.println("English term: " + decodedTerm);
                if (translationTable.containsKey(decodedTerm)) {
                    List<TranslatedTerm> sortedList = translationTable.get(decodedTerm).stream()
                            .filter(Predicate.isEqual("'").or(Predicate.isEqual("\"")).negate())  // ttable has ' as Arabic sometimes!
                            .sorted(new TranslatedTerm.SortByProbDesc())
                            .limit(5).collect(Collectors.toList());
                    for (TranslatedTerm t : sortedList) {
//                        System.out.println("Translated term: " + t.term);
                        String candidate = filterCertainCharactersPostTranslation(t.term);
//                        System.out.println("Translated term after filtering: " + candidate);
                        if (this.targetLanguage.equals("KOREAN")) {
                            if (!isAllKorean(candidate)) {
                                continue;   // skip non-Korean translations
                            }
                        }
                        newText += (" " + candidate);
                        break;  // take the top 1 candidate only
                    }
                    newText = newText.trim();
                }
            }
        }
        if (this.targetLanguage.equals("CHINESE")) {
            /*
            if (isAllChinese(newText)) {
                newText = bigramIt(newText);
            } else {
                newText = "";
            }
             */
            newText = bigramIt(newText);
        }
        return newText;
    }

    private String bigramIt(String rawtext) {
        String[] sentences = rawtext.split("。");
        String newText = "";
        for (String sentence : sentences) {
            // Remove any whitespace and punctuation
            String text = sentence.replaceAll("\\p{Punct}", "");
            text = text.replaceAll("\\s+", "");

            if (text.length() == 1) {
                newText += text.charAt(0);
            } else if (text.length() == 2) {
                newText += text.substring(0, 2);
            } else {
                for (int i = 1; i < text.length(); ++i) {
                    newText += text.substring(i - 1, i + 1);
                    newText += " ";
                }
            }
            newText += " ";
        }
        return newText.trim();
    }

    @Override
    public List<String> getTranslations(List<String> strings) {
        List<String> outputList = new ArrayList<>();
        for (String x : strings) {
            String t = getTranslation(x);
            if (t.length() > 0) {
                outputList.add(t);
            }
        }
        return outputList;
    }

    @Override
    public List<String> getTranslationTerms(List<String> strings) {
        List<String> outputList = new ArrayList<>();
        for (String x : strings) {
            String t = getTranslation(x);
            if (t.length() > 0) {
                outputList.add(t);
            }
        }
        return outputList;
    }

    @Override
    public List<String> getSingleTranslationTerm(List<String> strings) {
        List<String> outputList = new ArrayList<>();
        for (String x : strings) {
            String t = getTranslation(x);
            if (t.length() > 0) {
                outputList.add(t);
            }
        }
        return outputList;
    }

    /*
    @Override
    public List<Pair<String,Double>> getTranslations (Map<String, Double> strings) {
        List<String> terms = new ArrayList<>(strings.keySet());
        List<String> translatedTerms = getTranslations(terms);
        Iterator<String> it1 = translatedTerms.iterator();
        Iterator<Double> it2 = strings.values().iterator();
        List<Pair<String,Double>> translatedTermsMap = new ArrayList<>();
        while (it1.hasNext() && it2.hasNext()) {
            translatedTermsMap.add(new Pair<>(it1.next(), it2.next()));
        }
        return translatedTermsMap;
    }
     */

}
