package com.statemachine.listener;

import com.statemachine.listener.event.MachineStateChangeEvent;
import com.statemachine.service.MachineDefinitionService;
import com.statemachine.service.MachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MachineStateChangeListenerTest {

    @Autowired
    private MachineService machineService;

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    @Autowired
    private MachineStateChangeListenerRegistry listenerRegistry;

    private String machineDefinitionId = "first";
    private TestListener testListener;

    @BeforeEach
    void setUp() throws Exception {
        // Load machine definition
        machineDefinitionService.loadFromXml("first.xml");
        
        // Create and register a test listener
        testListener = new TestListener();
        listenerRegistry.registerListener(testListener);
    }

    @Test
    void testListenerReceivesStateChangeEvent() throws Exception {
        // Create a machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "test");
        
        var machine = machineService.createMachineInstance(
                machineDefinitionId, initialContext, null, null);
        
        // Wait a bit for async notification
        assertTrue(testListener.awaitEvent(2, TimeUnit.SECONDS), 
                "Should receive initial state event");
        
        assertNotNull(testListener.lastEvent);
        assertEquals(machine.getId(), testListener.lastEvent.getMachineId());
        assertEquals(MachineStateChangeEvent.EventType.INITIAL, 
                testListener.lastEvent.getEventType());
        
        // Execute a transition
        testListener.reset();
        machineService.executeTransition(machine.getId(), "t0");
        
        // Wait for state change event
        assertTrue(testListener.awaitEvent(2, TimeUnit.SECONDS), 
                "Should receive state change event");
        
        assertNotNull(testListener.lastEvent);
        assertEquals(MachineStateChangeEvent.EventType.STATE_CHANGED, 
                testListener.lastEvent.getEventType());
        assertEquals("start", testListener.lastEvent.getFromStateId());
        assertEquals("one", testListener.lastEvent.getToStateId());
        assertEquals("t0", testListener.lastEvent.getTransitionId());
    }

    @Test
    void testListenerReceivesContextUpdateEvent() throws Exception {
        // Create a machine instance
        Map<String, Object> initialContext = new HashMap<>();
        var machine = machineService.createMachineInstance(
                machineDefinitionId, initialContext, null, null);
        
        // Clear initial event
        testListener.reset();
        
        // Update context
        Map<String, Object> contextUpdate = new HashMap<>();
        contextUpdate.put("key", "value");
        machineService.updateContext(machine.getId(), contextUpdate, null);
        
        // Wait for context update event
        assertTrue(testListener.awaitEvent(2, TimeUnit.SECONDS), 
                "Should receive context update event");
        
        assertNotNull(testListener.lastEvent);
        assertEquals(MachineStateChangeEvent.EventType.CONTEXT_UPDATE, 
                testListener.lastEvent.getEventType());
    }

    @Test
    void testListenerUnregistration() throws Exception {
        int initialCount = listenerRegistry.getListenerCount();
        
        // Unregister the test listener
        boolean removed = listenerRegistry.unregisterListener(testListener);
        assertTrue(removed);
        assertEquals(initialCount - 1, listenerRegistry.getListenerCount());
        
        // Create a machine - listener should not receive event
        testListener.reset();
        Map<String, Object> initialContext = new HashMap<>();
        machineService.createMachineInstance(machineDefinitionId, initialContext, null, null);
        
        // Wait a bit - should not receive event
        assertFalse(testListener.awaitEvent(1, TimeUnit.SECONDS), 
                "Unregistered listener should not receive events");
    }

    /**
     * Test listener implementation for testing purposes.
     */
    private static class TestListener implements MachineStateChangeListener {
        private MachineStateChangeEvent lastEvent;
        private volatile CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onStateChange(MachineStateChangeEvent event) {
            this.lastEvent = event;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public String getListenerId() {
            return "test-listener";
        }

        public boolean awaitEvent(long timeout, TimeUnit unit) throws InterruptedException {
            if (latch != null) {
                boolean result = latch.await(timeout, unit);
                return result;
            }
            return false;
        }

        public void reset() {
            lastEvent = null;
            latch = new CountDownLatch(1);
        }
    }
}
