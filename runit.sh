set -v
java -jar target/BetterQueryBuilderTaskNounPhrases-3.0.0.jar $eventExtractorFileLocation/$INPUTFILE $MODE $queryFileLocation/$QUERYFILE $OUT_LANG $HOME/programfiles $PHASE $englishIndexLocation $logFileLocation $galagoLocation $qrelFile $targetIndexLocation $SEARCH_ENGINE
if [ $? -eq 0 ]
then
  set +e
  # The Docker container will run as a different user, so make sure the host user can read your run file:
  chmod -f a+rw $queryFileLocation/${QUERYFILE}*
  # If the chmod had a problem, ignore it
  exit 0
else
  exit $?
fi
