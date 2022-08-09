set -v
java -jar target/BetterQueryBuilderTaskNounPhrases-3.0.0.jar $eventExtractorFileLocation/$INPUTFILE $MODE $queryFileLocation/$QUERYFILE $OUT_LANG $HOME/programfiles $PHASE $englishIndexLocation $logFileLocation $galagoLocation $qrelFile $targetIndexLocation $SEARCH_ENGINE
set +e
chmod -f a+rw $queryFileLocation/${QUERYFILE}*
exit 0
