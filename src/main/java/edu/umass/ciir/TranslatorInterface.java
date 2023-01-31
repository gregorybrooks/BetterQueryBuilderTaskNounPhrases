package edu.umass.ciir;

import java.util.List;

public interface TranslatorInterface {
    List<String> getTranslations(List<String> strings);
    List<String> getTranslationTerms(List<String> strings);
    List<String> getSingleTranslationTerm(List<String> strings);
    String getTargetLanguage();
    String getTranslation(String text);
}
