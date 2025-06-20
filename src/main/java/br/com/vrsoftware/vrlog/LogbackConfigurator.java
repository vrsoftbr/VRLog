package br.com.vrsoftware.vrlog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Classe responsável por configurar o Logback programaticamente
 * com base nas propriedades fornecidas.
 */
public class LogbackConfigurator {
    private final Properties properties;
    private final LoggerContext context;

    // Mapeamento de strings de nível para enum Level do Logback
    private static final Map<String, Level> LEVEL_MAP = new HashMap<>();

    static {
        LEVEL_MAP.put("TRACE", Level.TRACE);
        LEVEL_MAP.put("DEBUG", Level.DEBUG);
        LEVEL_MAP.put("INFO", Level.INFO);
        LEVEL_MAP.put("WARN", Level.WARN);
        LEVEL_MAP.put("ERROR", Level.ERROR);
        LEVEL_MAP.put("OFF", Level.OFF);
    }

    public LogbackConfigurator(Properties properties) {
        this.properties = properties;
        this.context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
    }

    /**
     * Configura o Logback de acordo com as propriedades.
     */
    public void configure() {
        // Configura o logger raiz
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        // Define o nível de log padrão
        String defaultLevel = properties.getProperty("log.level", "INFO");
        rootLogger.setLevel(getLevel(defaultLevel));

        // Configura o padrão de layout
        String pattern = properties.getProperty("log.pattern",
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");

        // Adiciona appender de console se habilitado
        if (Boolean.parseBoolean(properties.getProperty("log.console", "true"))) {
            rootLogger.addAppender(createConsoleAppender(pattern));
        }

        // Adiciona appender de arquivo se o caminho estiver definido
        String filePath = properties.getProperty("log.file.path");
        String fileName = properties.getProperty("log.file.name", "application.log");

        if (filePath != null && !filePath.isEmpty()) {
            rootLogger.addAppender(createFileAppender(pattern, filePath, fileName));
        }

        // Configura loggers específicos por pacote
        properties.forEach((key, value) -> {
            String keyStr = (String) key;
            if (keyStr.startsWith("log.level.")) {
                String packageName = keyStr.substring("log.level.".length());
                Logger logger = context.getLogger(packageName);
                logger.setLevel(getLevel((String) value));
                // Não propagar para o logger raiz para evitar duplicação de logs
                logger.setAdditive(false);

                // Adicionar os mesmos appenders do root logger
                rootLogger.iteratorForAppenders().forEachRemaining(logger::addAppender);
            }
        });
    }

    /**
     * Cria um appender de console.
     */
    private ConsoleAppender<ILoggingEvent> createConsoleAppender(String pattern) {

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(context);
        appender.setName("CONSOLE");
        appender.setEncoder(getEncoder(pattern));
        appender.start();

        return appender;
    }

    /**
     * Cria um appender de arquivo com rolagem.
     */
    private RollingFileAppender<ILoggingEvent> createFileAppender(String pattern, String filePath, String fileName) {

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("FILE");
        appender.setEncoder(getEncoder(pattern));

        // Cria diretório se não existir
        File logDir = new File(filePath);
        if (!logDir.exists()) logDir.mkdirs();

        // Define o caminho completo do arquivo de log
        appender.setFile(new File(logDir, fileName).getAbsolutePath());

        // Configuração de rolagem por tamanho e tempo
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern(
                new File(logDir, fileName + ".%d{yyyy-MM-dd}.%i.zip").getAbsolutePath());

        // Configura tamanho máximo do arquivo
        String maxSize = properties.getProperty("log.file.maxSize", "10MB");
        rollingPolicy.setMaxFileSize(FileSize.valueOf(maxSize));

        // Configuração de histórico
        int maxHistory = Integer.parseInt(properties.getProperty("log.file.maxHistory", "30"));
        rollingPolicy.setMaxHistory(maxHistory);

        rollingPolicy.start();
        appender.setRollingPolicy(rollingPolicy);
        appender.start();

        return appender;
    }

    private PatternLayoutEncoder getEncoder(String pattern) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(pattern);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
        return encoder;
    }

    /**
     * Converte uma string de nível para o enum Level do Logback.
     */
    private Level getLevel(String levelStr) {
        Level level = LEVEL_MAP.get(levelStr.toUpperCase());
        return level != null ? level : Level.INFO;
    }
}