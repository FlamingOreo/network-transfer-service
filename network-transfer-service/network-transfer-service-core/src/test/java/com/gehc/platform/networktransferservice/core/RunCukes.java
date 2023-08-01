/*
 * RunCukes.java
 *
 * Copyright (c) 2023 by General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */


package com.gehc.platform.networktransferservice.core;

import io.cucumber.junit.CucumberOptions;
import io.cucumber.junit.Cucumber;

import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/resources/", plugin = {"pretty", "html:target/reports/cucumber/html/index.html",
		"json:target/cucumber.json", "usage:target/usage.jsonx", "junit:target/junit.xml" },
        extraGlue="com.gehc.platform.networktransferservice")
public class RunCukes
{
}
