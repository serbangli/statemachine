package com.statemachine.service;

import com.statemachine.domain.entity.MachineDefinitionEntity;
import com.statemachine.domain.entity.StateEntity;
import com.statemachine.domain.entity.TransitionEntity;
import com.statemachine.domain.model.Condition;
import com.statemachine.domain.model.MachineDefinition;
import com.statemachine.domain.model.State;
import com.statemachine.domain.model.Transition;
import com.statemachine.repository.MachineDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MachineDefinitionService {

    @Autowired
    private MachineDefinitionRepository machineDefinitionRepository;

    @Autowired
    private XmlMachineParser xmlMachineParser;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${statemachine.xml.path:classpath:machines/}")
    private String xmlPath;

    @Transactional
    public MachineDefinitionEntity loadFromXml(String xmlFileName) throws Exception {
        Resource resource = resourceLoader.getResource(xmlPath + xmlFileName);
        InputStream inputStream = resource.getInputStream();
        
        MachineDefinition machineDefinition = xmlMachineParser.parse(inputStream);
        
        // Check if definition already exists
        Optional<MachineDefinitionEntity> existing = machineDefinitionRepository.findById(machineDefinition.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Machine definition with id " + machineDefinition.getId() + " already exists");
        }

        // Convert to entity and save
        MachineDefinitionEntity entity = convertToEntity(machineDefinition, resource);
        return machineDefinitionRepository.save(entity);
    }

    @Transactional
    public MachineDefinitionEntity loadFromXmlContent(String xmlContent, String machineId) throws Exception {
        InputStream inputStream = new java.io.ByteArrayInputStream(xmlContent.getBytes());
        MachineDefinition machineDefinition = xmlMachineParser.parse(inputStream);
        
        // Check if definition already exists
        Optional<MachineDefinitionEntity> existing = machineDefinitionRepository.findById(machineDefinition.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Machine definition with id " + machineDefinition.getId() + " already exists");
        }

        // Convert to entity and save
        MachineDefinitionEntity entity = convertToEntity(machineDefinition, xmlContent);
        return machineDefinitionRepository.save(entity);
    }

    public Optional<MachineDefinitionEntity> getDefinition(String machineDefinitionId) {
        return machineDefinitionRepository.findById(machineDefinitionId);
    }

    public List<MachineDefinitionEntity> getAllDefinitions() {
        return machineDefinitionRepository.findAll();
    }

    public MachineDefinition convertToModel(MachineDefinitionEntity entity) {
        MachineDefinition model = new MachineDefinition();
        model.setId(entity.getId());
        model.setName(entity.getName());

        List<State> states = entity.getStates().stream()
            .map(this::convertStateToModel)
            .collect(Collectors.toList());
        model.setStates(states);

        List<Transition> transitions = entity.getTransitions().stream()
            .map(this::convertTransitionToModel)
            .collect(Collectors.toList());
        model.setTransitions(transitions);

        // Set start and end states
        entity.getStates().stream()
            .filter(s -> s.getType() == StateEntity.StateType.START)
            .findFirst()
            .ifPresent(s -> model.setStartState(convertStateToModel(s)));

        entity.getStates().stream()
            .filter(s -> s.getType() == StateEntity.StateType.END)
            .findFirst()
            .ifPresent(s -> model.setEndState(convertStateToModel(s)));

        return model;
    }

    private MachineDefinitionEntity convertToEntity(MachineDefinition model, Resource resource) throws Exception {
        return convertToEntity(model, new String(resource.getInputStream().readAllBytes()));
    }

    private MachineDefinitionEntity convertToEntity(MachineDefinition model, String xmlContent) {
        MachineDefinitionEntity entity = new MachineDefinitionEntity();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setXmlContent(xmlContent);

        // Convert states
        List<StateEntity> stateEntities = model.getStates().stream()
            .map(state -> {
                StateEntity stateEntity = new StateEntity();
                stateEntity.setStateId(state.getId());
                stateEntity.setName(state.getName());
                stateEntity.setType(convertStateType(state.getType()));
                stateEntity.setMachineDefinition(entity);
                return stateEntity;
            })
            .collect(Collectors.toList());
        entity.setStates(stateEntities);

        // Convert transitions
        List<TransitionEntity> transitionEntities = model.getTransitions().stream()
            .map(transition -> {
                TransitionEntity transitionEntity = new TransitionEntity();
                transitionEntity.setTransitionId(transition.getId());
                transitionEntity.setName(transition.getName());
                transitionEntity.setSourceStateId(transition.getSourceStateId());
                transitionEntity.setDestinationStateId(transition.getDestinationStateId());
                if (transition.getCondition() != null) {
                    transitionEntity.setConditionExpression(transition.getCondition().getExpression());
                }
                transitionEntity.setRole(transition.getRole());
                transitionEntity.setMachineDefinition(entity);
                return transitionEntity;
            })
            .collect(Collectors.toList());
        entity.setTransitions(transitionEntities);

        return entity;
    }

    private State convertStateToModel(StateEntity entity) {
        State.StateType type = entity.getType() == StateEntity.StateType.START 
            ? State.StateType.START
            : entity.getType() == StateEntity.StateType.END 
                ? State.StateType.END 
                : State.StateType.REGULAR;
        return new State(entity.getStateId(), entity.getName(), type);
    }

    private Transition convertTransitionToModel(TransitionEntity entity) {
        Transition transition = new Transition();
        transition.setId(entity.getTransitionId());
        transition.setName(entity.getName());
        transition.setSourceStateId(entity.getSourceStateId());
        transition.setDestinationStateId(entity.getDestinationStateId());
        if (entity.getConditionExpression() != null) {
            transition.setCondition(new Condition(entity.getConditionExpression()));
        }
        transition.setRole(entity.getRole());
        return transition;
    }

    private StateEntity.StateType convertStateType(State.StateType type) {
        switch (type) {
            case START:
                return StateEntity.StateType.START;
            case END:
                return StateEntity.StateType.END;
            default:
                return StateEntity.StateType.REGULAR;
        }
    }
}

