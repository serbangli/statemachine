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
import java.util.List;

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

        // Parse transitions
        List<Transition> transitions = new ArrayList<>();
        NodeList transitionNodes = machineElement.getElementsByTagName("transition");
        for (int i = 0; i < transitionNodes.getLength(); i++) {
            Element transitionElement = (Element) transitionNodes.item(i);
            Transition transition = parseTransition(transitionElement);
            transitions.add(transition);
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

