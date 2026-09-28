public class MessageDemo {
    public static void main(String[] args) {

        EmailMessage email = new EmailMessage("student@feutech.edu.ph");
        SmsMessage sms = new SmsMessage("09123456789");

        Message[] messages = {email, sms};

        System.out.println("=== Campus Message Dispatcher ===");

        System.out.println("\nSending Messages:");

        for (Message message : messages) {
            message.send();
        }

        System.out.println("\nTracking Status:");

        Trackable trackableMessage = email;
        trackableMessage.showStatus();

        System.out.println("\nUpcasting and Dynamic Dispatch Explanation:");
        System.out.println("EmailMessage and SmsMessage objects are upcast and stored as Message references in the Message array.");
        System.out.println("During runtime, dynamic dispatch calls the overridden send() method of the actual object, so each message uses its own implementation.");
        System.out.println("The Trackable reference also calls the showStatus() implementation of the object it references.");
    }
}