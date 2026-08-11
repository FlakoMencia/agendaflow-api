package com.flakomencia.agendaflow.scheduling.application;

import com.flakomencia.agendaflow.common.exception.ConflictException;

public class SlotNotAvailableException extends ConflictException {
    public SlotNotAvailableException() {
        super("SLOT_NOT_AVAILABLE", "The requested slot is not available");
    }
}
