def repository = 'hydro-serving-kafka-gateway'


def buildFunction={
    def curVersion = getVersion()
    sh "sbt -DappVersion=${curVersion} compile docker"


    //Test
    sh "sbt -DappVersion=${curVersion} test"
}

def collectTestResults = {
    junit testResults: '**/target/test-reports/io.hydrosphere*.xml', allowEmptyResults: true
}

pipelineCommon(
        repository,
        false, //needSonarQualityGate,
        ["hydrosphere/serving-gateway-kafka"],
        collectTestResults,
        buildFunction,
        buildFunction,
        buildFunction,
        null,
        "",
        "",
        {},
        commitToCD("gateway-kafka")
)