package edu.umass.ciir;

public class AnnotatedNounPhraseDetail {
    private String sentences;
    private String judgment;

    AnnotatedNounPhraseDetail(String judgment, String sentences) {
        this.judgment = judgment;
        this.sentences = sentences;
    }

    AnnotatedNounPhraseDetail(AnnotatedNounPhraseDetail other) {
        this.judgment = other.judgment;
        this.sentences = other.sentences;
    }

    public String getSentences() {
        return sentences;
    }

    public String getJudgment() {
        return judgment;
    }
}
