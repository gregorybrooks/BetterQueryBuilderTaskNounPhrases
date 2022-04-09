package edu.umass.ciir;

import com.cedarsoftware.util.io.JsonWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class BetterQueryBuilderTaskNounPhrases {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderTaskNounPhrases");
    private Map<String, String> queries = new ConcurrentHashMap<>();
    private Map<String, String> nonTranslatedQueries = new ConcurrentHashMap<>();
    private String analyticTasksFile;
    private String mode;
    private String programDirectory;
    private String logFileLocation;
    private boolean targetLanguageIsEnglish;
    private String outputQueryFileName;
    private Spacy spacy;
    private String phase;
    private TranslatorInterface translator;
    private AnnotatedNounPhrases np;
    private AnnotatedSentences annotatedSentences;
    private boolean includeAllPhrases = false;
    private String queryFileDirectory;
    private List<Task> tasks = new ArrayList<>();


    /* Set the following to true if you want to create the to-be-annotated noun phrases spreadsheet,
        for the HITL to fill in.
     */
    boolean createToBeAnnotatedNounPhrasesFile = false;

    public void setTranslator(TranslatorInterface translator) {
        this.translator = translator;
    }
    private Set<String> stopPhrases = null;
    public void setQuery(String key, String query) {
        queries.put(key, query);
    }

    public void setNonTranslatedQuery(String key, String query) {
        nonTranslatedQueries.put(key, query);
    }

    private List<String> callSpacy(String s) {
        return spacy.getSentences(s);
    }

    /**
     * Configures the logger for this program.
     * @param logFileName Name to give the log file.
     */
    private void configureLogger(String logFileName) {
        SimpleFormatter formatterTxt;
        FileHandler fileTxt;
        try {
            // suppress the logging output to the console
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            if (handlers[0] instanceof ConsoleHandler) {
                rootLogger.removeHandler(handlers[0]);
            }
            logger.setLevel(Level.INFO);
            fileTxt = new FileHandler(logFileName);
            // create a TXT formatter
            formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
        } catch (Exception cause) {
            throw new BetterQueryBuilderException(cause);
        }
    }

    /**
     * Sets up logging for this program.
     */
    public void setupLogging(String logFileLocation) {
        String logFileName = logFileLocation + "/better-query-builder-task-noun-phrases.log";
        configureLogger(logFileName);
    }

    protected void writeQueryFile() {
        // Output the query list as a JSON file,
        // in the format Galago's batch-search expects as input
        logger.info("Writing query file " + outputQueryFileName);

        try {
            JSONArray qlist = new JSONArray();
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                JSONObject qentry = new JSONObject();
                qentry.put("number", entry.getKey());
                String query = entry.getValue();
                /* WRONG: it's a REGEX so this just removes all "#sdm" */
                //query = query.replaceAll("#sdm()", " ");

                query = query.replaceAll("#sdm\\(\\)", " ");   // empty #sdm causes Galago errors
                qentry.put("text", query);
                qlist.add(qentry);
            }
            JSONObject outputQueries = new JSONObject();
            outputQueries.put("queries", qlist);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(outputQueryFileName)));
            writer.write(outputQueries.toJSONString());
            writer.close();

            String niceFormattedJson = JsonWriter.formatJson(outputQueries.toJSONString());
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(outputQueryFileName + ".PRETTY.json")));
            writer.write(niceFormattedJson);
            writer.close();

            qlist = new JSONArray();
            for (Map.Entry<String, String> entry : nonTranslatedQueries.entrySet()) {
                JSONObject qentry = new JSONObject();
                qentry.put("number", entry.getKey());
                qentry.put("text", entry.getValue());
                qlist.add(qentry);
            }
            outputQueries = new JSONObject();
            outputQueries.put("queries", qlist);
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(outputQueryFileName + ".NON_TRANSLATED.json")));
            writer.write(outputQueries.toJSONString());
            writer.close();

            niceFormattedJson = JsonWriter.formatJson(outputQueries.toJSONString());
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(outputQueryFileName + ".NON_TRANSLATED.PRETTY.json")));
            writer.write(niceFormattedJson);
            writer.close();

        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    public static String filterCertainCharactersPostTranslation(String q) {
        if (q == null || q.length() == 0) {
            return q;
        }
        else {
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
    public static String filterCertainCharacters(String q) {
        if (q == null || q.length() == 0) {
            return q;
        }
        else {
            q = q.replaceAll("\\t", " ");
            q = q.replaceAll("\\n", " ");
            q = q.replaceAll("\\.", " ");  // with the periods the queries hang
            q = q.replaceAll("\\(", " ");  // parentheses are included in the token
            q = q.replaceAll("\\)", " ");  // which causes that term to not be matched
            q = q.replaceAll("@", " ");  // Galago has @ multi-word token thing
            q = q.replaceAll("#", " ");  // Galago thinks #926 is an illegal node type
            q = q.replaceAll("\"", " "); // remove potential of mis-matched quotes
            q = q.replaceAll("“", " ");  // causes Galago to silently run an empty query
            q = q.replaceAll("”", " ");
            q = q.replaceAll("”", " ");
            q = q.replaceAll("\\{", " ");
            q = q.replaceAll("\\}", " ");
            q = q.replaceAll("\\=", " ");
            q = q.replaceAll("\\|", " ");
            q = q.replaceAll("\\<", " ");
            q = q.replaceAll("\\>", " ");
            q = q.replaceAll("\\+", " ");
            return q;
        }
    }

    private String getOptionalValue(JSONObject t, String field) {
        if (t.containsKey(field)) {
            return (String) t.get(field);
        } else {
            return "";
        }
    }

    private void readTaskFile() {
        try {
            logger.info("Reading analytic tasks info file " + analyticTasksFile);

            Reader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(analyticTasksFile)));
            JSONParser parser = new JSONParser();
            JSONArray tasksJSON = (JSONArray) parser.parse(reader);
            for (Object oTask : tasksJSON) {
                JSONObject t = (JSONObject) oTask;
                String taskNum = (String) t.get("task-num");
                String taskTitle = getOptionalValue(t, "task-title");
                String taskStmt = getOptionalValue(t, "task-stmt");
                String taskNarr = getOptionalValue(t, "task-narr");
                JSONObject taskDocMap = (JSONObject) t.get("task-docs");
                List<ExampleDocument> taskExampleDocuments = new ArrayList<>();
                for (Iterator iterator = taskDocMap.keySet().iterator(); iterator.hasNext(); ) {
                    String entryKey = (String) iterator.next();
                    JSONObject taskDoc = (JSONObject) taskDocMap.get(entryKey);
                    String highlight = getOptionalValue(taskDoc, "highlight");
                    String docText = (String) taskDoc.get("doc-text");
                    String docID = (String) taskDoc.get("doc-id");
                    List<SentenceRange> sentences = new ArrayList<>();
                    JSONArray jsonSentences = (JSONArray) taskDoc.get("sentences");
                    for (Object jsonObjectSentenceDescriptor : jsonSentences) {
                        JSONObject jsonSentenceDescriptor = (JSONObject) jsonObjectSentenceDescriptor;
                        long start = (long) jsonSentenceDescriptor.get("start");
                        long end = (long) jsonSentenceDescriptor.get("end");
                        long id = (long) jsonSentenceDescriptor.get("id");
                        String sentence = docText.substring((int) start, (int) end);
                        sentences.add(new SentenceRange((int) id, (int) start, (int) end, sentence));
                    }
                    taskExampleDocuments.add(new ExampleDocument(docID, docText, highlight, sentences));
                }
                JSONArray taskRequests = (JSONArray) t.get("requests");
                List<Request> requests = new ArrayList<>();
                for (Object o : taskRequests) {
                    JSONObject request = (JSONObject) o;
                    String reqText = getOptionalValue(request, "req-text");
                    String reqNum = (String) request.get("req-num");
                    JSONObject requestDocMap = (JSONObject) request.get("req-docs");
                    List<ExampleDocument> requestExampleDocuments = new ArrayList<>();
                    for (Iterator iterator = requestDocMap.keySet().iterator(); iterator.hasNext(); ) {
                        String entryKey = (String) iterator.next();
                        JSONObject reqDoc = (JSONObject) requestDocMap.get(entryKey);
                        String highlight = getOptionalValue(reqDoc, "highlight");
                        String docText = (String) reqDoc.get("doc-text");
                        String docID = (String) reqDoc.get("doc-id");
                        List<SentenceRange> sentences = new ArrayList<>();
                        JSONArray jsonSentences = (JSONArray) reqDoc.get("sentences");
                        for (Object jsonObjectSentenceDescriptor : jsonSentences) {
                            JSONObject jsonSentenceDescriptor = (JSONObject) jsonObjectSentenceDescriptor;
                            long start = (long) jsonSentenceDescriptor.get("start");
                            long end = (long) jsonSentenceDescriptor.get("end");
                            long id = (long) jsonSentenceDescriptor.get("id");
                            String sentence = docText.substring((int) start, (int) end);
                            sentences.add(new SentenceRange((int) id, (int) start, (int) end, sentence));
                        }
                        requestExampleDocuments.add(new ExampleDocument(docID, docText, highlight, sentences));
                    }
                    requests.add(new Request(reqNum, reqText, requestExampleDocuments));
                }
                tasks.add(new Task(taskNum, taskTitle, taskStmt, taskNarr, taskExampleDocuments, requests));
            }
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    protected void buildQueries() {
        logger.info("Building task-level queries");
        for (Task t : tasks) {
            buildDocsString(t);
        }
    }

    private boolean isRelevant(String judgment) {
        return (judgment.equals("P") || judgment.equals("E") || judgment.equals("G"));
    }

    protected void buildDocsString(Task t) {
        logger.info("Building query for task " + t.taskNum);

        if (mode.equals("HITL")) {
            String toBeAnnotatedNounPhrasesFileName = queryFileDirectory
                    + t.taskNum + ".to_be_annotated_task_level_noun_phrases.json";
            if (!fileExists(toBeAnnotatedNounPhrasesFileName)) {
                logger.info("Building the to_be_annotated_task_level_noun_phrases.json file for this task");
                np.createToBeAnnotatedNounPhrasesFile(toBeAnnotatedNounPhrasesFileName, t);
                createToBeAnnotatedNounPhrasesFile = true;
            } else {
                createToBeAnnotatedNounPhrasesFile = false;
                // Open the phrase annotation file if there is one for this task
                try {
                    logger.info("Opening the annotated_task_level_noun_phrases file for this task");
                    np.openNounPhrasesJSONFile(queryFileDirectory
                            + t.taskNum + ".annotated_task_level_noun_phrases.json", t);
                } catch (Exception e) {
                    throw new BetterQueryBuilderException(e);
                }
            }

            /* As a side-effect of this function, we create the to_be_annotated_sentences file if is not there */
            String toBeAnnotatedSentencesFileName = queryFileDirectory
                    + t.taskNum + ".to_be_annotated_sentences.json";
            if (!fileExists(toBeAnnotatedSentencesFileName)) {
                logger.info("Building the to_be_annotated_sentences.json file for this task");
                annotatedSentences.createToBeAnnotatedSentencesFile(toBeAnnotatedSentencesFileName, t);
            }
        }

        Map<String, Integer> soFar = new HashMap<>();
        Set<String> uniqueDocIds = new HashSet<>();
        /*
         * Extract unique noun phrases from the example documents,
         * with document use counts (how many docs use each phrase)
         */
        for (ExampleDocument d : t.taskExampleDocs) {
            if (!uniqueDocIds.contains(d.getDocid())) {
                uniqueDocIds.add(d.getDocid());
                addNounPhrases(soFar, d.getDocText(), "TASK DOC " + d.getDocid(), t.taskNum);
            }
        }
        for (Request r : t.getRequests()) {
            for (ExampleDocument d : r.reqExampleDocs) {
                if (!uniqueDocIds.contains(d.getDocid())) {
                    uniqueDocIds.add(d.getDocid());
                    addNounPhrases(soFar, d.getDocText(), "REQ DOC " + d.getDocid(), t.taskNum);
                }
            }
        }
        /*
         * Also extract unique noun phrases from any "highlights"
         * provided for the requests for this task
         */
        for (Request r : t.getRequests()) {
            for (String extr : r.getReqExtrList()) {
                addNounPhrases(soFar, extr, "HIGHLIGHT", t.taskNum);
            }
        }

        /*
         * Make a final list of those unique noun phrases used in more
         * than one example doc. (Include them all if there is only one example doc.)
         */

        /* Sort the unique noun phrases by document use count, descending */
        LinkedHashMap<String, Integer> reverseSortedMap = new LinkedHashMap<>();
        soFar.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        includeAllPhrases = false;

        List<String> finalList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : reverseSortedMap.entrySet()) {
            if (createToBeAnnotatedNounPhrasesFile) {    // we are creating the annotated noun phrases file
                /* Write out the selected noun phrases to a file to be annotated by the HITL */
                if (includeAllPhrases || (uniqueDocIds.size() == 1) || (entry.getValue() > 1)) {
                    np.writeRow(entry.getKey());
                    finalList.add(filterCertainCharacters(entry.getKey()));
                }
            } else {
                if (mode.equals("HITL")) {
                    if (np.hasAnnotations()) {   // if there is an annotated noun phrases file
                        //logger.info("Looking for phrase in annotations: " + entry.getKey());
                        if (np.containsPhrase(entry.getKey())) {
                            //logger.info("Phrase found in annotations");
                            if (isRelevant(np.getPhraseAnnotation(entry.getKey()))) {
                                //logger.info("Phrase is relevant, using it");
                                finalList.add(filterCertainCharacters(entry.getKey()));
                            } else {
                                //logger.info("Phrase is not relevant, skipping it");
                                ;  // annotator said it was not relevant, don't add it to finalList
                            }
                        } else { // annotation file has nothing to say about this phrase, so use normal selection criteria
                            //logger.info("Phrase not found in annotations, using normal selection criteria for it");
                            if (includeAllPhrases || (uniqueDocIds.size() == 1) || (entry.getValue() > 1)) {
                                //logger.info("Using the phrase");
                                finalList.add(filterCertainCharacters(entry.getKey()));
                            } else {
                                //logger.info("Not using the phrase");
                            }
                        }
                    }
                } else {   // not HITL mode or no annotation file--use the normal selection criteria for every phrase
                    //logger.info("Using normal selection criteria");
                    if (includeAllPhrases || (uniqueDocIds.size() == 1) || (entry.getValue() > 1)) {
                        //logger.info("Using the phrase");
                        finalList.add(filterCertainCharacters(entry.getKey()));
                    } else {
                        //logger.info("Not using the phrase");
                    }
                }
            }
        }

        if (createToBeAnnotatedNounPhrasesFile) {
            np.closeToBeAnnotatedNounPhrasesFile();
        }

/*
        System.out.println("****************************** " + t.taskNum + ": BEGIN SELECTED NOUN PHRASES:");
        for (String phrase : finalList) {
            System.out.println(phrase);
        }
        System.out.println("****************************** " + t.taskNum + ": END SELECTED NOUN PHRASES");
*/

        boolean useTaskParts = (mode.equals("AUTO-HITL") || mode.equals("HITL"));

        /*
         * Translate finalList, then put all those phrases into a string wrapped
         * in a #combine operator.
         * We also construct a non-translated version of the query for debugging purposes.
         */
        List<String> nonTranslatedFinalList = new ArrayList<>();
        /* First the non-translated version: */
        for (String phrase : finalList) {
            if (!phrase.contains(" ")) {
                nonTranslatedFinalList.add(phrase);
            } else {
                /* For multi-word phrases, we wrap the phrase in a sequential dependence operator */
                nonTranslatedFinalList.add("#sdm(" + phrase + ") ");
            }
        }
        /*
         * Next the translated version (translation includes adding #sdm
         * operators where appropriate
         */
        List<String> translatedFinalList;
        if (!targetLanguageIsEnglish) {
/*
            System.out.println("ENGLISH PHRASES:");
            for (String s : finalList) {
                System.out.println(s);
            }
*/
            translatedFinalList = translator.getTranslations(finalList);
/*
            System.out.println("BECOMES:");
            for (String s : translatedFinalList) {
                System.out.println(s);
            }
*/
        } else {
            translatedFinalList = nonTranslatedFinalList;
        }

        /*
         * Convert the final translated and non-translated lists of noun phrases
         * into strings wrapped in #combine operators
         */
        StringBuilder nonTranslatedNounPhrasesStringBuilder = new StringBuilder();
        for (String phrase : nonTranslatedFinalList) {
            nonTranslatedNounPhrasesStringBuilder.append(phrase).append(" ");
        }
        String nonTranslatedNounPhrasesString = nonTranslatedNounPhrasesStringBuilder.toString();
        nonTranslatedNounPhrasesString = "#combine(" + nonTranslatedNounPhrasesString + ") ";

        String nounPhrasesString;
        if (!targetLanguageIsEnglish) {
            StringBuilder nounPhrasesStringBuilder = new StringBuilder();
            for (String phrase : translatedFinalList) {
                nounPhrasesStringBuilder.append(filterCertainCharactersPostTranslation(phrase)).append(" ");
            }
            nounPhrasesString = nounPhrasesStringBuilder.toString();
            nounPhrasesString = "#combine(" + nounPhrasesString + ") ";
        } else {
            nounPhrasesString = nonTranslatedNounPhrasesString;
        }

        /*
         * At this point, nounPhasesString has the most important noun phrases from
         * the task's and its requests' example docs and its requests' highlights
         */

        /* Add query elements from the Task definition, which is allowed in AUTO-HITL and HITL mode */
        List<String> taskParts = new ArrayList<>();
        boolean hasTaskParts = false;
        if (useTaskParts) {
            if (t.taskTitle != null || t.taskStmt != null || t.taskNarr != null) {
                hasTaskParts = true;
                if (t.taskTitle != null) {
                    String part = filterCertainCharacters(t.taskTitle);
                    taskParts.add(part);
/*
                    System.out.println("****************************** " + t.taskNum + ": BEGIN TASK TITLE:");
                    System.out.println(part);
                    System.out.println("****************************** " + t.taskNum + ": END TASK TITLE");
*/
                }
                if (t.taskStmt != null) {
                    String part = filterCertainCharacters(t.taskStmt);
                    taskParts.add(part);
/*
                    System.out.println("****************************** " + t.taskNum + ": BEGIN TASK STATEMENT:");
                    System.out.println(part);
                    System.out.println("****************************** " + t.taskNum + ": END TASK STATEMENT");
*/
                }
                if (t.taskNarr != null) {
                    String part = filterCertainCharacters(t.taskNarr);
                    taskParts.add(part);
/*
                    System.out.println("****************************** " + t.taskNum + ": BEGIN TASK NARRATIVE:");
                    System.out.println(part);
                    System.out.println("****************************** " + t.taskNum + ": END TASK NARRATIVE");
*/
                }
            }
        }

        /* Translate taskParts (but we also keep a non-translated version for debugging purposes) */
        /*
         * Convert the translated and non-translated lists of task elements
         * into strings wrapped in #combine operators. No #sdm's are used on these
         * strings of words, which are sometimes long
         */
        String taskPartsString = "";
        String nonTranslatedTaskPartsString = "";
        if (hasTaskParts) {
            StringBuilder nonTranslatedTaskPartsStringBuilder = new StringBuilder();
            for (String phrase : taskParts) {
                nonTranslatedTaskPartsStringBuilder.append(phrase).append(" ");
            }
            nonTranslatedTaskPartsString = nonTranslatedTaskPartsStringBuilder.toString();
            nonTranslatedTaskPartsString = "#combine(" + nonTranslatedTaskPartsString + ") ";
            List<String> translatedTaskParts;
            if (!targetLanguageIsEnglish) {
                translatedTaskParts = translator.getTranslations(taskParts);
                StringBuilder taskPartsStringBuilder = new StringBuilder();
                for (String phrase : translatedTaskParts) {
                    taskPartsStringBuilder.append(filterCertainCharactersPostTranslation(phrase)).append(" ");
                }
                taskPartsString = taskPartsStringBuilder.toString();
                taskPartsString = "#combine(" + taskPartsString + ") ";
            } else {
                taskPartsString = nonTranslatedTaskPartsString;
            }
        }

        /*
         * Construct the final Galago queries, translated and non-translated versions,
         * from the noun phrases and task elements. Use the #combine's weights to heavily
         * emphasize the task elements (title, narrative and statement)
         */
        String finalString;
        String nonTranslatedFinalString;

        /* .8 vs .2 in the following was set based on benchmarks run on the ARABIC and FARSI eval data */
        if (useTaskParts && hasTaskParts) {
            finalString = "#combine:0=0.8:1=0.2 (" + nounPhrasesString + " "
                    + taskPartsString + ")";
            nonTranslatedFinalString = "#combine:0=0.8:1=0.2 (" + nonTranslatedNounPhrasesString + " "
                    + nonTranslatedTaskPartsString + ")";
        } else {
            finalString = "#combine (" + nounPhrasesString + ")";
            nonTranslatedFinalString = "#combine (" + nonTranslatedNounPhrasesString + ")";
        }

/*
        System.out.println("****************************** " + t.taskNum + ": BEGIN QUERY:");
        System.out.println(nonTranslatedFinalString);
        System.out.println("****************************** " + t.taskNum + ": END QUERY");
*/

        setQuery(t.taskNum, finalString);
        setNonTranslatedQuery(t.taskNum, nonTranslatedFinalString);
    }

    private void addNounPhrases(Map<String, Integer> soFar, String extr, String header, String taskNum) {
/*
        System.out.println("****************************** " + taskNum + ": BEGIN GET NOUN PHRASES for " + header);
        System.out.println("****************************** " + taskNum + ": BEGIN TEXT:");
        System.out.println(extr);
        System.out.println("****************************** " + taskNum + ": END TEXT:");
*/

        extr = filterCertainCharacters(extr);

        List<String> nouns = spacy.getNounPhrases(extr);

/*
        System.out.println("****************************** " + taskNum + ": BEGIN EXTRACTED NOUN PHRASES:");
*/

        for (String noun_phrase : nouns) {
/*
            System.out.println(noun_phrase);
*/
            if (mode.equals("HITL")) {
                if (stopPhrases.contains(noun_phrase)) {
                    logger.info("Phrase in the stop list--skipping it");
                    continue;
                }
            }
            if (noun_phrase.length() > 2) {  // omit small single words
                if (!soFar.containsKey(noun_phrase)) {
                    soFar.put(noun_phrase, 1);
                } else {
                    int x = soFar.get(noun_phrase);
                    soFar.put(noun_phrase, x + 1);
                }
            }
        }
/*
        System.out.println("****************************** " + taskNum + ": END EXTRACTED NOUN PHRASES:");
        System.out.println("****************************** " + taskNum + ": END GET NOUN PHRASES for " + header);
*/
    }

    private boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    private Task findTask(String taskNum) {
        for (Task t : tasks) {
            if (taskNum.equals(t.taskNum))
                return t;
        }
        return null;
    }

    /**
     * Processes the analytic tasks file: generates queries for the Tasks and Requests,
     * executes the queries, annotates hits with events.
     */
    private void processTaskQueries(String analyticTasksFile, String mode, String outputQueryFileName,
                         boolean targetLanguageIsEnglish, String programDirectory, String phase, String targetLanguage,
                                    String logFileLocation, String queryFileDirectory) {
        logger.info("Starting task-level query construction");
        this.programDirectory = programDirectory;
        this.spacy = new Spacy(programDirectory);
        this.mode = mode;
        this.analyticTasksFile = analyticTasksFile;
        this.targetLanguageIsEnglish = targetLanguageIsEnglish;
        this.outputQueryFileName = outputQueryFileName;
        this.logFileLocation = logFileLocation;
        this.queryFileDirectory = queryFileDirectory;

        readTaskFile();

        np = new AnnotatedNounPhrases(spacy);
        annotatedSentences = new AnnotatedSentences();

        stopPhrases = new HashSet<>();
        if (mode.equals("HITL")) {
            logger.info("Building stop phrase list");
            for (Task t : tasks) {
                // use the annotated noun phrase file that is in the Docker's home directory
                // unless there exists a file with the same name in the queryFiles directory (override file)
                String annotatedNounPhraseFileName = queryFileDirectory + t.taskNum + ".annotated_task_level_noun_phrases.json";
                try {
                    stopPhrases.addAll(np.getStopPhrases(annotatedNounPhraseFileName, t));
                } catch (Exception e) {
                    throw new BetterQueryBuilderException(e);
                }
            }
            logger.info("STOP PHRASES LIST:");
            for (String phrase : stopPhrases) {
                logger.info(phrase);
            }
        }

        buildQueries();
        writeQueryFile();
    }

    private static String ensureTrailingSlash(String s) {
        if (!s.endsWith("/")) {
            return s + "/";
        } else {
            return s;
        }
    }

    private static String getDirectoryFromFileName(String fileName) {
        return new File(fileName).getParent();
    }

    /**
     * Public entry point for this class.
     * ARGS:
     *  analytic tasks info file, full path name
     *  mode - AUTO, AUTO-HITL, or HITL
     *  output file full path name (will become a Galago-ready query file)
     *  target corpus language - "en" (English) or "ar" (Arabic)
     *  program directory - full path of the directory where programs needed by this program will be
     *    should end with a "/"
     *  phase - only "Task" is supported in this query builder
     */
    public static void main (String[] args) {
        for (int x = 0; x < args.length; ++x) {
            System.out.println(x + ": " + args[x]);
        }
        String analyticTasksFile = args[0];
        String mode = args[1];
        String outputQueryFileName = args[2] + ".queries.json";
        boolean targetLanguageIsEnglish = (args[3].equals("ENGLISH"));
        String programDirectory = args[4];
        programDirectory = ensureTrailingSlash(programDirectory);
        String phase = args[5];
        String targetLanguage = args[3];
        String englishIndexLocation = args[6];
        String logFileLocation = args[7];
        logFileLocation = ensureTrailingSlash(logFileLocation);
        String galagoLocation = args[8];
        String qrelFile = args[9];
        String targetIndexLocation = args[10];

        BetterQueryBuilderTaskNounPhrases betterIR = new BetterQueryBuilderTaskNounPhrases();
        betterIR.setupLogging(logFileLocation);

        if (targetLanguage.equals("ARABIC") || targetLanguage.equals("FARSI")) {
//            betterIR.setTranslator(new MarianTranslator(programDirectory, targetLanguage));
            betterIR.setTranslator(new TableTranslator(programDirectory, targetLanguage));
        } else if (targetLanguage.equals("ENGLISH")) {
            System.out.println("TARGET LANGUAGE IS ENGLISH");
        } else {
            System.out.println("Unsupported language: " + targetLanguage);
            System.exit(-1);
        }

        String queryFileDirectory = getDirectoryFromFileName(outputQueryFileName);
        queryFileDirectory = ensureTrailingSlash(queryFileDirectory);

        if (phase.equals("Task")) {
            betterIR.processTaskQueries(analyticTasksFile, mode, outputQueryFileName, targetLanguageIsEnglish,
                    programDirectory, phase, targetLanguage, logFileLocation, queryFileDirectory);
        } else {
            System.out.println("Bad phase: " + phase);
            System.exit(-1);
        }
    }
}
