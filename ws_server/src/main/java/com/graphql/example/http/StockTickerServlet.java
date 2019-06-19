package com.graphql.example.http;

import com.graphql.example.http.util.JsonComposer;
import com.graphql.example.http.util.QueryParameters;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * In Jetty this is how you map a servlet to a websocket per request
 */
public class StockTickerServlet extends WebSocketServlet {

    private static final long WEBSOCKET_IDLE_TIMEOUT = 3 * 60 * 1000;

    private static final Logger log = LoggerFactory.getLogger(StockTickerServlet.class);
    private static GraphQL graphQL;

    static GraphQL getGraphQL() {
        if (graphQL == null) {
            final StockTickerGraphqlPublisher graphqlPublisher = new StockTickerGraphqlPublisher();

            Instrumentation instrumentation = new ChainedInstrumentation(
                    singletonList(new TracingInstrumentation())
            );

            //
            // In order to have subscriptions in graphql-java you MUST use the
            // SubscriptionExecutionStrategy strategy.
            //
            graphQL = GraphQL
                    .newGraphQL(graphqlPublisher.getGraphQLSchema())
                    .instrumentation(instrumentation)
                    .build();
        }

        return graphQL;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setMaxTextMessageBufferSize(1024 * 1024);
        factory.getPolicy().setIdleTimeout(WEBSOCKET_IDLE_TIMEOUT);
        factory.register(StockTickerWebSocket.class);
    }

    private String executeQuery(String query) {
        QueryParameters parameters = QueryParameters.from(query);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(parameters.getQuery())
                .variables(parameters.getVariables())
                .operationName(parameters.getOperationName())
                .build();

        Object data = getGraphQL().execute(executionInput).getData();
        if (data != null) {
            final HashMap<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("data", data);
            try {
                return JsonComposer.compose(jsonMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        final PrintWriter out = resp.getWriter();
        out.println("<p>Hello Get!</p>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String body = req.getReader().lines().collect(Collectors.joining("\n"));
        log.info("Post received with content " + body);
        try {
            final String response = executeQuery(body);
            if (response == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
            else {
                resp.setContentType("application/json");
                final PrintWriter out = resp.getWriter();
                out.print(response);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}


