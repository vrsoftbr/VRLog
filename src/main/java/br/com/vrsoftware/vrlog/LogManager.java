package br.com.vrsoftware.vrlog;

import br.com.vrsoftware.vrlog.util.LogCompressor;
import br.com.vrsoftware.vrlog.util.LogEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Classe principal da biblioteca de logging que gerencia a configuração e manutenção dos logs.
 */
public class LogManager {

    private static LogManager instance;
    private final Properties properties;
    private final String logPath;
    private final ScheduledExecutorService scheduler;

    /**
     * Inicializa o gerenciador de logs com o arquivo de propriedades especificado.
     *
     * @param propertiesFile Caminho para o arquivo de propriedades
     * @return A instância do LogManager
     */
    public static synchronized LogManager initialize(String propertiesFile) {
        if (instance == null) instance = new LogManager(propertiesFile);
        return instance;
    }

    /**
     * Inicializa o gerenciador de logs com as propriedades fornecidas.
     *
     * @param properties Propriedades de configuração do log
     * @return A instância do LogManager
     */
    public static synchronized LogManager initialize(Properties properties) {
        if (instance == null) instance = new LogManager(properties);
        return instance;
    }

    /**
     * Obtém uma instância do logger para a classe especificada.
     *
     * @param clazz Classe para a qual obter o logger
     * @return Logger configurado para a classe
     */
    public static Logger getLogger(Class<?> clazz) {
        if (instance == null) throw new IllegalStateException("LogManager não foi inicializado. Chame initialze() primeiro.");
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Obtém uma instância do logger para o nome especificado.
     *
     * @param name Nome para o qual obter o logger
     * @return Logger configurado para o nome
     */
    public static Logger getLogger(String name) {
        if (instance == null) throw new IllegalStateException("LogManager não foi inicializado. Chame initialze() primeiro.");
        return LoggerFactory.getLogger(name);
    }

    /**
     * Obtém a instância única do LogManager.
     *
     * @return A instância do LogManager ou null se não inicializado
     */
    public static LogManager getInstance() {
        return instance;
    }

    /**
     * Obtém as propriedades de configuração atuais.
     *
     * @return Propriedades de configuração
     */
    public Properties getProperties() {
        return new Properties(properties);
    }

    private LogManager(String propertiesFile) {
        this.properties = new Properties();
        try (InputStream inputStream = new File(propertiesFile).exists() ? Files.newInputStream(Paths.get(propertiesFile)) : getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
            if (inputStream == null) throw new IOException("Arquivo de propeties não encontrado: " + propertiesFile);
            properties.load(inputStream);
        } catch (Exception e) {
            System.err.println("Erro ao carregar arquivo de proprieades: " + e.getMessage());
        }

        //Configurar o Logback
        new LogbackConfigurator(properties).configure();

        //Configurar envio de email se habiliado
        LogEmailSender.configureFromProperties(properties);

        this.logPath = properties.getProperty("log.file.path", "./logs");

        this.scheduler = Executors.newScheduledThreadPool(1);
        startDailyMaintenanceTask();

    }

    private LogManager(Properties properties) {
        this.properties = properties;

        //Configurar o Logback
        new LogbackConfigurator(properties).configure();

        //Configurar envio de email se habiliado
        LogEmailSender.configureFromProperties(properties);

        this.logPath = properties.getProperty("log.file.path", "./logs");

        this.scheduler = Executors.newScheduledThreadPool(1);
        startDailyMaintenanceTask();
    }

    /**
     * Inicia a tarefa de manutenção diária dos logs.
     */
    private void startDailyMaintenanceTask() {
        //Executar à meia-noite todos os dias
        this.scheduler.scheduleAtFixedRate(this::performMaintenance, calculateInitiDelay(), 24, TimeUnit.HOURS);
    }

    /**
     * Calcula o delay inicial para a primeira execução à meia-noite
     */
    private long calculateInitiDelay() {
        return 0;
    }

    /**
     * Realiza manutenção dos arquivos de log
     */
    private void performMaintenance() {
        try {

            // Comprimir logs do dia anterior
            LocalDate ontem = LocalDate.now().minusDays(1);

            // Verificar se a compactação está habilitada
            if (Boolean.parseBoolean(properties.getProperty("log.archive", "true"))) LogCompressor.compressLogsByDate(Paths.get(logPath), ontem);
            LogCompressor.cleanupOldLogs(Paths.get(logPath), Integer.parseInt(properties.getProperty("log.file.maxHistory", "15")));

            // Log da operação de manutençãoF
            Logger logger = LoggerFactory.getLogger(LogManager.class);
            logger.info("Manutenção diária de logs concluída");

        } catch (Exception e) {
            System.err.println("Erro na manutenção diária de logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) scheduler.shutdownNow();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}