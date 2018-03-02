docker cp create_topics.sh kafka:/
docker exec kafka bash create_topics.sh
./create_app.sh