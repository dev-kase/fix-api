package fix;

import kz.kase.fix.EncryptMethod;
import kz.kase.fix.ExecType;
import kz.kase.fix.FixProtocol;
import kz.kase.fix.TimeInForce;
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

import static kz.kase.fix.FixProtocol.*;
import static kz.kase.fix.FixProtocol.FIELD_PRODUCT;
import static kz.kase.fix.FixProtocol.FIELD_SECURITY_ID;

public class MyApplication implements Application {

    public static final int FIELD_TRANSACTION_TIME = 60;
    public static final int FIELD_SETTLEMENT_DATE = 64;
    public static final String PASS = "Password";

    public static final DateFormat dateFormatter1 = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
    public static final DateFormat dateFormatter2 = new SimpleDateFormat("yyyyMMdd");
    public static final DateFormat dateFormatter3 = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat dateFormatter4 = new SimpleDateFormat("HH:mm:ss");

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
            List<Group> secLst = secList.getGroups(FIELD_NO_RELATED_SYM);

            for (Group secGrp : secLst) {
                try {
                    String symbol = secGrp.getString(FIELD_SYMBOL);

                    File instrFile =
                            new File(SampleClient.getInstrDir() + "/" + symbol);

                    FileWriter fw = new FileWriter(instrFile.getAbsoluteFile(), false);
                    BufferedWriter bw = new BufferedWriter(fw);

                    String secId = secGrp.getString(FIELD_SECURITY_ID);
                    String prod = secGrp.getString(FIELD_PRODUCT);
                    String secDesc = fromHexString(secGrp.getString(FIELD_SECURITY_DESC));

/*                    TimeInForce ti = TimeInForce.valueOf(secGrp.getChar(FIELD_TIME_IN_FORCE));
                    int minTradeVol = secGrp.getInt(FIELD_MIN_TRADE_VOL);
                    int maxTradeVol = secGrp.getInt(FIELD_MAX_TRADE_VOL);
                    double maxPrcVariation = secGrp.getDouble(FIELD_MAX_PRICE_VARIATION);
                    String trCurr = secGrp.getString(FIELD_TRADING_CURRENCY);
                    int lot = secGrp.getInt(FIELD_ROUND_LOT);
                    int securityStatus = secGrp.getInt(FIELD_SECURITY_STATUS);
                    String text = secGrp.getString(FIELD_TEXT);
                    Long nominal = secGrp.getLong(FIELD_NOMINAL_VALUE);
                    double limDevAvg = secGrp.getDouble(FIELD_DEV_LIMIT_AVG_PRICE_VALUE);
                    double limMarketPrc = secGrp.getInt(FIELD_DEV_LIM_MARKET_PRC_VALUE);
                    double step = secGrp.getDouble(FIELD_MIN_PRICE_INCREMENT);
                    Date matDate = secGrp.getUtcDateOnly(FIELD_MATURITY_DATE);
                    boolean isFutures = secGrp.getBoolean(FIELD_IS_FUTURE);

                    String legs = "Instrument_Repo_Legs:\n";
                    if (!secGrp.getBoolean(FIELD_NO_LEGS)) {
                        Group leg1 = secGrp.getGroup(1, FIELD_NO_LEGS);
                        legs += "\tLegs1=";
                        legs += leg1.getString(FIELD_LEG_SYMBOL) + "\n";

                        Group leg2 = secGrp.getGroup(2, FIELD_NO_LEGS);
                        legs += "\tLegs2=";
                        legs += leg2.getString(FIELD_LEG_SYMBOL) + "\n";
                    }*/


                    String instrInfo = "";
                    instrInfo += "Instrument_Id=" + secId + "\n"
                            + "Instrument_Prod=" + prod + "\n"
                            + "Instrument_ShortName=" + symbol + "\n"
                            + "Instrument_FullName=" + secDesc + "\n"

/*                            + "Instrument_TimeInForce=" + ti + "\n"
                            + "Instrument_MaxTradeVol=" + maxTradeVol + "\n"
                            + "Instrument_MinTradeVol=" + minTradeVol + "\n"
                            + "Instrument_MaxPrcVariation" + maxPrcVariation + "\n"
                            + "Instrument_TradingCurrency=" + trCurr + "\n"
                            + "Instrument_Lot=" + lot + "\n"
                            + "Instrument_SecurityStatus=" + securityStatus + "\n"
                            + "Instrument_AllowedSides=" + text + "\n"
                            + "Instrument_Nominal=" + nominal + "\n"
                            + "Instrument_LimDeviationFromAvgPrc=" + limDevAvg + "\n"
                            + "Instrument_LimDeviationFromMarketPrc=" + limMarketPrc + "\n"
                            + "Instrument_PriceStep=" + step + "\n"
                            + "Instrument_MaturityDate=" + matDate + "\n"
                            + "Instrument_IsFutures=" + isFutures + "\n"
                            + legs
*/
                    ;

                    bw.write(instrInfo);
                    bw.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (message instanceof ExecutionReport) {

            ExecutionReport report = (ExecutionReport) message;
            if (report.getExecType() == ExecType.TRADE) {
                writeDeal(report);
            } else if (report.getExecType() == ExecType.ORDER_STATUS) {
                writeOrder(report);
            }
        }
    }

    private void writeDeal(ExecutionReport report) {
        try {

            String exId = report.getExecId();

            if (exId == null || exId.equals("")) return;

            File dealFile = new File(SampleClient.getDealsDir() + "/" + exId);

            if (dealFile.exists()) return;

            FileWriter fw = new FileWriter(dealFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            String dealInfo = "";

            try {

                dealInfo += "Deal_DealId=" + report.getExecId() + "\n";
                dealInfo += "Deal_ShortName=" + report.getInstrSymbol() + "\n";
                dealInfo += "Deal_BS=" + report.getSide() + "\n";
                dealInfo += "Deal_Price=" + report.getPrice() + "\n";
                dealInfo += "Deal_Volume=" + report.getOrderCashQty() + "\n";

                Date d = dateFormatter1.parse(report.getString(FIELD_TRANSACTION_TIME));
                Date s = dateFormatter2.parse(report.getString(FIELD_SETTLEMENT_DATE));

                dealInfo += "Deal_Date=" + dateFormatter3.format(d) + "\n";
                dealInfo += "Deal_Time=" + dateFormatter4.format(d) + "\n";
                dealInfo += "Deal_TrdAcc=" + report.getAccount() + "\n";
                dealInfo += "Deal_UserNick=" + report.getUserName() + "\n";
                dealInfo += "Deal_SettlDate=" + dateFormatter3.format(s) + "\n";
                dealInfo += "Deal_OrderStatus=" + report.getOrderStatus() + "\n";
                dealInfo += "Deal_OrderId=" + report.getOrderId() + "\n";
                dealInfo += "Deal_DealType=" + report.getDealType() + "\n"; //REGULAR_DEAL=Обычная, SWAP_DEAL=Своп, SWAP_LEG_DEAL=Нога свопа, REPO_DEAL=Репо

            } catch (Exception e) {
                e.printStackTrace();
            }

            bw.write(dealInfo);
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeOrder(ExecutionReport report) {
        try {

            String exId = report.getOrderId();

            if (exId == null || exId.equals("")) return;

            File orderFile = new File(SampleClient.getOrdersDir() + "/" + exId);

            if (orderFile.exists()) return;

            FileWriter fw = new FileWriter(orderFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            String orderInfo = "";
            String sessId, whoRemv, resrict;
            Date remTime;

            try {

                orderInfo += "Order_OderId=" + report.getOrderId() + "\n";
                orderInfo += "Order_InstrSymbol=" + report.getInstrSymbol() + "\n";
                orderInfo += "Order_Account=" + report.getAccount() + "\n";
                orderInfo += "Order_Side=" + report.getSide() + "\n";
                orderInfo += "Order_Price=" + report.getPrice() + "\n";
                orderInfo += "Order_UserName=" + report.getUserName() + "\n";
                orderInfo += "Order_QuantityOfInstrs=" + report.getQty() + "\n";
                orderInfo += "Order_ExpireDate=" + report.getExpireDate() + "\n";
                orderInfo += "Order_TimeInForce=" + TimeInForce.valueOf(report.getTimeInForce()) + "\n";
                orderInfo += "Order_Comment=" + report.getComment() + "\n";
                orderInfo += "Order_LeavesQty=" + report.getLeavesQty() + "\n";
                orderInfo += "Order_OrderCashQty=" + report.getOrderCashQty() + "\n";
                orderInfo += "Order_OrderStatus=" + report.getOrderStatus() + "\n";
                orderInfo += "Order_TransactionTime=" + report.getTransactionTime() + "\n";
                whoRemv = report.hasWhoRemoved() ? report.getWhoRemoved() : "";
                orderInfo += "Order_WhoRemoved=" + whoRemv + "\n";
                remTime = report.hasRemovedTime() ? report.getRemovedTime() : new Date(0);
                orderInfo += "Order_RemovedTime=" + remTime + "\n";
                sessId = report.getSessionId() == null ? "" : report.getSessionId();
                orderInfo += "Order_SessionId=" + sessId + "\n";
                resrict = report.hasOrderRestrictions() ? report.getOrderRestrictions() : "";
                orderInfo += "Order_OrderRestrictions=" + resrict + "\n";

            } catch (Exception e) {
                e.printStackTrace();
            }

            bw.write(orderInfo);
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
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
