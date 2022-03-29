set -v
java -jar target/BetterQueryBuilderTaskNounPhrases-2.0.0.jar $eventExtractorFileLocation/$INPUTFILE $MODE $queryFileLocation/$QUERYFILE $OUT_LANG $HOME/programfiles $PHASE $englishIndexLocation $logFileLocation $galagoLocation $qrelFile $targetIndexLocation
set +e
chmod -f a+rw $queryFileLocation/${QUERYFILE}*
exit 0
