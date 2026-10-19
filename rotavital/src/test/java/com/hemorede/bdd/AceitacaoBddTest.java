package com.hemorede.bdd;

import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "com.hemorede.bdd")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty,html:target/cucumber/relatorio.html,json:target/cucumber/relatorio.json")
public class AceitacaoBddTest {
}
