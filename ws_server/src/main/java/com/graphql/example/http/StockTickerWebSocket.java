package com.graphql.example.http;

import com.graphql.example.http.util.JsonComposer;
import com.graphql.example.http.util.QueryParameters;
import com.graphql.example.http.util.WebSocketParameters;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A websocket object is created per browser client.  This is the main interface code between the backing
 * publisher of event objects, graphql subscriptions in the middle and responses back to the browser.
 */
public class StockTickerWebSocket extends WebSocketAdapter {

    private static final Logger log = LoggerFactory.getLogger(StockTickerWebSocket.class);

    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

    @Override
    public void onWebSocketConnect(Session session) {
        log.info("Opening web socket in " + this + " with session " + session);
        super.onWebSocketConnect(session);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.info("Closing web socket in " + this + " with statusCode " + statusCode + " and reason " + reason);
        super.onWebSocketClose(statusCode, reason);
        Subscription subscription = subscriptionRef.get();
        if (subscription != null) {
            subscription.cancel();
        }
    }

    @Override
    public void onWebSocketText(String graphqlQuery) {
        log.info("Text received in " + this + ". Websocket said {}", graphqlQuery);

        WebSocketParameters parameters = WebSocketParameters.from(graphqlQuery);

        if ("connection_init".equals(parameters.getType())) {
            final String resultText = "{\"type\":\"connection_ack\"}";
            try {
                getRemote().sendString(resultText);
                log.info("  Returned " + resultText);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if ("start".equals(parameters.getType())) {
            final QueryParameters payload = parameters.getPayload();
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(payload.getQuery())
                    .variables(payload.getVariables())
                    .operationName(payload.getOperationName())
                    .build();

            final Object data = StockTickerServlet.getGraphQL().execute(executionInput).getData();
            final String subscriptionId = parameters.getId();
            if (data instanceof Publisher) {
                final Publisher<ExecutionResult> publisher = (Publisher<ExecutionResult>) data;
                publisher.subscribe(new Subscriber<ExecutionResult>() {

                    private Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription s) {
                        log.info("onSubscribe called in " + StockTickerWebSocket.this);
                        subscription = s;
                        s.request(1);
                    }

                    @Override
                    public void onNext(ExecutionResult executionResult) {
                        log.info("onNext called in " + StockTickerWebSocket.this);
                        final RemoteEndpoint remote = getRemote();
                        if (remote != null) {
                            try {
                                final String resultText = JsonComposer.compose(executionResult.getData());
                                //final String wrappedResult = "{\"type\":\"data\",\"id\":\"" + subscriptionId + "\", \"payload\":" + resultText + "}";
                                //final String wrappedResult = "{\"type\":\"data\",\"id\":\"" + subscriptionId + "\", \"payload\":{\"data\":" + resultText + "}}";
                                final String wrappedResult = "{\"type\":\"data\",\"id\":\"" + subscriptionId + "\", \"payload\":{\"data\":{\"newNote\":" + resultText + "}}}";
                                log.info("  Returning {} [{}]", wrappedResult, data.getClass().getName());
                                getRemote().sendString(wrappedResult);
                                subscription.request(1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.info("onError called in " + StockTickerWebSocket.this);
                    }

                    @Override
                    public void onComplete() {
                        log.info("onComplete called in " + StockTickerWebSocket.this);
                    }
                });
            } else if (data != null) {
                try {
                    final String resultText = JsonComposer.compose(data);
                    log.info("  Returning {} [{}]", resultText, data.getClass().getName());
                    getRemote().sendString(resultText);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
