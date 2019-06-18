package com.graphql.example.http;

import com.graphql.example.http.utill.JsonComposer;
import com.graphql.example.http.utill.QueryParameters;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;

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

        QueryParameters parameters = QueryParameters.from(graphqlQuery);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(parameters.getQuery())
                .variables(parameters.getVariables())
                .operationName(parameters.getOperationName())
                .build();

        final Object data = StockTickerServlet.getGraphQL().execute(executionInput).getData();
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
                    try {
                        final String resultText = JsonComposer.compose(executionResult.getData());
                        log.info("  Returning {} [{}]", resultText, data.getClass().getName());
                        getRemote().sendString(resultText);
                        subscription.request(1);
                    } catch (Exception e) {
                        e.printStackTrace();
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
        }
        else if (data != null) {
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
