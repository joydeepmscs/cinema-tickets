package uk.gov.dwp.uc.pairtest.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Immutable Object
 */
@ToString
@EqualsAndHashCode
public final class TicketTypeRequest {

    private int noOfTickets;
    private Type type;

    public TicketTypeRequest(final Type type, final int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT, CHILD, INFANT
    }

}
