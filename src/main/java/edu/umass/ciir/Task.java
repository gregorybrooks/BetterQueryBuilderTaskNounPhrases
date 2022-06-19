package edu.umass.ciir;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.*;
import java.util.logging.Logger;

public class Task {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderTaskNounPhrases");
    String taskNum;
    String taskTitle;
    String taskStmt;
    String taskNarr;
    List<Request> requests;
    public List<ExampleDocument> taskExampleDocs;

    Task(String taskNum, String taskTitle, String taskStmt, String taskNarr, List<ExampleDocument> exampleDocuments,
         List<Request> requests) {
        this.taskNum = taskNum;
        this.taskTitle = taskTitle;
        this.taskStmt = taskStmt;
        this.taskNarr = taskNarr;
        this.taskExampleDocs = new ArrayList<>(exampleDocuments);
        this.requests = new ArrayList<>(requests);
    }

    public List<String> getHighlights() {
        List<String> extractions = new ArrayList<>();
        for (ExampleDocument d : taskExampleDocs) {
            extractions.add(d.getHighlight());
        }
        return extractions;
    }


    public List<String> getExampleDocids() {
        List<String> docids = new ArrayList<>();
        for (ExampleDocument d : taskExampleDocs) {
            docids.add(d.getDocid());
        }
        return docids;
    }

   /**
     * Copy constructor (deep copy)
     * @param otherTask The Task to make a copy of.
     */
    Task(Task otherTask) {
        this.taskNum = new String(otherTask.taskNum);
        this.taskTitle = (otherTask.taskTitle == null ? null : new String(otherTask.taskTitle));
        this.taskStmt = (otherTask.taskStmt == null ? null : new String(otherTask.taskStmt));;
        this.taskNarr = (otherTask.taskNarr == null ? null : new String(otherTask.taskNarr));
        this.requests = new ArrayList<>(otherTask.requests);
        this.taskExampleDocs = new ArrayList<>(otherTask.taskExampleDocs);
    }

    public List<Request> getRequests() { return requests; }

}


