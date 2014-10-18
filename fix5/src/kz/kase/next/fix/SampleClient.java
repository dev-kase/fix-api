package kz.kase.next.fix;


import quickfix.*;
import quickfix.field.SecurityListRequestType;
import quickfix.field.SecurityReqID;
import quickfix.fix50.SecurityListRequest;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

public class SampleClient {


    public static void main(String[] args) throws Exception {

        String configFile = "fix.cfg";

        File theDir = new File("deals");

        if (!theDir.exists()) {
            System.out.println("Creating directory: deals");
            boolean result = false;

            try{
                theDir.mkdir();
                result = true;
            } catch(SecurityException se){

            }
            if(result) {
                System.out.println("Directory created");
            }
        }

        //Настройки сессии берем из конфиг-файла
        SessionSettings settings = new SessionSettings(new FileInputStream(configFile));

        MyApplication app = new MyApplication();
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true, false);
        MessageFactory mesFactory = new DefaultMessageFactory();
        SocketInitiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, mesFactory);
        initiator.start();

        //ждем успешной авторизации
        app.awaitLogon();

        System.out.println("Sending security-list-request...");
        SecurityListRequest secReq = new SecurityListRequest();

        secReq.setField(new SecurityReqID(String.valueOf(nextRef())));
        secReq.setField(new SecurityListRequestType(SecurityListRequestType.ALL_SECURITIES));

        app.sendMessage(secReq);

        while(true);

      //  initiator.stop();
    }

    public static long nextRef() {
        return new Random(System.currentTimeMillis()).nextInt(100000);
    }
}
