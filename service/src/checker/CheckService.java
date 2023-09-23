package checker;

import de.faust.auction.AuctionException;
import de.faust.auction.AuctionService;
import de.faust.auction.communication.RPCConnection;
import de.faust.auction.model.*;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

public class CheckService {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final Random RNG = new Random();

    public static void main(String[] args) {
        // get command line arguments
        if (args.length < 3) {
            System.err.println("usage: java " + CheckService.class.getName() + " <serverRegistryHost> <serverRegistryPort> <randomString>");
            return;
        }

        String serverRegistryHost = args[0]; //host name or IP the auction service is running on
        int serverRegistryPort = Integer.parseInt(args[1]); // The port of the registry where the AuctionService stub is registered
        String randomString = args[2];

        try {

            String randomString2 = Utils.randomString(14);
            if (RNG.nextBoolean()) {
                String swap = randomString2;
                randomString2 = randomString;
                randomString = swap;
            }

            RPCConnection.enableTimeouts();

            //get AuctionService stub from server's registry
            AuctionService auctionService = Utils.getAuctionService(serverRegistryHost, serverRegistryPort);

            //use stub in the intended way and register an auction
            if (RNG.nextBoolean()) {
                checkNormalAuction(auctionService, randomString);
                checkByItNowAuction(auctionService, randomString2);
            } else {
                checkByItNowAuction(auctionService, randomString2);
                checkNormalAuction(auctionService, randomString);
            }
        } catch (RemoteException | AuctionException e) {
            e.printStackTrace();
            Utils.exitFaulty();
        }
    }

    private static void checkNormalAuction(AuctionService auctionService, String content) throws RemoteException, AuctionException {
        System.err.println("checkNormalAuction");
        String name = Utils.randomString(16);
        int price = RNG.nextInt(20);
        int duration = 300 + RNG.nextInt(300);
        AuctionEntry auction = new AuctionEntry(name, AuctionType.NORMAL, 0, null, content, price);

        String result = auctionService.registerAuction(auction, duration, null);
        if (result != null) {
            System.err.println("registerAuction return non-null");
            Utils.exitFaulty();
        }

        // fetch auctions
        AuctionEntry[] auctions = auctionService.getAuctions();
        if (auctions == null || auctions.length == 0) {
            System.err.println("getAuctions return null or empty array");
            Utils.exitFaulty();
        }
        AuctionEntry returned = Arrays.stream(auctions)
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
        if (returned == null) {
            System.err.println("registered auction not found");
            Utils.exitFaulty();
        }
        if (returned.hasEnded()
                || returned.getAuctionType() != auction.getAuctionType()
                || returned.getContent() != null
                || returned.getCouponCode() != null
                || returned.getPrice() != price) {
            System.err.println("returned auction != registered auction");
            Utils.exitFaulty();
        }

        int bidPrice = price + RNG.nextInt(20) + 1;
        boolean bidResult = auctionService.placeBid(Utils.randomString(16), name, bidPrice, null);
        if (!bidResult) {
            System.err.println("placing bid failed");
            Utils.exitFaulty();
        }
        System.err.println("auctionService.placeBid success");

        // fetch auctions again
        auctions = auctionService.getAuctions();
        if (auctions == null || auctions.length == 0) {
            System.err.println("getAuctions return null or empty array");
            Utils.exitFaulty();
        }
        returned = Arrays.stream(auctions)
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
        if (returned == null) {
            System.err.println("bidded auction not found");
            Utils.exitFaulty();
        }
        if (returned.hasEnded()
                || returned.getAuctionType() != auction.getAuctionType()
                || returned.getContent() != null
                || returned.getCouponCode() != null
                || returned.getPrice() != bidPrice) {
            System.err.println("returned auction != bidded auction");
            Utils.exitFaulty();
        }
    }

    private static void checkByItNowAuction(AuctionService auctionService, String content) throws RemoteException, AuctionException {
        System.err.println("checkByItNowAuction");
        String name = Utils.randomString(16);
        int price = 1 + RNG.nextInt(10);
        int duration = 300 + RNG.nextInt(300);
        AuctionEntry auction = new AuctionEntry(name, AuctionType.BUY_IT_NOW, 0, null, content, price);

        String result = auctionService.registerAuction(auction, duration, null);
        if (result == null) {
            System.err.println("registerAuction return null");
            Utils.exitFaulty();
        }
        if (result.length() != 32) {
            System.err.println("registerAuction return not 32 chars");
            Utils.exitFaulty();
        }

        // fetch auctions
        AuctionEntry[] auctions = auctionService.getAuctions();
        if (auctions == null || auctions.length == 0) {
            System.err.println("getAuctions return null or empty array");
            Utils.exitFaulty();
        }
        AuctionEntry returned = Arrays.stream(auctions)
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
        if (returned == null) {
            System.err.println("registered auction not found");
            Utils.exitFaulty();
        }
        if (returned.hasEnded()
                || returned.getAuctionType() != auction.getAuctionType()
                || returned.getContent() != null
                || returned.getCouponCode() != null
                || returned.getPrice() != price) {
            System.err.println("returned auction != registered auction");
            Utils.exitFaulty();
        }

        PaymentMethod paymentMethod = new Wallet(Utils.randomGuid());
        if (RNG.nextInt(4) == 0) {
            paymentMethod = new Coupon(result);
        }
        String buyResult = auctionService.buy(name, paymentMethod);
        if (!content.equals(buyResult)) {
            System.err.println("buy auction failed");
            Utils.exitFaulty();
        }
        System.err.println("auctionService.buy success");

        // fetch auctions again
        auctions = auctionService.getAuctions();
        if (auctions == null || auctions.length == 0) {
            System.err.println("getAuctions return null or empty array");
            Utils.exitFaulty();
        }
        returned = Arrays.stream(auctions)
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElse(null);
        if (returned == null) {
            System.err.println("registered auction not found");
            Utils.exitFaulty();
        }
        if (returned.hasEnded()
                || returned.getAuctionType() != auction.getAuctionType()
                || returned.getContent() != null
                || returned.getCouponCode() != null
                || returned.getPrice() != price) {
            System.err.println("returned auction != registered auction");
            Utils.exitFaulty();
        }

        // try to buy it again
        if (paymentMethod instanceof Wallet) {
            buyResult = auctionService.buy(name, paymentMethod);
            boolean shouldBeSuccess = price * 2 <= 10;
            if (shouldBeSuccess) {
                if (!content.equals(buyResult)) {
                    System.err.println("buy auction failed");
                    Utils.exitFaulty();
                }
            } else if (buyResult != null) {
                System.err.println("buy auction expected to fail");
                Utils.exitFaulty();
            }
        }
    }
}
