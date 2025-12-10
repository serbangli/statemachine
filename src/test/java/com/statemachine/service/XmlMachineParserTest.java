package com.statemachine.service;

import com.statemachine.domain.model.MachineDefinition;
import com.statemachine.domain.model.Transition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class XmlMachineParserTest {

    private XmlMachineParser parser;

    @BeforeEach
    void setUp() {
        parser = new XmlMachineParser();
    }

    @Test
    void testParseMachineDefinition() throws Exception {
        String xml = """
            <machine id="test-machine">
                <start id="start" name="start"/>
                <state id="state1" name="State One"/>
                <state id="state2" name="State Two"/>
                <end id="end" name="end"/>
                <transition id="t1" name="Transition 1" source="start" destination="state1"/>
                <transition id="t2" name="Transition 2" source="state1" destination="state2">
                    <condition>
                        name == 'john'
                    </condition>
                </transition>
                <transition id="t3" name="Transition 3" source="state2" destination="end"/>
            </machine>
            """;

        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        MachineDefinition definition = parser.parse(inputStream);

        assertNotNull(definition);
        assertEquals("test-machine", definition.getId());
        assertNotNull(definition.getStartState());
        assertEquals("start", definition.getStartState().getId());
        assertNotNull(definition.getEndState());
        assertEquals("end", definition.getEndState().getId());
        assertEquals(4, definition.getStates().size());
        assertEquals(3, definition.getTransitions().size());
    }

    @Test
    void testParseTransitionWithCondition() throws Exception {
        String xml = """
            <machine id="test">
                <start id="start" name="start"/>
                <state id="s1" name="s1"/>
                <end id="end" name="end"/>
                <transition id="t1" name="t1" source="start" destination="s1">
                    <condition>
                        age > 18
                    </condition>
                </transition>
            </machine>
            """;

        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        MachineDefinition definition = parser.parse(inputStream);

        Transition transition = definition.getTransitions().get(0);
        assertNotNull(transition.getCondition());
        assertEquals("age > 18", transition.getCondition().getExpression().trim());
    }

    @Test
    void testParseTransitionWithSurceTypo() throws Exception {
        // Test that parser handles "surce" typo
        String xml = """
            <machine id="test">
                <start id="start" name="start"/>
                <state id="s1" name="s1"/>
                <end id="end" name="end"/>
                <transition id="t1" name="t1" surce="start" destination="s1"/>
            </machine>
            """;

        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        MachineDefinition definition = parser.parse(inputStream);

        Transition transition = definition.getTransitions().get(0);
        assertEquals("start", transition.getSourceStateId());
        assertEquals("s1", transition.getDestinationStateId());
    }
}

