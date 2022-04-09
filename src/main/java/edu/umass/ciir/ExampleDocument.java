package edu.umass.ciir;

import java.util.ArrayList;
import java.util.List;

public class ExampleDocument {
    private String docid;
    private String docText;
    private String highlight;
    private List<SentenceRange> sentences;

    ExampleDocument(String docid, String docText, String highlight, List<SentenceRange> sentences) {
        this.highlight = highlight;
        this.docid = docid;
        this.docText = docText;
        this.sentences = new ArrayList<>(sentences);
    }

    ExampleDocument(ExampleDocument other) {
        this.docid = other.docid;
        this.docText = other.docText;
        this.highlight = other.highlight;
        this.sentences = new ArrayList<>(other.sentences);
    }

    public String getDocid() {
        return docid;
    }

    public String getDocText() {
        return docText;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public void setDocText(String docText) {
        this.docText = docText;
    }

    public void setSentences(List<SentenceRange> sentences) {
        this.sentences = sentences;
    }

    public List<SentenceRange> getSentences() {
        return sentences;
    }
}

