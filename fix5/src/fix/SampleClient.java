package fix;

import kz.kase.fix.SecurityListRequestType;
import kz.kase.fix.factory.KaseFixMessageFactory;
import kz.kase.fix.messages.OrderStatusRequest;
import kz.kase.fix.messages.SecurityListRequest;
import quickfix.*;
import quickfix.logging.LogFactory;
import quickfix.logging.ScreenLogFactory;
import quickfix.store.FileStoreFactory;
import quickfix.store.MessageStoreFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

public class SampleClient {

    public static File getDealsDir() {
        return DEALS_DIR;
    }

    public static File getOrdersDir() {
        return ORDERS_DIR;
    }

    public static File getInstrDir() {
        return INSTR_DIR;
    }

    private static final File ORDERS_DIR = new File("files.order");
    private static final File DEALS_DIR = new File("files.deal");
    private static final File INSTR_DIR = new File("files.inst");

    private static final String FIX_CFG = "fix.cfg";
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

        SampleClient client = new SampleClient();

        client.securityListReq();
        client.orderStatusReq();

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

/*        TradeCaptureReportRequest trdReq = new TradeCaptureReportRequest();
        trdReq.setTradeReqId("0");
        trdReq.setTradeReqType(0);
        trdReq.setSymbol("0");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        try {

            Date parsedDate = formatter.parse("20140916");

            trdReq.setFromDate(parsedDate);
            trdReq.setTillDate(new Date());

            app.sendMessage(trdReq);

        } catch (ParseException e) {
            e.printStackTrace();
        }*/

        OrderStatusRequest ordReq = new OrderStatusRequest();
        ordReq.setRef(nextRef());
        app.sendMessage(ordReq);

    }

    public static long nextRef() {
        return new Random(System.currentTimeMillis()).nextInt(REF_RAND_SEED);
    }

    //http://opalev.blogspot.com/2011/04/apache-ant-windows.html
}
