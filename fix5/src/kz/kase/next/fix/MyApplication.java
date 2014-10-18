package kz.kase.next.fix;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix50.ExecutionReport;
import quickfix.fix50.MarketDataRequest;
import quickfix.fix50.SecurityList;
import quickfix.fixt11.Logon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MyApplication implements Application {

    public static final int FIELD_ORDER_SERIAL = 5181;
    public static final int FIELD_DEAL_SERIAL = 5185;
    public static final int FIELD_CASH_QTY = 152;
    public static final int FIELD_TRANSACTION_TIME = 60;
    public static final int FIELD_ACCOUNT_NAME = 448;
    public static final int FIELD_ORDER_ID = 37;
    public static final int FIELD_USERNAME = 553;
    public static final int FIELD_SETTLEMENT_DATE = 64;
    public static final int FIELD_NIN = 5032;

    private final CountDownLatch logonLatch = new CountDownLatch(1);
    private HashMap<String, String> ninsBySymbol;

    public MyApplication() {
        ninsBySymbol = new HashMap<>();
    }

    private Session session;

    @Override
    public void onCreate(SessionID sessionID) {
    }

    @Override
    public void onLogon(SessionID sessionID) {
        System.out.println("Logged in: " + sessionID);
        this.session = Session.lookupSession(sessionID);
        logonLatch.countDown();
    }

    @Override
    public void onLogout(SessionID sessionID) {
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        if (message instanceof Logon) {
            Logon logon = (Logon) message;
            logon.setField(new Username(sessionID.getSenderCompID()));
            logon.setField(new Password("12345"));
            logon.setField(new EncryptMethod(EncryptMethod.NONE_OTHER));
            logon.setField(new ResetSeqNumFlag(ResetSeqNumFlag.YES_RESET_SEQUENCE_NUMBERS));
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID)
            throws IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, FieldNotFound {

        if (message instanceof SecurityList) {

            MarketDataRequest mdReq = new MarketDataRequest();
            mdReq.setField(new MDReqID(String.valueOf(SampleClient.nextRef())));
            mdReq.setField(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
            mdReq.setField(new MarketDepth(0));
            mdReq.setField(new NoMDEntryTypes(0));

            SecurityList securityList = (SecurityList) message;

            List<Group> securities = securityList.getGroups(NoRelatedSym.FIELD);

            for (Group s : securities) {
                MarketDataRequest.NoRelatedSym instrument = new MarketDataRequest.NoRelatedSym();
                instrument.setString(Symbol.FIELD, s.getString(Symbol.FIELD));

                mdReq.addGroup(instrument);
                ninsBySymbol.put(s.getString(Symbol.FIELD), s.getString(FIELD_NIN));
            }

            sendMessage(mdReq);

        } else if (message instanceof ExecutionReport) {
            ExecutionReport exRep = (ExecutionReport) message;

            if (exRep.getChar(ExecType.FIELD) == ExecType.TRADE) {
                try {

                    File dealFile = new File("deals/"+getField(exRep, FIELD_ORDER_SERIAL) + "-" + getField(exRep, FIELD_DEAL_SERIAL));

                    FileWriter fw = new FileWriter(dealFile.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fw);

                    String dealInfo = "";
                    try {

                        dealInfo += "Deal_DealId=" + getField(exRep, FIELD_DEAL_SERIAL) + "\n";
                        dealInfo += "Deal_ShortName=" + getField(exRep, Symbol.FIELD) + "\n";
                        dealInfo += "Deal_BS=" + getField(exRep, Side.FIELD) + "\n";
                        dealInfo += "Deal_Price=" + getField(exRep, Price.FIELD) + "\n";
                        dealInfo += "Deal_Volume=" + getField(exRep, FIELD_CASH_QTY) + "\n";

                        DateFormat dateFormatter1 = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
                        DateFormat dateFormatter2 = new SimpleDateFormat("yyyyMMdd");

                        Date d = dateFormatter1.parse(exRep.getString(FIELD_TRANSACTION_TIME));
                        Date s = dateFormatter2.parse(exRep.getString(FIELD_SETTLEMENT_DATE));

                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        DateFormat tf = new SimpleDateFormat("HH:mm:ss");

                        dealInfo += "Deal_Date=" + df.format(d) + "\n";
                        dealInfo += "Deal_Time=" + tf.format(d) + "\n";
                        dealInfo += "Deal_TrdAcc=" + getField(exRep, FIELD_ACCOUNT_NAME) + "\n";
                        dealInfo += "Deal_OrderId=" + getField(exRep, FIELD_ORDER_ID) + "\n";
                        dealInfo += "Deal_NIN=" + ninsBySymbol.get(getField(exRep, Symbol.FIELD)) + "\n";
                        dealInfo += "Deal_OrderNum=" + getField(exRep, FIELD_ORDER_SERIAL) + "\n";
                        dealInfo += "Deal_UserNick=" + getField(exRep, FIELD_USERNAME) + "\n";
                        dealInfo += "Deal_SettlDate=" + df.format(s) + "\n";

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    bw.write(dealInfo);
                    bw.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getField(Message msg, int field) {
        String r = "";
        try {
            if (msg.isSetField(field))
                r = msg.getString(field);
        } catch (FieldNotFound fieldNotFound) {
            fieldNotFound.printStackTrace();
        }
        return r;
    }

    public void awaitLogon() throws InterruptedException {
        logonLatch.await();
    }

    public void sendMessage(Message message) {
        if (session != null) {
            session.send(message);
        }
    }
}
