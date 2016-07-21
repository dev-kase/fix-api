package fix;

import fix.utils.SoutLogger;
import kz.kase.fix.FixProtocol;
import kz.kase.fix.MassStatusReqType;
import kz.kase.fix.SecurityListRequestType;
import kz.kase.fix.SubscriptionType;
import kz.kase.fix.factory.KaseFixMessageFactory;
import kz.kase.fix.messages.MarketDataRequest;
import kz.kase.fix.messages.OrderMassStatusRequest;
import kz.kase.fix.messages.OrderStatusRequest;
import kz.kase.fix.messages.SecurityListRequest;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import quickfix.*;
import quickfix.logging.LogFactory;
import quickfix.logging.ScreenLogFactory;
import quickfix.store.FileStoreFactory;
import quickfix.store.MessageStoreFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicLong;

public class SampleClient {
    public static Logger log = Logger.getLogger(SampleClient.class);

    public static File getDealsDir() {
        return DEALS_DIR;
    }

    public static File getOrdersDir() {
        return ORDERS_DIR;
    }

    public static File getInstrDir() {
        return INSTR_DIR;
    }

    public static File getMdRefrDir() {
        return MDREQ_DIR;
    }

    private static AtomicLong ref = new AtomicLong(1);

    private static final File ORDERS_DIR = new File("files.order");

    private static final File DEALS_DIR = new File("files.deal");
    private static final File INSTR_DIR = new File("files.inst");
    private static final File MDREQ_DIR = new File("files.mdreq");
    private static final String FIX_CFG = "fix.cfg";
    public static final String LOG4J_PROPERTIES = "log4j.properties";

    public static final String HEART_BT_INT = "HeartBtInt";

    public static final int REF_RAND_SEED = 100000;
    private static long HARD_BEAT_SEC = 3000;

    private final MyApplication app;
    private boolean stop;

    public SampleClient() throws
            FileNotFoundException,
            ConfigError,
            InterruptedException {
        SessionSettings settings = new SessionSettings(new FileInputStream(FIX_CFG));
        try {
            HARD_BEAT_SEC = settings.getLong(HEART_BT_INT) * 1000;
        } catch (FieldConvertError ignored) {

        }

        app = new MyApplication(settings);

        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true, false);
        MessageFactory mesFactory = new KaseFixMessageFactory();
        SocketInitiator initiator =
                new SocketInitiator(app, storeFactory, settings, logFactory, mesFactory);
        initiator.start();

        app.awaitLogon();
    }

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure(LOG4J_PROPERTIES);
        SoutLogger.tieSystemOutAndErrToLog();

        if (!INSTR_DIR.exists()) {
            System.out.println("Creating directory: instruments");
            boolean result = INSTR_DIR.mkdir();

            if (result) {
                System.out.println("Instr directory created");
            }
        }

        if (!DEALS_DIR.exists()) {
            System.out.println("Creating directory: deals");
            boolean result = DEALS_DIR.mkdir();

            if (result) {
                System.out.println("Deals directory created");
            }
        }

        if (!ORDERS_DIR.exists()) {
            System.out.println("Creating directory: orders");
            boolean result = ORDERS_DIR.mkdir();

            if (result) {
                System.out.println("Orders directory created");
            }
        }

        if (!MDREQ_DIR.exists()) {
            System.out.println("Creating directory: mdreq");
            boolean result = MDREQ_DIR.mkdir();

            if (result) {
                System.out.println("MDReq directory created");
            }
        }

        SampleClient client = new SampleClient();

        client.securityListReq();
        client.orderStatusReq();
        client.mdRefreshReq();

        client.orderMassStatusRequest("KZTO_T2", "12345");

        while (!client.isStopped()) {
            Thread.sleep(HARD_BEAT_SEC);
        }
    }


    public boolean isStopped() {
        return stop;
    }

    public void stop() {
        stop = true;
    }


    private void securityListReq() {
        System.out.println("Sending security-list-request...");
        SecurityListRequest secReq = new SecurityListRequest();
        secReq.setRef(nextRef());
        secReq.setType(SecurityListRequestType.ALL_SECURITIES);
        app.sendMessage(secReq);
    }

    private void orderStatusReq() {
        OrderStatusRequest ordReq = new OrderStatusRequest();
        ordReq.setRef(nextRef());
        app.sendMessage(ordReq);
    }

    private void orderMassStatusRequest(String instrument, String massStatusReqID) {
        OrderMassStatusRequest req = new OrderMassStatusRequest();
        req.setMassStatusReqID(massStatusReqID);
        req.setSymbol(instrument);
        req.setMassStatusReqType(MassStatusReqType.STATUS_FOR_ALL_ORDERS);
        app.sendMessage(req);
    }

    private void mdRefreshReq() {
        getFullSnapReq(nextRef(), SubscriptionType.SNAPSHOT_AND_UPDATES);
    }

    public void getFullSnapReq(long MDReqID, SubscriptionType reqType) {
        log.info("Sending MarketData request");

        MarketDataRequest marketDataRequest = new MarketDataRequest(true);
        marketDataRequest.setRef(MDReqID);
        marketDataRequest.setSubscriptionType(reqType);
        marketDataRequest.setMarketDepth(0);

        String mdEntries = "0:1:2:3:4:5:7:8:B";
        String[] mdEntriesSplitted = mdEntries.split(":");


        for (String mdEntry : mdEntriesSplitted) {
            MarketDataRequest.NoMDEntryTypes noMDEntryTypes = new MarketDataRequest.NoMDEntryTypes();
            noMDEntryTypes.setChar(FixProtocol.FIELD_MD_ENTRY_TYPE, (mdEntry.charAt(0)));
            marketDataRequest.addGroup(noMDEntryTypes);
        }


        MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym();

        noRelatedSym.setString(FixProtocol.FIELD_SYMBOL, "ALL");
        marketDataRequest.addGroup(noRelatedSym);

        app.sendMessage(marketDataRequest);
    }

    public static long nextRef() {
        return ref.getAndIncrement();
    }

    //http://opalev.blogspot.com/2011/04/apache-ant-windows.html
}
