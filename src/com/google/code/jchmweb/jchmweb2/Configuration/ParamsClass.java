package com.google.code.jchmweb.jchmweb2.Configuration;

import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * The class with the parameters.
 * @author AKhusnutdinov
 */
public class ParamsClass {

    public ParamsClass() {
        PropertyConfigurator.configure(props());
        ParamsClass.logger = Logger.getRootLogger();
    }
    public static Logger logger = null;

    private static Properties props() {
        Properties props = new Properties();
        props.put("log4j.rootLogger", "INFO, R");
        props.put("log4j.appender.R",
                "org.apache.log4j.DailyRollingFileAppender");
        props.put("log4j.appender.R.File", "logs/jchmweb.log");
        props.put("log4j.appender.R.Append", "true");
        props.put("log4j.appender.R.Threshold", "INFO");
        props.put("log4j.appender.R.DatePattern", "'.'yyyy-MM-dd");
        props.put("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.R.layout.ConversionPattern",
                //"%d{HH:mm:ss,SSS} %c - %m%n");
                "[%5p] %d{yyyy-MM-dd mm:ss} (%F:%M:%L)%n%m%n%n");
                //"[%5p] %d{yyyy-MM-dd mm:ss} %c (%F:%M:%L)%n");
        return props;
    }
}
