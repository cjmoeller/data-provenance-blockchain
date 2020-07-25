package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDateTime;

/**
 * Implements the Data Acquisition WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class DataAcquisition extends WorkflowStep {


    private String originator;

    public String getOriginator() {
        return originator;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public LocalDateTime getTimeOfCreation() {
        return timeOfCreation;
    }

    public void setTimeOfCreation(LocalDateTime timeOfCreation) {
        this.timeOfCreation = timeOfCreation;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    private String device;
    private LocalDateTime timeOfCreation;
    private String format;

    public DataAcquisition(String name, String description, String hash, String originator, String device, LocalDateTime timeOfCreation, String format) {
        super(name, description, hash);
        this.originator = originator;
        this.device = device;
        this.timeOfCreation = timeOfCreation;
        this.format = format;
    }
}
