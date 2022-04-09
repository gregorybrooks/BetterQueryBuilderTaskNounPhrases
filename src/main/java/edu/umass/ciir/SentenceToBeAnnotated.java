package edu.umass.ciir;

public class SentenceToBeAnnotated {
    private String sentenceId;
    private String sentence;

    SentenceToBeAnnotated(String sentenceId, String sentence) {
        this.sentenceId = sentenceId;
        this.sentence = sentence;
    }

    public String getSentence() {
        return sentence;
    }

    public String getSentenceId() {
        return sentenceId;
    }
}

