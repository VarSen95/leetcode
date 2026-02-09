package selfpractice.dayTwo.factory;

public class PaymentMethodFactory {

    public PaymentMethod create(String type){
        return switch (type) {
            case "Card" -> new CardPayment();
            case "BankTransfer" -> new BankTransferPayment();
            case "Wallet" -> new WalletPayment();
            default -> throw new IllegalArgumentException("Unknown payment method: " + type);
        };
    }

    public static void main(String[] args) {
        PaymentMethodFactory factory = new PaymentMethodFactory();
        PaymentMethod card = factory.create("Card1");
        System.out.println(card.getType());
    }
    
}
