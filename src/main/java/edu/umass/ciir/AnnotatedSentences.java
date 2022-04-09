package edu.umass.ciir;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class AnnotatedSentences {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderTaskNounPhrases");

    AnnotatedSentences() { }

    public void createToBeAnnotatedSentencesFile(String toBeAnnotatedFilePath, Task t) {
        try {
            logger.info("Writing to-be-annotated-sentences file to " + toBeAnnotatedFilePath);
            JSONObject targetTopMap = new JSONObject();
            targetTopMap.put("taskId", t.taskNum);
            targetTopMap.put("taskTitle", t.taskTitle);
            targetTopMap.put("taskNarrative", t.taskNarr);
            targetTopMap.put("taskStatement", t.taskStmt);
            JSONArray taskExampleDocs = new JSONArray();
            int taskExampleDocIndex = 0;
            for (ExampleDocument e : t.taskExampleDocs) {
                JSONObject exampleDocJSON = new JSONObject();
                exampleDocJSON.put("docId", e.getDocid());
                exampleDocJSON.put("docNumber", ++taskExampleDocIndex);
                JSONArray sentencesJSON = new JSONArray();
                int sentenceIndex = 0;
                for (SentenceRange sentenceRange : e.getSentences()) {
                    JSONObject detail = new JSONObject();
                    detail.put("judgment", "");
                    detail.put("sentence", sentenceRange.text);
                    String sentenceId = t.taskNum + "-td" + taskExampleDocIndex + "-s" + ++sentenceIndex;
                    detail.put("sentenceId", sentenceId);
                    sentencesJSON.add(detail);
                }
                exampleDocJSON.put("sentences", sentencesJSON);
                taskExampleDocs.add(exampleDocJSON);
            }
            targetTopMap.put("exampleDocs", taskExampleDocs);
            JSONArray requestsJSON = new JSONArray();
            int requestIndex = 0;
            for (Request r : t.getRequests()) {
                requestIndex += 1;
                JSONObject requestJSON = new JSONObject();
                requestJSON.put("requestId", r.reqNum);

                requestJSON.put("requestText", r.reqText == null ? "" : r.reqText);
                JSONArray requestExampleDocs = new JSONArray();
                int requestExampleDocIndex = 0;
                for (ExampleDocument e : r.reqExampleDocs) {
                    requestExampleDocIndex += 1;
                    JSONObject exampleDocJSON = new JSONObject();
                    exampleDocJSON.put("docId", e.getDocid());
                    exampleDocJSON.put("docNumber", ++requestExampleDocIndex);
                    JSONArray sentencesJSON = new JSONArray();
                    int sentenceIndex = 0;
                    for (SentenceRange sentenceRange : e.getSentences()) {
                        JSONObject detail = new JSONObject();
                        detail.put("judgment", "");
                        detail.put("sentence", sentenceRange.text);
                        String sentenceId = t.taskNum + "-r" + requestIndex + "-rd" + requestExampleDocIndex + "-s" + ++sentenceIndex;
                        detail.put("sentenceId", sentenceId);
                        sentencesJSON.add(detail);
                    }
                    exampleDocJSON.put("sentences", sentencesJSON);
                    requestExampleDocs.add(exampleDocJSON);
                }
                requestJSON.put("exampleDocs",requestExampleDocs);
                requestsJSON.add(requestJSON);
            }
            targetTopMap.put("requests", requestsJSON);

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(toBeAnnotatedFilePath)));
            writer.write(targetTopMap.toJSONString());
            writer.close();
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }
}
