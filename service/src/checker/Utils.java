package checker;

import de.faust.auction.AuctionService;
import de.faust.auction.RPCInvocationHandler;
import de.faust.auction.communication.RPCRemoteReference;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Proxy;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.SecureRandom;
import java.util.Random;

public final class Utils {
    private Utils() {}

    public static final Random RANDOM = new SecureRandom();
    public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    public static String randomString(int length) {
        char[] buf = new char[length];
        for (int idx = 0; idx < length; ++idx)
            buf[idx] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        return new String(buf);
    }
    
    public static String randomGuid() {
        return java.util.UUID.randomUUID().toString();
    }
    
    public static void exitFaulty() {
        System.err.println(">>>FAULTY<<<");
        System.exit(1);
    }

    public static void exitDown() {
        System.err.println(">>>DOWN<<<");
        System.exit(1);
    }

    public static void exitNotFound() {
        System.err.println(">>>FLAG_NOT_FOUND<<<");
        System.exit(1);
    }
    
    private static final VarHandle handlerReference;
    static {
        try {
            handlerReference = MethodHandles
                    .privateLookupIn(RPCInvocationHandler.class,MethodHandles.lookup())
                    .findVarHandle(RPCInvocationHandler .class, "remoteReference", RPCRemoteReference.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static AuctionService getAuctionService(String serverRegistryHost, int serverRegistryPort)  {
        try {
            Registry registry = LocateRegistry.getRegistry(serverRegistryHost, serverRegistryPort);
            AuctionService auctionService = (AuctionService) registry.lookup("auctionService");
            if (!Proxy.isProxyClass(auctionService.getClass())) {
                exitFaulty();
            }
            if (!(Proxy.getInvocationHandler(auctionService) instanceof RPCInvocationHandler invocationHandler)) {
                exitFaulty();
            } else {
                RPCRemoteReference remoteReference = (RPCRemoteReference) handlerReference.get(invocationHandler);
                if (!ipv6Equals(serverRegistryHost, remoteReference.getHost())) {
                    System.err.println("invalid remote reference: " + remoteReference);
                    exitFaulty();
                }

                // try to connect to the remoteReference
                checkInvocationHandlerConnection(invocationHandler);
            }
            return auctionService;
        }catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            exitDown();
            throw new RuntimeException("unreachable");
        }
    }
    
    private static void checkInvocationHandlerConnection(RPCInvocationHandler invocationHandler) {
        // create a dummy invoke call that connects the socket
        // but later fails in RPCRemoteObjectManager.getStub()
        Object willFail = new Object() {
            @Override
            public int hashCode() {
                throw new RuntimeException("intended fail");
            }

            @Override
            public boolean equals(Object obj) {
                throw new RuntimeException("intended fail");
            }
        };

        try {
            invocationHandler.invoke(null, Utils.class.getMethods()[0], new Object[]{willFail});
            throw new RuntimeException("unreachable");
        } catch (RemoteException e) {
            if (e.getMessage().startsWith("connection failed;")) {
                // socket connection failed
                e.getCause().printStackTrace();
                exitDown();
            }
        }
        catch (RuntimeException e) {
            if ("intended fail".equals(e.getMessage())) {
                // socket is connected
                return;
            }
            throw e;
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    private static boolean ipv6Equals(String a, String b) {
        try {
            return Inet6Address.getByName(a).equals(Inet6Address.getByName(b));
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
