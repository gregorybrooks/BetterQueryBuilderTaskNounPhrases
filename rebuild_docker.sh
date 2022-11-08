set -v
docker rmi gregorybrooks/better-query-builder-task-noun-phrases:HITL-TEST
docker build -t gregorybrooks/better-query-builder-task-noun-phrases:HITL-TEST .
docker push gregorybrooks/better-query-builder-task-noun-phrases:HITL-TEST
