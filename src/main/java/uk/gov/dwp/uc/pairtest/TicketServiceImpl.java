package uk.gov.dwp.uc.pairtest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.util.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;
import static uk.gov.dwp.uc.pairtest.util.Constants.ADULT_TICKET_PRICE_IN_GBP;
import static uk.gov.dwp.uc.pairtest.util.Constants.CHILD_TICKET_PRICE_IN_GBP;
import static uk.gov.dwp.uc.pairtest.util.Constants.INFANT_TICKET_PRICE_IN_GBP;
import static uk.gov.dwp.uc.pairtest.util.Constants.INVALID_ACCOUNT_ID_MSG;
import static uk.gov.dwp.uc.pairtest.util.Constants.MAX_TICKET_LIMIT;
import static uk.gov.dwp.uc.pairtest.util.Constants.NULL_REQUEST_MSG;

public class TicketServiceImpl implements TicketService {

    private static final Logger logger = LogManager.getLogger(TicketServiceImpl.class);

    private TicketPaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;

    TicketServiceImpl(final TicketPaymentService ticketPaymentService, final SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Purchase cinema tickets for valid request
     *
     * @param accountId
     * @param ticketTypeRequests
     * @throws InvalidPurchaseException
     */
    @Override
    public synchronized void purchaseTickets(final Long accountId, final TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        logger.debug("Purchase ticket request for account Id {}: {}", accountId, ticketTypeRequests);
        accountIdValidator(accountId);
        nullRequestChecker(ticketTypeRequests);
        List<TicketTypeRequest> filteredTicketTypeRequestList = Arrays.stream(ticketTypeRequests).filter(Objects::nonNull).collect(Collectors.toList());

        requestValidator(filteredTicketTypeRequestList);

        int totalAmountToPay = 0;
        int totalSeatsToAllocate = 0;
        for (TicketTypeRequest ticketTypeRequest : filteredTicketTypeRequestList) {
            totalAmountToPay += calculateAmountForEachTicketType(ticketTypeRequest);
            totalSeatsToAllocate += seatCalculator(ticketTypeRequest);
        }
        //make payment
        logger.info("total amount to pay {}", totalAmountToPay);
        ticketPaymentService.makePayment(accountId, totalAmountToPay);
        //reserve seat
        logger.info("total seats to allocate {}", totalSeatsToAllocate);
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);


    }

    /**
     * Throws exception if ticket type request is null
     *
     * @param ticketTypeRequests
     */
    private void nullRequestChecker(final TicketTypeRequest[] ticketTypeRequests) {
        if(Objects.isNull(ticketTypeRequests)){
            logger.error(NULL_REQUEST_MSG);
            throw new InvalidPurchaseException(NULL_REQUEST_MSG);
        }
    }

    /**
     * Account Id validator
     *
     * @param accountId
     */
    private void accountIdValidator(final long accountId) {
        if(accountId<1){
            logger.error(INVALID_ACCOUNT_ID_MSG);
            throw new InvalidPurchaseException(INVALID_ACCOUNT_ID_MSG);
        }
    }

    /**
     * Calculate required seats for each ticket type request
     *
     * @param ticketTypeRequest
     * @return
     */
    private int seatCalculator(final TicketTypeRequest ticketTypeRequest) {
        int seatsToAllocate = 0;
        if (ticketTypeRequest.getTicketType().equals(ADULT)
                || ticketTypeRequest.getTicketType().equals(TicketTypeRequest.Type.CHILD)) {
            seatsToAllocate = ticketTypeRequest.getNoOfTickets();
        }
        logger.debug("seats to allocate per ticket type request {}", seatsToAllocate);
        return seatsToAllocate;
    }

    /**
     * Validate requests and throws exception if invalid
     *
     * @param ticketTypeRequests
     * @throws InvalidPurchaseException
     */
    private void requestValidator(final List<TicketTypeRequest> ticketTypeRequests) throws InvalidPurchaseException {
        int totalNoOfAllTickets = 0;
        int totalNoOfAdultTickets = 0;
        int totalNoOfInfantTickets = 0;

        if (Objects.isNull(ticketTypeRequests) || ticketTypeRequests.isEmpty()) {
            throw new InvalidPurchaseException(NULL_REQUEST_MSG);
        }


        boolean hasAdultTicket = false;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            totalNoOfAllTickets += ticketTypeRequest.getNoOfTickets();
            totalNoOfAdultTickets += calculateNoOfTicketsForEachType(ticketTypeRequest, ADULT);
            totalNoOfInfantTickets += calculateNoOfTicketsForEachType(ticketTypeRequest, INFANT);

            // check maximum ticket limit
            maxTicketLimitChecker(totalNoOfAllTickets);

            if (ticketTypeRequest.getTicketType().equals(ADULT) && ticketTypeRequest.getNoOfTickets() > 0) {
                logger.debug("Adult ticket present in the request");
                hasAdultTicket = true;
            }
        }
        //
        ticketTypeRequestValidator(hasAdultTicket, totalNoOfAdultTickets, totalNoOfInfantTickets);
    }

    /**
     * Checks if adult ticket present or infant tickets more than adults tickets
     *
     * @param hasAdultTicket
     * @param totalNoOfAdultTickets
     * @param totalNoOfInfantTickets
     */
    private void ticketTypeRequestValidator(final boolean hasAdultTicket, final int totalNoOfAdultTickets, final int totalNoOfInfantTickets) {
        if (!hasAdultTicket) {
            logger.error(Constants.NO_ADULT_TICKET_MSG);
            throw new InvalidPurchaseException(Constants.NO_ADULT_TICKET_MSG);
        } else if (totalNoOfInfantTickets > totalNoOfAdultTickets) {
            logger.error(Constants.INFANT_TICKETS_MORE_THAN_ADULT_TICKETS_MSG);
            throw new InvalidPurchaseException(Constants.INFANT_TICKETS_MORE_THAN_ADULT_TICKETS_MSG);
        }
    }

    /**
     * Throws exception if max ticket limit exceeds
     *
     * @param totalNoOfAllTickets
     * @throws InvalidPurchaseException
     */
    private void maxTicketLimitChecker(final int totalNoOfAllTickets) throws InvalidPurchaseException {
        if (totalNoOfAllTickets > MAX_TICKET_LIMIT) {
            logger.error(String.format(Constants.MAX_NO_OF_TICKET_PURCHASE_LIMIT_EXCEEDED_MSG, MAX_TICKET_LIMIT));
            throw new InvalidPurchaseException(String.format(Constants.MAX_NO_OF_TICKET_PURCHASE_LIMIT_EXCEEDED_MSG, MAX_TICKET_LIMIT));
        }
    }

    /**
     * Returns total no of tickets for each ticket type
     *
     * @param ticketTypeRequest
     * @return
     */
    private int calculateNoOfTicketsForEachType(final TicketTypeRequest ticketTypeRequest, final TicketTypeRequest.Type type) {
        int totalNoOfInfantTickets = 0;
        if (type.equals(ticketTypeRequest.getTicketType())) {
            totalNoOfInfantTickets = ticketTypeRequest.getNoOfTickets();

        }
        return totalNoOfInfantTickets;
    }

    /**
     * Calculates total amount for each ticket type
     *
     * @param ticketTypeRequest
     * @return
     */
    private int calculateAmountForEachTicketType(final TicketTypeRequest ticketTypeRequest) {
        int amountForEachTicketType = 0;
        switch (ticketTypeRequest.getTicketType()) {
            case ADULT:
                amountForEachTicketType = ticketTypeRequest.getNoOfTickets() * ADULT_TICKET_PRICE_IN_GBP;
                break;
            case CHILD:
                amountForEachTicketType = ticketTypeRequest.getNoOfTickets() * CHILD_TICKET_PRICE_IN_GBP;
                break;
            case INFANT:
                amountForEachTicketType = ticketTypeRequest.getNoOfTickets() * INFANT_TICKET_PRICE_IN_GBP;
                break;
            default:
                logger.error(Constants.INVALID_TICKET_TYPE_MSG);
                throw new InvalidPurchaseException(Constants.INVALID_TICKET_TYPE_MSG);
        }
        return amountForEachTicketType;
    }


}
