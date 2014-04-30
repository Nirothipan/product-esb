/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.esb.jms.transport.test;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.extensions.servers.httpserver.RequestInterceptor;
import org.wso2.carbon.automation.extensions.servers.httpserver.SimpleHttpServer;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.JMSEndpointManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * https://wso2.org/jira/browse/ESBJAVA-1153
 */
public class SpecialCharacterTestCase extends ESBIntegrationTest {


    private TestRequestInterceptor interceptor = new TestRequestInterceptor();
    private SimpleHttpServer httpServer;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        httpServer = new SimpleHttpServer();
        try {
            httpServer.start();
            Thread.sleep(5000);
        } catch (IOException e) {
            log.error("Error while starting the HTTP server", e);
        }

        interceptor = new TestRequestInterceptor();
        httpServer.getRequestHandler().setInterceptor(interceptor);


        super.init();

        updateESBConfiguration(JMSEndpointManager.setConfigurations(
                esbUtils.loadResource(File.separator + "artifacts" + File.separator + "ESB"
                                      + File.separator + "synapseconfig" + File.separator
                                      + "messageStore" + File.separator + "special_character.xml")));

    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = {"wso2.esb"})
    public void testSpecialCharacterMediation() throws Exception {
//        SimpleHttpClient httpClient = new SimpleHttpClient();
        String payload = "<test>This payload is üsed to check special character mediation</test>";
        try {
            Map<String, String> header = new HashMap<String, String>();
            header.put("Content-Type", "application/xml");
//            HttpResponse response = httpClient.doPost(getProxyServiceURLHttp("InOutProxy"), null, payload, "application/xml");
            HttpRequestUtil.doPost(new URL(getProxyServiceURLHttp("InOutProxy")), payload, header);
        } catch (AxisFault e) {
            log.error("Response not expected here, Exception can be accepted ");
        }
        Thread.sleep(10000);
        assertTrue(interceptor.getPayload().contains(payload));
    }

    private static class TestRequestInterceptor implements RequestInterceptor {

        private String payload;

        public void requestReceived(HttpRequest request) {
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                try {
                    InputStream in = entity.getContent();
                    String inputString = IOUtils.toString(in, "UTF-8");
                    payload = inputString;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public String getPayload() {
            return payload;
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        try {
            super.cleanup();
            Thread.sleep(10000);
        } finally {

            try {
                httpServer.stop();
            } catch (Exception e) {
                log.warn("Error while shutting down the HTTP server", e);
            }
        }
    }
}
