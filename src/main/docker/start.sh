#!/usr/bin/env sh

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

[ -z "$SIDECAR_PORT" ] && SIDECAR_PORT="8080"
[ -z "$SIDECAR_HOST" ] && SIDECAR_HOST="sidecar"

[ -z "$KAFKA_HOST" ] && KAFKA_HOST="kafka"
[ -z "$KAFKA_PORT" ] && KAFKA_PORT="9092"

[ -z "$APP_ID" ] && APP_ID="hydro-serving-kafka"
[ -z "$APP_PORT" ] && APP_PORT="9091"

JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"
APP_OPTS=""

echo "Running Manager with:"
echo "JAVA_OPTS=$JAVA_OPTS"

if [ "$CUSTOM_CONFIG" = "" ]
then
    echo "Custom config does not exist"
    APP_OPTS="$APP_OPTS -Dsidecar.port=$SIDECAR_PORT -Dsidecar.host=$SIDECAR_HOST"
    APP_OPTS="$APP_OPTS -Dkafka.advertised-host=$KAFKA_HOST -Dkafka.advertised-port=$KAFKA_PORT"
    APP_OPTS="$APP_OPTS -Dapplication.app-id=$APP_ID"
    APP_OPTS="$APP_OPTS -Dgrpc.port=$APP_PORT"

    echo "APP_OPTS=$APP_OPTS"

else
   APP_OPTS="$APP_OPTS -Dconfig.file=$CUSTOM_CONFIG"
   echo "with config file config.file=$CUSTOM_CONFIG"
fi


java $JAVA_OPTS $APP_OPTS -cp "/app/app.jar:/app/lib/*" io.hydrosphere.serving.kafka.KafkaStreamApp