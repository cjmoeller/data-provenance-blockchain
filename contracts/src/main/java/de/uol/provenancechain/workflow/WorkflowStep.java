package de.uol.provenancechain.workflow;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDateTime;

/**
 * Implements the WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class WorkflowStep {
    public String getHash() {
        return hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    private String description;
    private LocalDateTime time;
    private final String hash;
    private String name;

    @ConstructorForDeserialization
    public WorkflowStep(String name, String description, String hash) {
        this.name = name;
        this.description = description;
        this.time = LocalDateTime.now();
        this.hash = hash;
    }
}
