package com.graphql.example.http;

import graphql.schema.GraphQLSchema;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * An very simple example of serving a qraphql schema over http and using websockets for subscriptions.
 * <p>
 * More info can be found here : http://graphql.org/learn/serving-over-http/
 */
public class HttpMain {

    private static final String SERVLET_HOLDER_NAME = "graphql-ws";
    private static final String PATH = "/graphql";
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        //
        // This example uses Jetty as an embedded HTTP server
        Server server = new Server(PORT);

        //
        // In Jetty, handlers are how your get called back on a request
        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");

        ServletHolder stockTicker = new ServletHolder(SERVLET_HOLDER_NAME, StockTickerServlet.class);
        servletContextHandler.addServlet(stockTicker, PATH);

        server.setHandler(servletContextHandler);
        server.start();
        server.join();
    }
}
