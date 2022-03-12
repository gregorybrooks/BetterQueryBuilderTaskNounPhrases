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

    public TableTranslator() { }

    /**
     * A translator that uses a translation table.
     * The table is a text file that has lines like this: source_word[TAB]target_word[TAB]probability
     * where probability is a float like 0.000545658.
     */
    public TableTranslator(String programDirectory, String targetLanguage) {
        String ttable;
        if (targetLanguage.equals("FARSI")) {
//            ttable = "translation_tables/en-fa-3-col-ttable-no-normal.txt"; // FARSI
//            this.lowercaseNeeded = true;
            ttable = "translation_tables/CCAligned.en-fa.fw.actual.ti.final"; // FARSI
            this.lowercaseNeeded = false;
        } else if (targetLanguage.equals("ARABIC")) {
            this.lowercaseNeeded = true;
            ttable = "translation_tables/unidirectional-with-null-en-ar.simple-tok.txt";  // ARABIC
        } else {
            throw new BetterQueryBuilderException("Unsupported language: " + targetLanguage);
        }
        String translationTableFileName = programDirectory + ttable;

        File f = new File(translationTableFileName);
        if (f.exists()) {

            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(translationTableFileName));
            } catch (FileNotFoundException e) {
                throw new BetterQueryBuilderException("Could not read translation table file "
                        + translationTableFileName);
            }

            String line;
            while(true) {
                try {
                    if (!((line =br.readLine())!=null))
                        break;
                } catch (IOException e) {
                    throw new BetterQueryBuilderException("IO error while reading translation table file "
                            + translationTableFileName);
                }
                String[] tokens = line.split("[\t ]");
                String source = tokens[0];
                String target = tokens[1];
                Double prob = Double.valueOf(tokens[2]);
                if (!translationTable.containsKey(source)) {
                    List<TranslatedTerm> lst = new ArrayList<TranslatedTerm>();
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

    /**
     * Translate a string consisting of space-delimited words.
     * @param text the text to be translated
     * @return the translated text
     */
    @Override
    public String getTranslation(String text) {
        String newText = "";
        for (String originalTerm : text.split("[‘'’´!”“\"\\[\\]@ — .();?{}:,/\n\t-]")) {
            String term = originalTerm;
            if (term.length() > 2 && !stopWords.contains(term) && !isAllNumeric(term)) {
                String decodedTerm;
                if (lowercaseNeeded) {
                    decodedTerm = unidecode.decode(term).toLowerCase();
                } else {
                    decodedTerm = unidecode.decode(term);
                }
//                System.out.println("English term: " + decodedTerm);
                if (translationTable.containsKey(decodedTerm)) {
                    List<TranslatedTerm> sortedList = translationTable.get(decodedTerm).stream()
                            .filter(Predicate.isEqual("'").or(Predicate.isEqual("\"")).negate())  // ttable has ' as Arabic sometimes!
                            .sorted(new TranslatedTerm.SortByProbDesc())
                            .limit(1).collect(Collectors.toList());
                    for (TranslatedTerm t : sortedList) {
//                        System.out.println("Translated term: " + t.term);
                        String candidate = filterCertainCharactersPostTranslation(t.term);
//                        System.out.println("Translated term after filtering: " + candidate);
                        newText += (" " + candidate);
                    }
                    newText = newText.trim();
                }
            }
        }
        return newText;
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
