package edu.umass.ciir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * A translator that uses Rab's translator.
 */
public class MarianTranslator implements TranslatorInterface {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderTaskNounPhrases");

    private String programDirectory = "./";
    private String language = "";
    private String program = "";

    private LineOrientedPythonDaemon translatorDaemon = null;

    private void startDaemon() {
        logger.info("Starting translator daemon");
        translatorDaemon = new LineOrientedPythonDaemon(programDirectory,
                program, programDirectory);
    }

    // No need to do this, let it run until the whole program ends
    public void stopSpacy() {
        translatorDaemon.stop();
    }

    MarianTranslator(String programDirectory, String language) {
        if (!programDirectory.endsWith("/")) {
            programDirectory += "/";
        }
        this.programDirectory = programDirectory;
        this.language = language;
        if (language.equals("ARABIC")) {
            this.program = "machine-translation-service/batch_translate.py";
        } else if (language.equals("FARSI")) {
            this.program = "persiannlp/batch_translate.py";
        } else if (language.equals("RUSSIAN")) {
            this.program = "machine-translation-service/batch_translate_russian.py";
        }
        startDaemon();
    }

    // need to do this but only on the translated words, not on the Galago structured query
    // operators and their parameters
    // For now, omitting this filtering
    private static String filterCertainCharactersPostTranslation(String q) {
        if (q == null || q.length() == 0) {
            return q;
        }
        else {
            q = q.replaceAll("\\.", " ");  // with the periods the queries hang
            q = q.replaceAll("@", " ");  // Galago has @ multi-word token thing
            q = q.replaceAll("\"", " "); // remove potential of mis-matched quotes
            q = q.replaceAll("“", " ");  // causes Galago to silently run an empty query
            q = q.replaceAll("”", " ");

            q = q.replaceAll("\\(\\)", " ");
            q = q.replaceAll("\\)", " ");
            q = q.replaceAll(":", " ");
            q = q.replaceAll("-", " ");
            q = q.replaceAll("\\/", " ");
            q = q.replaceAll("\\\\", " ");

            return q;
        }
    }

    @Override
    public List<String> getTranslationTerms (List<String> strings) {
        // NOT IMPLEMENTED
        return new ArrayList<>();
    }

    @Override
    public List<String> getSingleTranslationTerm(List<String> strings) {
        // NOT IMPLEMENTED
        return new ArrayList<>();
    }

    private int MAX_SENTENCES_PER_TRANSLATION = 10;

    @Override
    public String getTranslation(String text) {
        List<String> inputList = new ArrayList<>(Arrays.asList(text.split("[.]")));
        List<String> outputList = getTranslations(inputList);
        String outputString = "";
        for (String phrase : outputList) {
            outputString += phrase + " ";
        }
        return outputString;
    }

    @Override
    public List<String> getTranslations(List<String> inputList) {
        List<List<String>> parts = new ArrayList<>();
        final int N = inputList.size();
        for (int i = 0; i < N; i += MAX_SENTENCES_PER_TRANSLATION) {
            parts.add(new ArrayList<String>(
                    inputList.subList(i, Math.min(N, i + MAX_SENTENCES_PER_TRANSLATION)))
            );
        }

        List<String> outputList = new ArrayList<>();
        for (List<String> part : parts) {
            for (String s : translatorDaemon.getAnswers(part)) {
                outputList.add(filterCertainCharactersPostTranslation(s));
            }
        }
        return outputList;
    }
}
