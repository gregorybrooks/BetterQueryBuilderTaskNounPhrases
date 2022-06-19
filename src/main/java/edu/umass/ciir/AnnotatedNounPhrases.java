package edu.umass.ciir;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class AnnotatedNounPhrases {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderTaskNounPhrases");
    private Map<String, AnnotatedNounPhraseDetail> annotatedNounPhrases = new HashMap<>();
    private List<String> validJudgments = Arrays.asList("P","E","G","F","B");
    private String annotatedNounPhasesFilePath;
    private String toBeAnnotatedFileName;
    private String toBeAnnotatedFilePath;
    private XSSFSheet phrasesSheet = null;
    private XSSFSheet metadataSheet = null;
    private XSSFWorkbook workbook = null;
    private int currentSheetRow = 0;
    private Spacy spacy;
    List<String> sentencesList = null;

    AnnotatedNounPhrases(Spacy spacy) {
        this.spacy = spacy;
    }

    public boolean hasAnnotations() {
        return annotatedNounPhrases != null;
    }

    public boolean containsPhrase(String phrase) {
        return annotatedNounPhrases.containsKey(phrase);
    }

    public String getPhraseAnnotation(String phrase) {
        return annotatedNounPhrases.get(phrase).getJudgment();
    }


    public List<String> getAllRelevantPhrases() {
        List<String> relevantPrases = new ArrayList<>();
        for (Map.Entry<String,AnnotatedNounPhraseDetail> e : annotatedNounPhrases.entrySet()) {
            String phrase = e.getKey();
            AnnotatedNounPhraseDetail judgment = e.getValue();
            if (!judgment.getJudgment().equals("B")) {
                relevantPrases.add(phrase);
            }
        }
        return relevantPrases;
    }

    private Set<String> getBuiltInStopPhrases() {
        Set<String> stopPhrases = new HashSet<>();
        String stopPhasesFileName = "/home/taskquerybuilder/stop_phrases.txt";
        try {
            if (fileExists(stopPhasesFileName)) {
                logger.info("Opening stop phrases file " + stopPhasesFileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(stopPhasesFileName)));
                String line = reader.readLine();
                while (line != null) {
                    stopPhrases.add(line.toLowerCase());
                    line = reader.readLine();
                }
                reader.close();
            } else {
                logger.info("No built-in stop phrases file");
            }
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
        return stopPhrases;
    }


    public List<String> getStopPhrases(String fileName, Task t) throws IOException, ParseException {
        openNounPhrasesJSONFile(fileName, t);
        List<String> stopWordsList = new ArrayList<>();
        for (Map.Entry<String,AnnotatedNounPhraseDetail> e : annotatedNounPhrases.entrySet()) {
            String phrase = e.getKey();
            AnnotatedNounPhraseDetail judgment = e.getValue();
            if ((judgment.getJudgment().equals("B") || judgment.getJudgment().equals("F"))) {  //TBD
                stopWordsList.add(phrase.toLowerCase());
            }
        }
        return stopWordsList;
    }

    private boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    public void openNounPhrasesJSONFile(String fileName, Task t) throws IOException, ParseException {
        // If there is no annotated phrases file, leave annotatedNounPhrases empty (not null)
        if (fileExists(fileName)) {
            Reader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(fileName)));
            JSONParser parser = new JSONParser();
            JSONObject phrasesMap = (JSONObject) parser.parse(reader);
            for (Iterator iterator = phrasesMap.keySet().iterator(); iterator.hasNext(); ) {
                String phrase = (String) iterator.next();
                JSONObject phraseEntry = (JSONObject) phrasesMap.get(phrase);
                String sentences = (String) phraseEntry.get("sentences");
                String judgment = ((String) Objects.requireNonNullElse(phraseEntry.get("judgment"), "")).toUpperCase();
                AnnotatedNounPhraseDetail detail = new AnnotatedNounPhraseDetail(judgment, sentences);
                if (phrase != null && validJudgments.stream().anyMatch(judgment::equals)) {
                    if (!annotatedNounPhrases.containsKey(phrase)) {
                        logger.info(detail.getJudgment() + ": " + phrase);
                        annotatedNounPhrases.put(phrase, detail);
                    }
                }
            }
        } else {
            logger.info("There is no annotated_task_level_noun_phrases file for this task");
        }
    }

    /* WRITE OPERATIONS */

    public void createToBeAnnotatedNounPhrasesFile(String toBeAnnotatedFilePath, Task t) {
        this.toBeAnnotatedFilePath = toBeAnnotatedFilePath;
        getSentences(t);
    }

    public void closeToBeAnnotatedNounPhrasesFile() {
        try {
            logger.info("Writing to-be-annotated-noun-phrases file to " + toBeAnnotatedFilePath);
            JSONObject targetTopMap = new JSONObject();
            for (Map.Entry<String,AnnotatedNounPhraseDetail> entry : annotatedNounPhrases.entrySet()) {
                String phrase = entry.getKey();
                AnnotatedNounPhraseDetail j = entry.getValue();
                JSONObject detail = new JSONObject();
                detail.put("judgment",j.getJudgment());
                detail.put("sentences",j.getSentences());
                targetTopMap.put(phrase, detail);
            }
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(toBeAnnotatedFilePath)));
            writer.write(targetTopMap.toJSONString());
            writer.close();
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    private List<String> callSpacy(String s) {
        return spacy.getSentences(s);
    }

    private void getSentenceRangesFromText(String text, List<SentenceRange> sentences) {
        List<String> spacySentences = callSpacy(text);
        int start = 0;
        int end = -1;
        int id = 0;
        for (String sentence : spacySentences) {
            ++id;
            if (id > 1) {
                start = text.indexOf(sentence, end);
                if (start == -1) {
                    System.out.println("ERROR: Cannot find spacy sentence in doc");
                }
            }
            end = start + sentence.length();
            String sentenceText = text.substring(start, end);
            if (sentence.length() > 0) {
                sentences.add(new SentenceRange(id, start, end, sentenceText));
            }
        }
    }

    private void getSentences(Task t) {
        Map<String, Integer> soFar = new HashMap<>();
        Set<String> uniqueDocIds = new HashSet<>();
        sentencesList = new ArrayList<>();
        for (ExampleDocument d : t.taskExampleDocs) {
            if (!uniqueDocIds.contains(d.getDocid())) {
                uniqueDocIds.add(d.getDocid());
                List<SentenceRange> sentences = new ArrayList<>();
                getSentenceRangesFromText(d.getDocText(), sentences);
                for (SentenceRange sentence : sentences) {
                    sentencesList.add(sentence.text);
                }
            }
        }
        for (Request r : t.getRequests()) {
            for (ExampleDocument d : r.reqExampleDocs) {
                if (!uniqueDocIds.contains(d.getDocid())) {
                    uniqueDocIds.add(d.getDocid());
                    List<SentenceRange> sentences = new ArrayList<>();
                    getSentenceRangesFromText(d.getDocText(), sentences);
                    for (SentenceRange sentence : sentences) {
                        sentencesList.add(sentence.text);
                    }
                }
            }
        }
        for (Request r : t.getRequests()) {
            for (String extr : r.getReqExtrList()) {
                List<SentenceRange> sentences = new ArrayList<>();
                getSentenceRangesFromText(extr, sentences);
                for (SentenceRange sentence : sentences) {
                    sentencesList.add(sentence.text);
                }
            }
        }
    }

    public void writeRow(String nounPhrase) {
        List<String> hits = new ArrayList<>();
        int counter = 0;
        int LIMIT = 5;
        for (String sentence : sentencesList) {
            if (sentence.contains(nounPhrase)) {
                if (++counter > LIMIT) {
                    break;
                }
                hits.add(sentence);
            }
        }
        String context = String. join("\n...", hits);
        AnnotatedNounPhraseDetail detail = new AnnotatedNounPhraseDetail("", context);
        annotatedNounPhrases.put(nounPhrase, detail);
    }
}
