package com.graphql.example.http.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Graphql clients can send GET or POST HTTP requests.  The spec does not make an explicit
 * distinction.  So you may need to handle both.  The following was tested using
 * a graphiql client tool found here : https://github.com/skevy/graphiql-app
 *
 * You should consider bundling graphiql in your application
 *
 * https://github.com/graphql/graphiql
 *
 * This outlines more information on how to handle parameters over http
 *
 * http://graphql.org/learn/serving-over-http/
 */
public class QueryParameters {

    private final String query;
    private final String operationName;
    private final Map<String, Object> variables;

    private QueryParameters(String query, String operationName, Map<String, Object> variables) {
        this.query = query;
        this.operationName = operationName;
        this.variables = variables;
    }

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    private static Map<String, Object> getVariables(Object variables) {
        if (variables instanceof Map) {
            Map<?, ?> inputVars = (Map) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.valueOf(k), v));
            return vars;
        }
        return JsonKit.toMap(String.valueOf(variables));
    }

    static QueryParameters from(Map<String, Object> json) {
        if (json != null) {
            final String query = (String) json.get("query");
            final String operationName = (String) json.get("operationName");
            final Map<String, Object> variables = getVariables(json.get("variables"));
            return new QueryParameters(query, operationName, variables);
        }

        return null;
    }

    public static QueryParameters from(String queryMessage) {
        return from(JsonKit.toMap(queryMessage));
    }
}
