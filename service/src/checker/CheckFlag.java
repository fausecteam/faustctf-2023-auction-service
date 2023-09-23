package checker;

import de.faust.auction.AuctionException;
import de.faust.auction.AuctionService;
import de.faust.auction.communication.RPCConnection;
import de.faust.auction.model.Coupon;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class CheckFlag {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public static void main(String args[]) throws RemoteException, NotBoundException {
        // get command line arguments
        if(args.length < 4){
            System.err.println("usage: java " + CheckFlag.class.getName() + " <serverRegistryHost> <serverRegistryPort> <auctionName> <coupon>");
            return;
        }

        String serverRegistryHost = args[0]; //host name or IP the auction service is running on
        int serverRegistryPort = Integer.parseInt(args[1]); // The port of the registry where the AuctionService stub is registered
        String auctionName = args[2];
        String couponCode = args[3];

        try {
            RPCConnection.enableTimeouts();

            //get AuctionService stub from server's registry
            AuctionService auctionService = Utils.getAuctionService(serverRegistryHost, serverRegistryPort);

            Coupon coupon = new Coupon(couponCode);
            String flag = auctionService.buy(auctionName, coupon);

            logger.info(">>>flag>>>" + flag + "<<<");
        } catch (RemoteException e) {
            e.printStackTrace();
            Utils.exitFaulty();
        } catch (AuctionException e) {
            if ("Auction not found".equals(e.getMessage())) {
                logger.info("Auction not found");
                Utils.exitNotFound();
            }
            e.printStackTrace();
            Utils.exitFaulty();
        }
    }
}
