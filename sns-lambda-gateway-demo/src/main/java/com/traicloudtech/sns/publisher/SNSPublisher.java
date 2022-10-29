package com.traicloudtech.sns.publisher;


import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * Handler for requests to Lambda function.
 */
public class SNSPublisher implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        final String topicARN = System.getenv("TOPIC_ARN");
        String requestBody = input.getBody();
        LambdaLogger logger = context.getLogger();
        logger.log("Original json body: " + requestBody);

        JsonObject messageDetail = JsonParser.parseString(requestBody).getAsJsonObject();

        String sender = messageDetail.get("sender").getAsString();
        String subject = messageDetail.get("subject").getAsString();
        String message = messageDetail.get("message").getAsString();

        JsonObject messageJson = new JsonObject();
        messageJson.addProperty("sender", sender);
        messageJson.addProperty("subject", subject);
        messageJson.addProperty("message", message);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        try {

            SnsClient snsClient = SnsClient.builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build();


            PublishRequest request = PublishRequest.builder().message(new Gson().toJson(messageJson, JsonObject.class))
                    .topicArn(topicARN)
                    .build();

            PublishResponse result = snsClient.publish(request);
            logger.log(result.messageId() + " Message sent. Status is " +  result.sdkHttpResponse().statusCode());

            String output = String.format("{ \"message\": \"%s\" }", result.messageId() );

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (SnsException e) {
            logger.log(e.getMessage());
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }
}
