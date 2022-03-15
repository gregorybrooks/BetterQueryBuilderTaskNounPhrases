package edu.umass.ciir;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class AnnotatedNounPhrases {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderTaskNounPhrases");

    private Map<String, String> annotatedNounPhrases = null;
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
        return annotatedNounPhrases.get(phrase);
    }

    public List<String> getStopPhrases(Task t) {
        openNounPhrasesFile(t.taskNum + ".annotated_task_level_noun_phrases.xlsx", t);
        List<String> stopWordsList = new ArrayList<>();
        for (Map.Entry<String,String> e : annotatedNounPhrases.entrySet()) {
            String phrase = e.getKey();
            String judgment = e.getValue();
            if ((judgment.equals("B") || judgment.equals("F"))) {
                stopWordsList.add(phrase);
            }
        }
        return stopWordsList;
    }

    public void openNounPhrasesFile(String fileName, Task t) {
        try {
            List<String> validJudgments = Arrays.asList("P","E","G","F","E");

            // The annotated file is expected to be included in the Docker, in the home directory
            annotatedNounPhasesFilePath = fileName;
            File f = new File(annotatedNounPhasesFilePath);
            if (f.exists()) {
                logger.info("Opening " + annotatedNounPhasesFilePath);
                annotatedNounPhrases = new HashMap<>();
                FileInputStream excelFile = new FileInputStream(new File(annotatedNounPhasesFilePath));
                Workbook workbook = new XSSFWorkbook(excelFile);
                Sheet datatypeSheet = workbook.getSheetAt(1);
                Iterator<Row> iterator = datatypeSheet.iterator();

                boolean headerSkipped = false;

                while (iterator.hasNext()) {

                    String phrase = null;
                    String judgment = null;
                    Row currentRow = iterator.next();
                    if (!headerSkipped) {
                        headerSkipped = true;
                        continue;
                    }
                    Cell c = currentRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (c != null) {
                        phrase = c.getStringCellValue();
                    }
                    c = currentRow.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (c != null) {
                        judgment = c.getStringCellValue().toUpperCase(Locale.ROOT);
                        if (!validJudgments.stream().anyMatch(judgment::equals)) {
                            judgment = null;
                        }
                    }
                    if (phrase != null && judgment != null ) {
                        if (!annotatedNounPhrases.containsKey(phrase)) {
                            annotatedNounPhrases.put(phrase, judgment);
                        }
                    }
                }
                excelFile.close();
                logger.info("Annotations for this task:");
                for (Map.Entry<String,String> e : annotatedNounPhrases.entrySet()) {
                    logger.info(e.getKey() + ": " + e.getValue());
                }
            } else {
                logger.info("Noun phrases file " + annotatedNounPhasesFilePath + " not found");
            }
        } catch(Exception e){
            throw new BetterQueryBuilderException(e);
        }
    }

    /* WRITE OPERATIONS */

    public void createToBeAnnotatedNounPhrasesFile(String toBeAnnotatedFilePath, Task t) {
        this.toBeAnnotatedFilePath = toBeAnnotatedFilePath;
        workbook = new XSSFWorkbook();
        metadataSheet = workbook.createSheet("Instructions");
        phrasesSheet = workbook.createSheet("Task Level Noun Phrases");
        currentSheetRow = 0;
        writeHeaderRow();
        getSentences(t);
        setMetadata(t);
    }

    public void closeToBeAnnotatedNounPhrasesFile() {
        try {
            FileOutputStream outputStream = new FileOutputStream(toBeAnnotatedFilePath);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
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

    private void setMetadata(Task t) {
        int currentRow = 0;
        metadataSheet.setColumnWidth(0, 100*256);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        String x = "Consider the following task:";
        createCell(x, style, currentRow++);

        x = "";
        createCell(x, style, currentRow++);

        x = "Task Title: " + t.taskTitle;
        createCell(x, style, currentRow++);

        x = "Task Narrative: " + t.taskNarr;
        createCell(x, style, currentRow++);

        x = "Task Statement: " + t.taskStmt;
        createCell(x, style, currentRow++);

        x = "";
        createCell(x, style, currentRow++);

        x = "We have done some analysis of documents that are related to this topic, trying to find words, phrases, names, locations, and so on that we believe are related to the task. We're interested in whether these things are actually important to the task or not. Should they absolutely be present, are they useless, or are they actually distracting and counter-producive?";
        createCell(x, style, currentRow++);

        x = "Each item we found is listed below in column A. Column C show examples of how that term is used in the documents we analyzed.";
        createCell(x, style, currentRow++);

        x = "";
        createCell(x, style, currentRow++);

        x = "For each phrase, provide the following judgment values:";
        createCell(x, style, currentRow++);

        x = "Perfect means that the term is critically important and that you'd expect to see it in nearly any document that was related to this task";
        createCellWithBold(0, 7, x, style, currentRow++);

        x = "Excellent means that the term is likely to appear in most documents but there might be occasions when it isn't";
        createCellWithBold(0, 9, x, style, currentRow++);

        x = "Good means that the term probably occurs in relevant documents but could also occur in some other contexts";
        createCellWithBold(0, 4, x, style, currentRow++);

        x = "Fair means that the term could occur but it could also occur in numerous other tasks or context that are unrelated";
        createCellWithBold(0, 4, x, style, currentRow++);

        x = "Bad means that the term actually points away from the task -- documents that contain it are probably not relevant";
        createCellWithBold(0, 3, x, style, currentRow++);

        x = "";
        createCell(x, style, currentRow++);

        x = "Do as many as you can in 10 minutes. Feel free to skip around, to skip ones you can't figure out. If you're unsure, don't answer for that term.";
        createCell(x, style, currentRow++);
    }

    private void createCell(String x, XSSFCellStyle style, int currentRow) {
        Row row;
        XSSFCell cell;
        row = metadataSheet.createRow(currentRow);
        row.setHeight((short) -1);
        cell = (XSSFCell) row.createCell(0);
        cell.setCellStyle(style);
        cell.setCellValue(x);
    }

    private void createCellWithBold(int start, int end, String x, XSSFCellStyle style, int currentRow) {
        XSSFFont font1 = (XSSFFont) workbook.createFont();
        XSSFRichTextString richString = new XSSFRichTextString(x);
        font1.setBold(true);
        richString.applyFont(start, end, font1);
        Row row;
        XSSFCell cell;
        row = metadataSheet.createRow(currentRow);
        row.setHeight((short) -1);
        cell = (XSSFCell) row.createCell(0);
        cell.setCellStyle(style);
        cell.setCellValue(richString);
    }

    private void writeHeaderRow() {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        phrasesSheet.setColumnWidth(0, 25*256);
        phrasesSheet.setColumnWidth(1, 15*256);
        phrasesSheet.setColumnWidth(2, 100*256);
        Row row = phrasesSheet.createRow(currentSheetRow++);
        row.setHeight((short) -1);
        Cell cell = row.createCell(0);
        cell.setCellValue("Phrase");
        cell.setCellStyle(style);
        cell = row.createCell(1);
        cell.setCellStyle(style);
        cell.setCellValue("Judgment");
        cell = row.createCell(2);
        cell.setCellStyle(style);
        cell.setCellValue("Sentences");
    }

    public void writeRow(String nounPhrase) {
        phrasesSheet.setColumnWidth(0, 25*256);
        phrasesSheet.setColumnWidth(1, 15*256);
        phrasesSheet.setColumnWidth(2, 100*256);
        Row row = phrasesSheet.createRow(currentSheetRow++);
        row.setHeight((short) -1);
        row.createCell(0).setCellValue(nounPhrase);
        row.createCell(1).setCellValue("");

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
        String context = String. join("\n", hits);

        XSSFCell hssfCell = (XSSFCell) row.createCell(2);

        XSSFFont font1 = (XSSFFont) workbook.createFont();
        XSSFRichTextString richString = new XSSFRichTextString( context );
        font1.setBold(true);
//        font1.setItalic(true);

        int offset = 0;

        for (int iter = 1; iter <= LIMIT; ++iter) {
            offset = context.indexOf(nounPhrase, offset);
            if (offset == -1) {
                break;
            }
            richString.applyFont(offset, offset + nounPhrase.length(), font1);
            offset += nounPhrase.length();
        }
        XSSFCellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
//        row.setRowStyle(style);
        hssfCell.setCellStyle(style);
        hssfCell.setCellValue( richString );
    }
}
