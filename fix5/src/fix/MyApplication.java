package fix;

import kz.kase.fix.EncryptMethod;
import kz.kase.fix.ExecType;
import kz.kase.fix.FixProtocol;
import kz.kase.fix.messages.ExecutionReport;
import kz.kase.fix.messages.Logon;
import kz.kase.fix.messages.SecurityList;
import quickfix.*;

import java.io.*;
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
    public static final String PASS = "Password";
    public static final String DEALS_FOLDER = "deals/";

    private final CountDownLatch logonLatch = new CountDownLatch(1);
    private HashMap<String, String> ninsBySymbol;

    private final SessionSettings settings;

    public MyApplication(SessionSettings settings) {
        this.settings = settings;
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
            logon.setUsername(sessionID.getSenderCompID());

            try {
                logon.setPassword(settings.getString(PASS));
            } catch (ConfigError | FieldConvertError configError) {
                configError.printStackTrace();
            }

            logon.setEncryptMethod(EncryptMethod.NONE_OTHER);
            logon.setResetSeqNum(true);
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
            throws IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

        if (message instanceof SecurityList) {

            SecurityList secList = (SecurityList) message;
            List<Group> secLst = secList.getGroups(FixProtocol.FIELD_NO_RELATED_SYM);

            for (Group secGrp : secLst) {
                try {
                    String symbol = secGrp.getString(FixProtocol.FIELD_SYMBOL);

                    File instrFile =
                            new File(SampleClient.getInstrDir() + "/" + symbol);

                    FileWriter fw = new FileWriter(instrFile.getAbsoluteFile(), false);
                    BufferedWriter bw = new BufferedWriter(fw);

                    String secId = secGrp.getString(FixProtocol.FIELD_SECURITY_ID);
                    String prod = secGrp.getString(FixProtocol.FIELD_PRODUCT);
                    String secDesc = fromHexString(secGrp.getString(FixProtocol.FIELD_SECURITY_DESC));

                    String instrInfo = "";
                    instrInfo += "Instrument_Id=" + secId + "\n"
                            + "Instrument_Prod=" + prod + "\n"
                            + "Instrument_ShortName=" + symbol + "\n"
                            + "Instrument_Name=" + secDesc + "\n";

                    bw.write(instrInfo);
                    bw.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (message instanceof ExecutionReport) {

            ExecutionReport exRep = (ExecutionReport) message;

            if (exRep.getExecType() == ExecType.TRADE) {
                try {

                    String exId = exRep.getExecId();

                    if(exId==null || exId.equals("")) return;

                    File dealFile = new File(SampleClient.getDialsDir() +"/"+ exId);

                    if (dealFile.exists()) return;

                    FileWriter fw = new FileWriter(dealFile.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fw);

                    String dealInfo = "";

                    try {

                        dealInfo += "Deal_DealId=" + exRep.getExecId() + "\n";
                        dealInfo += "Deal_ShortName=" + exRep.getInstrSymbol() + "\n";
                        dealInfo += "Deal_BS=" + exRep.getSide() + "\n";
                        dealInfo += "Deal_Price=" + exRep.getPrice() + "\n";
                        dealInfo += "Deal_Volume=" + exRep.getOrderCashQty() + "\n";

                        DateFormat dateFormatter1 = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
                        DateFormat dateFormatter2 = new SimpleDateFormat("yyyyMMdd");

                        Date d = dateFormatter1.parse(exRep.getString(FIELD_TRANSACTION_TIME));
                        Date s = dateFormatter2.parse(exRep.getString(FIELD_SETTLEMENT_DATE));

                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        DateFormat tf = new SimpleDateFormat("HH:mm:ss");

                        dealInfo += "Deal_Date=" + df.format(d) + "\n";
                        dealInfo += "Deal_Time=" + tf.format(d) + "\n";
                        dealInfo += "Deal_TrdAcc=" + exRep.getAccount() + "\n";
                        dealInfo += "Deal_UserNick=" + exRep.getUserName() + "\n";
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

    public void awaitLogon() throws InterruptedException {
        logonLatch.await();
    }

    public void sendMessage(Message message) {
        if (session != null) {
            session.send(message);
        }
    }

    public static String fromHexString(String input) {
        int n = input.length() / 2;
        byte[] output = new byte[n];
        int l = 0;
        for (int k = 0; k < n; k++) {
            char c = input.charAt(l++);
            byte b = (byte) ((c >= 'a' ? (c - 'a' + 10) : (c - '0')) << 4);
            c = input.charAt(l++);
            b |= (byte) (c >= 'a' ? (c - 'a' + 10) : (c - '0'));
            output[k] = b;
        }
        try {
            return new String(output, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
