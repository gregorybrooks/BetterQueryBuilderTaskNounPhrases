package edu.umass.ciir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * A translator that uses Rab's translator.
 */
public class RabTranslator implements TranslatorInterface {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderNGram");

    private String programDirectory = "./";
    private String logFileDirectory = "./";
    private String targetLanguage = "";
    private String ttable = "";

    RabTranslator(String programDirectory, String logFileDirectory, String targetLanguage) {
        if (!programDirectory.endsWith("/")) {
            programDirectory += "/";
        }
        this.programDirectory = programDirectory;
        if (!logFileDirectory.endsWith("/")) {
            logFileDirectory += "/";
        }
        this.logFileDirectory = logFileDirectory;
        this.targetLanguage = targetLanguage;
        if (targetLanguage.equals("ARABIC")) {
            this.ttable = "translation_tables/unidirectional-with-null-en-ar.simple-tok.txt";  // ARABIC
        } else if (targetLanguage.equals("FARSI")) {
            this.ttable = "translation_tables/en-fa-3-col-ttable-no-normal.txt"; // FARSI
        } else {
            throw new BetterQueryBuilderException("Unsupported language: " + targetLanguage);
        }
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
            return q;
        }
    }

    private List<String> callPythonTranslationProgram(List<String> strings, Integer numTranslationAlternatives,
                                                      String weighted)  {
        try {
            String program = "translation_package/batch_translate.py";
            logger.info("Calling " + programDirectory + program + " " + programDirectory + " " +
                    String.valueOf(numTranslationAlternatives) + " " + weighted);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    programDirectory + program, programDirectory, String.valueOf(numTranslationAlternatives), weighted);
            processBuilder.directory(new File(programDirectory));

            Process process = processBuilder.start();

            BufferedWriter called_process_stdin = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            BufferedReader called_process_stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            for (String s : strings) {
                called_process_stdin.write(s + "\n");
            }
            called_process_stdin.flush();
            called_process_stdin.close();

            List<String> phrases = new ArrayList<>();
            String line;
            while ((line = called_process_stdout.readLine()) != null) {
                phrases.add(line);
            }
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                throw new BetterQueryBuilderException("Unexpected " + exitVal + " exit value calling " + programDirectory + program );
            }
            return phrases;
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    @Override
    public List<String> getTranslations (List<String> strings) {
        return callPythonTranslationProgram(strings, 2, "weighted");
    }

    @Override
    public List<String> getTranslationTerms (List<String> strings) {
        return callPythonTranslationProgram(strings, 2, "get_terms");
    }

    @Override
    public List<String> getSingleTranslationTerm(List<String> strings) {
        return callPythonTranslationProgram(strings, 1, "get_terms");
    }

    /*
    @Override
    public List<Pair<String,Double>> getTranslations (Map<String, Double> strings) {
        List<String> terms = new ArrayList<>(strings.keySet());
        List<String> translatedTerms = callPythonTranslationProgram(terms, 2, "weighted");
        Iterator<String> it1 = translatedTerms.iterator();
        Iterator<Double> it2 = strings.values().iterator();
        List<Pair<String,Double>> translatedTermsMap = new ArrayList<>();
        while (it1.hasNext() && it2.hasNext()) {
            translatedTermsMap.add(new Pair<>(it1.next(), it2.next()));
        }
        return translatedTermsMap;
    }

     */

    @Override
    public String getTranslation(String text) {
        List<String> inputList = new ArrayList<>();
        List<String> outputList;
        inputList.add(text);
        outputList = getTranslations(inputList);
        return outputList.get(0);
    }
}
