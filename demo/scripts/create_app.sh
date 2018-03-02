#!/bin/bash

modelId=$(curl -X GET --header 'Accept: application/json' 'http://localhost:8080/api/v1/model' | jq -c '.[].model | select(.name | contains("kek"))| .id')

startModelId=$(curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ 
   "modelId": '"$modelId"' 
 }' 'http://localhost:8080/api/v1/model/build' | jq '.id')

runtimeId=$(curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{  
   "name": "hydrosphere/serving-runtime-spark",  
   "version": "2.1-latest",  
   "modelTypes": [  
     "spark:2.1" 
   ], 
   "tags": [ 
     "string"  
   ], 
   "configParams": {}  
 }' 'http://localhost:8080/api/v1/runtime' | jq '.id')


curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ 
   "name": "Test_4", 
   "executionGraph": { 
     "stages": [ 
       { 
         "services": [  
           {  
             "serviceDescription": {  
               "runtimeId": '"$runtimeId"',  
               "modelVersionId": '"$startModelId"'  
             },  
             "weight": 100  
           }  
         ],  
         "signatureName": "default_spark"  
       }
     ]  
   },  
   "kafkaStreaming": [  
     {  
       "sourceTopic": "test",  
       "destinationTopic": "success",  
       "consumerId": "test_1",  
       "errorTopic": "failure"  
     }  
   ]  
 }' 'http://localhost:8080/api/v1/applications'