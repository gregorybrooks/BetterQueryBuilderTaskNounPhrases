set -v
sudo docker rmi gregorybrooks/better-query-builder-task-noun-phrases:3.1.0
sudo docker build -t gregorybrooks/better-query-builder-task-noun-phrases:3.1.0 .
sudo docker push gregorybrooks/better-query-builder-task-noun-phrases:3.1.0
