package com.example.cycleworkbench.task;

import java.time.Instant;

public class TriggerInstanceRow {

    private Long id;
    private Long taskId;
    private int ordinal;
    private InstanceState state;
    private String dstNote;
    private java.time.LocalDateTime nominalLocal;
    private Instant actualUtc;
    private java.time.LocalDateTime actualLocal;
    private String utcOffset;
    private String explanation;
    private Instant generatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public String getDstNote() {
        return dstNote;
    }

    public void setDstNote(String dstNote) {
        this.dstNote = dstNote;
    }

    public java.time.LocalDateTime getNominalLocal() {
        return nominalLocal;
    }

    public void setNominalLocal(java.time.LocalDateTime nominalLocal) {
        this.nominalLocal = nominalLocal;
    }

    public Instant getActualUtc() {
        return actualUtc;
    }

    public void setActualUtc(Instant actualUtc) {
        this.actualUtc = actualUtc;
    }

    public java.time.LocalDateTime getActualLocal() {
        return actualLocal;
    }

    public void setActualLocal(java.time.LocalDateTime actualLocal) {
        this.actualLocal = actualLocal;
    }

    public String getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(String utcOffset) {
        this.utcOffset = utcOffset;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
