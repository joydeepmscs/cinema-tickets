package uk.gov.dwp.uc.pairtest.util;

public class Constants {
    public static final String MAX_NO_OF_TICKET_PURCHASE_LIMIT_EXCEEDED_MSG = "Max no of ticket purchase limit %s exceeded";
    public static final String INVALID_TICKET_TYPE_MSG = "Invalid ticket type";
    public static final String NO_ADULT_TICKET_MSG = "No adult ticket present in the request";
    public static final String INFANT_TICKETS_MORE_THAN_ADULT_TICKETS_MSG = "Infant tickets more than adult tickets";
    public static final String NULL_REQUEST_MSG = "Ticket type requests is null";
    public static final String INVALID_ACCOUNT_ID_MSG = "Invalid account Id";

    public static final int MAX_TICKET_LIMIT = 20;
    public static int ADULT_TICKET_PRICE_IN_GBP = 20;
    public static int CHILD_TICKET_PRICE_IN_GBP = 10;
    public static int INFANT_TICKET_PRICE_IN_GBP = 0;
}
