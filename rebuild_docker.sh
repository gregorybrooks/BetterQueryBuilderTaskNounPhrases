set -v
docker rmi gregorybrooks/better-query-builder-task-noun-phrases:2.1.0
docker build -t gregorybrooks/better-query-builder-task-noun-phrases:2.1.0 .
docker push gregorybrooks/better-query-builder-task-noun-phrases:2.1.0
