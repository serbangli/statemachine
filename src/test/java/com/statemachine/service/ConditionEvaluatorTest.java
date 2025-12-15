package com.statemachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
    }

    @Test
    void testEvaluateSimpleEquality() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");

        boolean result = evaluator.evaluate("name == 'john'", context);
        assertTrue(result);
    }

    @Test
    void testEvaluateSimpleInequality() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "jane");

        boolean result = evaluator.evaluate("name == 'john'", context);
        assertFalse(result);
    }

    @Test
    void testEvaluateNumericComparison() {
        Map<String, Object> context = new HashMap<>();
        context.put("age", "25");

        boolean result = evaluator.evaluate("age > 18", context);
        assertTrue(result);
    }

    @Test
    void testEvaluateNullCondition() {
        Map<String, Object> context = new HashMap<>();
        
        // Null or empty condition should return true
        assertTrue(evaluator.evaluate(null, context));
        assertTrue(evaluator.evaluate("", context));
        assertTrue(evaluator.evaluate("   ", context));
    }

    @Test
    void testEvaluateMultipleVariables() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");
        context.put("age", "30");
        context.put("status", "active");

        boolean result = evaluator.evaluate("name == 'john' and age > 25", context);
        assertTrue(result);
    }

    @Test
    void testEvaluateComplexCondition() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");
        context.put("age", "30");

        boolean result = evaluator.evaluate("(name == 'john' or name == 'jane') and age > 18", context);
        assertTrue(result);
    }

    @Test
    void testEvaluateInvalidExpression() {
        Map<String, Object> context = new HashMap<>();
        
        assertThrows(RuntimeException.class, () -> {
            evaluator.evaluate("invalid expression syntax", context);
        });
    }

    @Test
    void testEvaluateArrayConditionWithSome() {
        Map<String, Object> context = new HashMap<>();
        // Create a list of maps representing users
        java.util.List<Map<String, Object>> users = new java.util.ArrayList<>();
        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "john");
        user1.put("role", "editor");
        users.add(user1);
        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "anna");
        user2.put("role", "reviewer");
        users.add(user2);
        context.put("users", users);

        // Test array.some() condition - should find john
        boolean result = evaluator.evaluate("users.some(u => u.name === 'john')", context);
        assertTrue(result, "Should find user with name 'john'");

        // Test negative case - should not find bob
        boolean result2 = evaluator.evaluate("users.some(u => u.name === 'bob')", context);
        assertFalse(result2, "Should not find user with name 'bob'");

        // Test with role condition
        boolean result3 = evaluator.evaluate("users.some(u => u.role === 'editor')", context);
        assertTrue(result3, "Should find user with role 'editor'");
         // Test with role condition
         boolean result4= evaluator.evaluate("users.length === 3", context);
         assertTrue(result4, "Should find 2 users");
    }
}

