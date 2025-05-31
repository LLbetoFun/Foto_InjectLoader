package com.fun.network.logger;

import com.fun.network.handlers.LoggerConnection;
import com.fun.network.model.DataModel;
import org.apache.logging.log4j.LogManager;

import java.net.InetSocketAddress;

public class Logger {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger("Foto");

    public static void info(String msg) {
        logger.info(msg);
        LoggerConnection.connection.writeMessage(new DataModel(DataModel.Type.INFO,msg));
    }
    public static void warn(String msg) {
        logger.warn(msg);
        LoggerConnection.connection.writeMessage(new DataModel(DataModel.Type.WARN,msg));
    }
    public static void error(String msg) {
        logger.error(msg);
        LoggerConnection.connection.writeMessage(new DataModel(DataModel.Type.ERROR,msg));
    }
    public static void debug(String msg) {
        logger.debug(msg);
        LoggerConnection.connection.writeMessage(new DataModel(DataModel.Type.DEBUG,msg));
    }
    public static void unwrap(DataModel packet) {
        switch (packet.type) {
            case INFO -> {
                logger.info(packet.msg);
            }
            case WARN -> {
                logger.warn(packet.msg);
            }
            case ERROR -> {
                logger.error(packet.msg);
            }
            case DEBUG -> {
                logger.debug(packet.msg);
            }
        }
    }

    public static void main(String[] args) {
        LoggerConnection.connect(new InetSocketAddress("localhost",13337));
        Logger.info("aaaaa");
    }
}
