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
import java.util.HashMap;
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

    private final HashMap<String, Subscription> subscriptionMap = new HashMap<>();

    private interface WebSocketProtocolMessageTypes {
        String CONNECTION_ACK = "connection_ack";
        String CONNECTION_INIT = "connection_init";
        String START = "start";
        String STOP = "stop";
    }

    @Override
    public void onWebSocketText(String graphqlQuery) {
        log.info("Text received in " + this + ". Websocket said {}", graphqlQuery);

        WebSocketParameters parameters = WebSocketParameters.from(graphqlQuery);

        if (WebSocketProtocolMessageTypes.CONNECTION_INIT.equals(parameters.getType())) {
            final String resultText = "{\"type\":\"" + WebSocketProtocolMessageTypes.CONNECTION_ACK + "\"}";
            try {
                getRemote().sendString(resultText);
                log.info("  Returned " + resultText);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (WebSocketProtocolMessageTypes.START.equals(parameters.getType())) {
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

                    @Override
                    public void onSubscribe(Subscription s) {
                        log.info("onSubscribe called in " + StockTickerWebSocket.this);
                        subscriptionMap.put(subscriptionId, s);
                        s.request(1);
                    }

                    @Override
                    public void onNext(ExecutionResult executionResult) {
                        log.info("onNext called in " + StockTickerWebSocket.this);
                        final RemoteEndpoint remote = getRemote();
                        final Subscription subs = subscriptionMap.get(subscriptionId);
                        if (remote != null && subs != null) {
                            try {
                                final String resultText = JsonComposer.compose(executionResult.getData());
                                final String wrappedResult = "{\"type\":\"data\",\"id\":\"" + subscriptionId + "\", \"payload\":{\"data\":{\"newNote\":" + resultText + "}}}";
                                log.info("  Returning {} [{}]", wrappedResult, data.getClass().getName());
                                getRemote().sendString(wrappedResult);
                                subs.request(1);
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
        else if (WebSocketProtocolMessageTypes.STOP.equals(parameters.getType())) {
            final String subscriptionId = parameters.getId();
            final Subscription subscription = subscriptionMap.remove(subscriptionId);
            if (subscription != null) {
                log.info("  Canceling subscription for id {}", subscriptionId);
                subscription.cancel();
            }
        }
    }
}
