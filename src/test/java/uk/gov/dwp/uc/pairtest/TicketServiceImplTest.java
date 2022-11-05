package uk.gov.dwp.uc.pairtest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.util.Constants.INFANT_TICKETS_MORE_THAN_ADULT_TICKETS_MSG;
import static uk.gov.dwp.uc.pairtest.util.Constants.INVALID_ACCOUNT_ID_MSG;
import static uk.gov.dwp.uc.pairtest.util.Constants.MAX_NO_OF_TICKET_PURCHASE_LIMIT_EXCEEDED_MSG;
import static uk.gov.dwp.uc.pairtest.util.Constants.MAX_TICKET_LIMIT;
import static uk.gov.dwp.uc.pairtest.util.Constants.NO_ADULT_TICKET_MSG;
import static uk.gov.dwp.uc.pairtest.util.Constants.NULL_REQUEST_MSG;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;
    @Mock
    private SeatReservationService seatReservationService;
    @InjectMocks
    private TicketServiceImpl ticketService;
    @Captor
    private ArgumentCaptor<Integer> totalAmountCaptor;
    @Captor
    private ArgumentCaptor<Integer> totalSeatsToAllocateCaptor;
    @Captor
    private ArgumentCaptor<Long> accountIdCaptor;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testPurchaseTicketSuccess() {
        long accountId = 1;
        TicketTypeRequest adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        TicketTypeRequest childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 9);

        ticketService.purchaseTickets(accountId, adultTicketRequest, childTicketRequest, infantTicketRequest);
        Mockito.verify(this.ticketPaymentService).makePayment(this.accountIdCaptor.capture(), this.totalAmountCaptor.capture());
        Mockito.verify(this.seatReservationService).reserveSeat(this.accountIdCaptor.capture(), this.totalSeatsToAllocateCaptor.capture());

        Assert.assertEquals(Integer.valueOf(11), this.totalSeatsToAllocateCaptor.getValue());
        Assert.assertEquals(Integer.valueOf(210), this.totalAmountCaptor.getValue());
        Assert.assertEquals(Long.valueOf(1), this.accountIdCaptor.getValue());

    }

    @Test
    public void testPurchaseTicketWhenTotalNoOfTicketsMoreThanTwenty() {
        long accountId = 2;
        TicketTypeRequest adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        TicketTypeRequest childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        TicketTypeRequest infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6);

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage(String.format(MAX_NO_OF_TICKET_PURCHASE_LIMIT_EXCEEDED_MSG, MAX_TICKET_LIMIT));

        ticketService.purchaseTickets(accountId, adultTicketRequest, childTicketRequest, infantTicketRequest);
    }

    @Test
    public void testPurchaseChildAndInfantTicketsWithoutAdultTickets() {
        long accountId = 3;
        TicketTypeRequest adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        TicketTypeRequest infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6);

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage(NO_ADULT_TICKET_MSG);

        ticketService.purchaseTickets(accountId, adultTicketRequest, childTicketRequest, infantTicketRequest);
    }

    @Test
    public void testPurchaseMoreInfantTicketsThanAdultTickets() {
        long accountId = 4;
        TicketTypeRequest adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);
        TicketTypeRequest childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        TicketTypeRequest infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6);

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage(INFANT_TICKETS_MORE_THAN_ADULT_TICKETS_MSG);

        ticketService.purchaseTickets(accountId, adultTicketRequest, childTicketRequest, infantTicketRequest);
    }

    @Test
    public void testPurchaseNullTicketRequest() {
        long accountId = 5;

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage(NULL_REQUEST_MSG);

        ticketService.purchaseTickets(accountId, null);
    }

    @Test
    public void testPurchaseTicketForInvalidAccount() {
        long accountId = 0;
        TicketTypeRequest adultTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);
        TicketTypeRequest childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage(INVALID_ACCOUNT_ID_MSG);

        ticketService.purchaseTickets(accountId, adultTicketRequest, childTicketRequest);
    }

}