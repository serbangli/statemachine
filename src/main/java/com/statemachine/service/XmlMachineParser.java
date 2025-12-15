package com.statemachine.service;

import com.statemachine.domain.model.Condition;
import com.statemachine.domain.model.MachineDefinition;
import com.statemachine.domain.model.State;
import com.statemachine.domain.model.Transition;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class XmlMachineParser {

    public MachineDefinition parse(InputStream xmlInputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlInputStream);
        document.getDocumentElement().normalize();

        Element machineElement = document.getDocumentElement();
        String machineId = machineElement.getAttribute("id");
        String machineName = machineElement.hasAttribute("name") 
            ? machineElement.getAttribute("name") 
            : machineId;

        MachineDefinition machineDefinition = new MachineDefinition();
        machineDefinition.setId(machineId);
        machineDefinition.setName(machineName);

        // Parse states
        List<State> states = new ArrayList<>();
        State startState = null;
        State endState = null;

        NodeList startNodes = machineElement.getElementsByTagName("start");
        if (startNodes.getLength() > 0) {
            Element startElement = (Element) startNodes.item(0);
            startState = parseState(startElement, State.StateType.START);
            states.add(startState);
            machineDefinition.setStartState(startState);
        }

        NodeList stateNodes = machineElement.getElementsByTagName("state");
        for (int i = 0; i < stateNodes.getLength(); i++) {
            Element stateElement = (Element) stateNodes.item(i);
            State state = parseState(stateElement, State.StateType.REGULAR);
            states.add(state);
        }

        NodeList endNodes = machineElement.getElementsByTagName("end");
        if (endNodes.getLength() > 0) {
            Element endElement = (Element) endNodes.item(0);
            endState = parseState(endElement, State.StateType.END);
            states.add(endState);
            machineDefinition.setEndState(endState);
        }

        machineDefinition.setStates(states);

        // Parse transitions - support both direct <transition> and <transitions role="..."> grouping
        List<Transition> transitions = new ArrayList<>();
        Set<String> processedTransitionIds = new HashSet<>();
        
        // First, parse transitions grouped by role
        NodeList transitionsGroups = machineElement.getElementsByTagName("transitions");
        for (int i = 0; i < transitionsGroups.getLength(); i++) {
            Element transitionsGroup = (Element) transitionsGroups.item(i);
            String role = transitionsGroup.getAttribute("role");
            
            // Get direct child transition elements (not nested ones)
            NodeList childNodes = transitionsGroup.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                if (childNodes.item(j).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element transitionElement = (Element) childNodes.item(j);
                    if ("transition".equals(transitionElement.getNodeName())) {
                        Transition transition = parseTransition(transitionElement);
                        transition.setRole(role); // Set the role from the parent element
                        transitions.add(transition);
                        processedTransitionIds.add(transition.getId());
                    }
                }
            }
        }
        
        // Also parse standalone transitions (direct children of machine, not in transitions groups) for backward compatibility
        NodeList allChildNodes = machineElement.getChildNodes();
        for (int i = 0; i < allChildNodes.getLength(); i++) {
            if (allChildNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                Element element = (Element) allChildNodes.item(i);
                if ("transition".equals(element.getNodeName())) {
                    // This is a direct child transition (not inside a transitions group)
                    Transition transition = parseTransition(element);
                    // Only add if not already processed from a transitions group
                    if (!processedTransitionIds.contains(transition.getId())) {
                        transitions.add(transition);
                    }
                }
            }
        }

        machineDefinition.setTransitions(transitions);

        return machineDefinition;
    }

    private State parseState(Element stateElement, State.StateType type) {
        String stateId = stateElement.getAttribute("id");
        String stateName = stateElement.hasAttribute("name") 
            ? stateElement.getAttribute("name") 
            : stateId;
        return new State(stateId, stateName, type);
    }

    private Transition parseTransition(Element transitionElement) {
        String transitionId = transitionElement.getAttribute("id");
        String transitionName = transitionElement.hasAttribute("name") 
            ? transitionElement.getAttribute("name") 
            : transitionId;
        
        // Handle typo in XML: "surce" should be "source"
        String source = transitionElement.hasAttribute("source") 
            ? transitionElement.getAttribute("source")
            : transitionElement.getAttribute("surce");
        
        String destination = transitionElement.getAttribute("destination");

        Transition transition = new Transition();
        transition.setId(transitionId);
        transition.setName(transitionName);
        transition.setSourceStateId(source);
        transition.setDestinationStateId(destination);

        // Parse condition if present
        NodeList conditionNodes = transitionElement.getElementsByTagName("condition");
        if (conditionNodes.getLength() > 0) {
            Element conditionElement = (Element) conditionNodes.item(0);
            String conditionText = conditionElement.getTextContent().trim();
            Condition condition = new Condition(conditionText);
            transition.setCondition(condition);
        }

        return transition;
    }
}

