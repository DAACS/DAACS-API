package com.daacs.service.hystrix.http;


import com.lambdista.util.Try;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.http.HttpStatus;

import java.nio.charset.Charset;
import java.util.List;


/**
 * Created by chostetter on 4/6/17.
 */
public class CanvasUpdateSubmissionHystrixCommand extends HttpHystrixCommand<String> {

    private HttpPut httpPut;

    public CanvasUpdateSubmissionHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, String baseUrl, String oAuthToken, Integer courseId, Integer assignmentId, String sisId, List<NameValuePair> params) {
        super(hystrixGroupKey, hystrixCommandKey, buildUrl(baseUrl, courseId, assignmentId, sisId));

        httpPut = new HttpPut(url);
        httpPut.setEntity(new UrlEncodedFormEntity(params, Charset.forName("UTF-8")));
        httpPut.setHeader(new BasicHeader("Authorization", "Bearer " + oAuthToken));
        httpPut.setHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
    }

    @Override
    protected Try<String> run() throws Exception {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(4);

        HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
        HttpClient httpClient = builder.build();

        try {
            HttpResponse httpResponse = httpClient.execute(httpPut);

            String responseBody = IOUtils.toString(httpResponse.getEntity().getContent(), Charset.forName("UTF-8"));
            HttpStatus status = HttpStatus.valueOf(httpResponse.getStatusLine().getStatusCode());
            if(status != HttpStatus.OK){
                return failedHttpFallback(status, responseBody);
            }

            return createSuccess(responseBody);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected String getResourceName() {
        return "canvas";
    }

    private static String buildUrl(String baseUrl, Integer courseId, Integer assignmentId, String sisId){
        return baseUrl + "/courses/" + String.valueOf(courseId) + "/assignments/" + String.valueOf(assignmentId) + "/submissions/sis_user_id:" + sisId;
    }
}