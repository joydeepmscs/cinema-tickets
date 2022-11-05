package thirdparty.paymentgateway;

public class TicketPaymentServiceImpl  implements TicketPaymentService {

    @Override
    public void makePayment(final long accountId, final int totalAmountToPay) {
        //Real implementation omitted, assume a work code will take the payment using a card pre linked to the account.
    }

}
