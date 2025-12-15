package com.statemachine.service;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class ConditionEvaluator {

    public boolean evaluate(String conditionExpression, Map<String, Object> context) {
        if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
            return true; // No condition means always true
        }

        // Normalize the expression: convert SpEL-style operators to JavaScript operators
        String normalizedExpression = normalizeExpression(conditionExpression);
        
        // Convert context values: convert numeric strings to numbers for proper comparison
        Map<String, Object> convertedContext = convertContext(context);
        
        // Create a GraalVM Polyglot context for JavaScript evaluation
        // Suppress JVMCI warning - interpreter mode works fine for condition evaluation
        try (Context polyglotContext = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {
            // Create a JavaScript object from the context map
            Value bindings = polyglotContext.getBindings("js");
            
            // Inject all context variables as global variables for direct access
            // This allows expressions like: name == "John" or name == 'John'
            // Convert Java Lists to JavaScript arrays so array methods (some, filter, etc.) work
            for (Map.Entry<String, Object> entry : convertedContext.entrySet()) {
                try {
                    Value jsValue = convertToJavaScriptValue(polyglotContext, entry.getValue());
                    bindings.putMember(entry.getKey(), jsValue);
                } catch (Exception e) {
                    throw new RuntimeException("Error converting context variable '" + entry.getKey() + 
                        "' (type: " + (entry.getValue() != null ? entry.getValue().getClass().getName() : "null") + 
                        ") to JavaScript value: " + e.getMessage(), e);
                }
            }

            // Evaluate the expression as JavaScript
            Value result = polyglotContext.eval("js", normalizedExpression);

            // Handle boolean result
            if (result.isBoolean()) {
                return result.asBoolean();
            }
            
            // If result is not boolean, try to convert
            return Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            // Log the context for debugging
            System.err.println("Error evaluating condition: " + conditionExpression);
            System.err.println("Context: " + context);
            System.err.println("Normalized expression: " + normalizeExpression(conditionExpression));
            e.printStackTrace();
            throw new RuntimeException("Error evaluating condition: " + conditionExpression + ". " + 
                "Context keys: " + (context != null ? context.keySet() : "null") + 
                ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes the condition expression to JavaScript syntax:
     * - Converts 'and' to '&&'
     * - Converts 'or' to '||'
     * - Converts 'not' to '!'
     * - Handles both single and double quotes for strings
     */
    private String normalizeExpression(String expression) {
        if (expression == null) {
            return null;
        }
        
        // Replace logical operators (using word boundaries to avoid partial matches)
        String normalized = expression
            .replaceAll("\\band\\b", "&&")
            .replaceAll("\\bor\\b", "||")
            .replaceAll("\\bnot\\b", "!");
        
        return normalized;
    }

    /**
     * Converts the context map, converting numeric strings to numbers for proper comparison.
     * This allows expressions like "age > 18" to work when age is stored as "25".
     * Complex objects and arrays are passed through as-is since they're already deserialized from JSON.
     */
    private Map<String, Object> convertContext(Map<String, Object> context) {
        Map<String, Object> converted = new HashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();

            // Special-case: if "users" is provided as a single object instead of an array,
            // wrap it into a list so JS array methods like some() work.
            if ("users".equals(entry.getKey()) && value instanceof Map) {
                List<Object> usersList = new ArrayList<>();
                usersList.add(value);
                value = usersList;
            }

            Object convertedValue = convertValue(value);
            converted.put(entry.getKey(), convertedValue);
        }
        return converted;
    }

    /**
     * Converts a value to the appropriate type:
     * - String numbers are converted to Long or Double
     * - Complex objects and arrays are returned as-is
     * - Other values are returned as-is
     */
    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // If it's already a number, boolean, or complex object, return as-is
        if (value instanceof Number || value instanceof Boolean || 
            value instanceof Map || value instanceof List || value.getClass().isArray()) {
            return value;
        }
        
        // If it's a string, try to convert to number
        if (value instanceof String) {
            String strValue = (String) value;
            
            // Try to parse as integer
            try {
                // Check if it's a valid integer (no decimal point)
                if (!strValue.contains(".") && !strValue.contains("e") && !strValue.contains("E")) {
                    return Long.parseLong(strValue);
                }
            } catch (NumberFormatException e) {
                // Not an integer, continue
            }
            
            // Try to parse as double
            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                // Not a number, return as string
                return strValue;
            }
        }
        
        // Return as-is for any other type
        return value;
    }

    /**
     * Converts a Java object to a JavaScript value, ensuring Lists become JavaScript arrays
     * so array methods like some(), filter(), map(), etc. work properly.
     */
    private Value convertToJavaScriptValue(Context polyglotContext, Object value) {
        if (value == null) {
            return polyglotContext.eval("js", "null");
        }
        
        // Convert Java Lists to JavaScript arrays
        // Handle both java.util.List and any Collection that might be returned from JSON deserialization
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            // Create a JavaScript array and populate it using push
            Value jsArray = polyglotContext.eval("js", "[]");
            for (Object item : list) {
                Value jsItem = convertToJavaScriptValue(polyglotContext, item);
                jsArray.invokeMember("push", jsItem);
            }
            return jsArray;
        }
        
        // Convert Object[] arrays to JavaScript arrays (possible from JSON deserialization)
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            Value jsArray = polyglotContext.eval("js", "[]");
            for (Object item : array) {
                Value jsItem = convertToJavaScriptValue(polyglotContext, item);
                jsArray.invokeMember("push", jsItem);
            }
            return jsArray;
        }

        // Handle arrays (Object[]) that might come from JSON deserialization
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            Value jsArray = polyglotContext.eval("js", "[]");
            for (Object item : array) {
                Value jsItem = convertToJavaScriptValue(polyglotContext, item);
                jsArray.invokeMember("push", jsItem);
            }
            return jsArray;
        }
        
        // Convert Java Maps to JavaScript objects
        // Handle both java.util.Map and any Map implementation (including LinkedHashMap from JSON deserialization)
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            Value jsObject = polyglotContext.eval("js", "({})");
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Value jsValue = convertToJavaScriptValue(polyglotContext, entry.getValue());
                jsObject.putMember(entry.getKey(), jsValue);
            }
            return jsObject;
        }
        
        // For other types, use asValue which handles primitives and strings
        return polyglotContext.asValue(value);
    }
}

