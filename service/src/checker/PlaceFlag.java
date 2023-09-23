package checker;

import de.faust.auction.AuctionException;
import de.faust.auction.AuctionService;
import de.faust.auction.AuctionServiceImpl;
import de.faust.auction.communication.RPCConnection;
import de.faust.auction.model.AuctionEntry;
import de.faust.auction.model.AuctionType;

import java.rmi.RemoteException;
import java.util.logging.Logger;

public class PlaceFlag {
    public static final int FLAG_PRICE = 100; // higher than Wallets.DEFAULT_BALANCE, so it can never be bought

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) {
        // get command line arguments
        if (args.length < 3) {
            System.err.println("usage: java " + PlaceFlag.class.getName() + " <serverRegistryHost> <serverRegistryPort> <flag>");
            return;
        }

        String serverRegistryHost = args[0]; //host name or IP the auction service is running on
        int serverRegistryPort = Integer.parseInt(args[1]); // The port of the registry where the AuctionService stub is registered
        String flag = args[2];

        try {
            RPCConnection.enableTimeouts();

            //get AuctionService stub from server's registry
            AuctionService auctionService = Utils.getAuctionService(serverRegistryHost, serverRegistryPort);

            String auctionName = Utils.randomString(16);

            AuctionEntry auctionEntry = new AuctionEntry(auctionName, AuctionType.BUY_IT_NOW, 0, null, flag, FLAG_PRICE);
            String coupon = auctionService.registerAuction(auctionEntry, AuctionServiceImpl.MAX_DURATION, null);

            logger.info(">>>auctionName>>>" + auctionName + "<<<");
            logger.info(">>>coupon>>>" + coupon + "<<<");
        } catch (RemoteException | AuctionException e) {
            e.printStackTrace();
            Utils.exitFaulty();
        }
    }
}
