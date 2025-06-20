package br.com.vrsoftware.vrlog.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.boolex.OnMarkerEvaluator;
import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.spi.CyclicBufferTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Properties;

/**
 * Utilitário para envio de logs por email.
 * Permite configurar Logback para enviar logs por email quando ocorrem erros críticos.
 */
public class LogEmailSender {

    // Marker para identificar logs que devem ser enviados por email
    public static final Marker EMAIL_MARKER = MarkerFactory.getMarker("EMAIL");

    /**
     * Configura um appender para envio de logs por email.
     *
     * @param smtpHost     Servidor SMTP
     * @param smtpPort     Porta do servidor SMTP
     * @param smtpUsername Usuário SMTP
     * @param smtpPassword Senha SMTP
     * @param useSSL       Usar SSL para conexão
     * @param from         Endereço de email remetente
     * @param to           Lista de emails destinatários (separados por vírgula)
     * @param subject      Assunto do email
     * @param minLevel     Nível mínimo de log para enviar email (ERROR, WARN, etc)
     */
    public static void configureEmailAppender(
            String smtpHost, int smtpPort, String smtpUsername, String smtpPassword,
            boolean useSSL, String from, String to, String subject, Level minLevel) {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        SMTPAppender appender = new SMTPAppender();

        // Configuração básica
        appender.setContext(context);
        appender.setName("EMAIL");
        appender.setFrom(from);
        appender.addTo(to);
        appender.setSubject(subject);
        appender.setSMTPHost(smtpHost);
        appender.setSMTPPort(smtpPort);

        // Autenticação SMTP se necessário
        if (smtpUsername != null && !smtpUsername.isEmpty()) {
            appender.setUsername(smtpUsername);
            appender.setPassword(smtpPassword);
        }

        // SSL se necessário
        appender.setSTARTTLS(useSSL);

        // Layout do email
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%date %-5level [%thread] %logger{36} - %msg%n");
        layout.start();
        appender.setLayout(layout);

        // Configurar buffer cíclico (armazena logs até que seja disparado um evento)
        appender.setCyclicBufferTracker(new CyclicBufferTracker<>());
//        appender.setCyclic(true);

        // Configurar avaliador de eventos - quando enviar emails
        EventEvaluator<ILoggingEvent> evaluator = null;

        // Opção 1: Enviar com base no nível de log
        if (minLevel != null) {
            //evaluator = event -> event.getLevel().isGreaterOrEqual(minLevel);}
        }
        // Opção 2: Enviar com base no marcador
        else {
            OnMarkerEvaluator markerEvaluator = new OnMarkerEvaluator();
            markerEvaluator.setContext(context);
            markerEvaluator.addMarker(EMAIL_MARKER.getName());
            markerEvaluator.start();
            evaluator = markerEvaluator;
        }

        if (evaluator != null) appender.setEvaluator(evaluator);

        // Iniciar o appender
        appender.start();

        // Adicionar ao logger raiz
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
    }

    /**
     * Configura envio de email com base em propriedades.
     *
     * @param properties Propriedades de configuração
     */
    public static void configureFromProperties(Properties properties) {
        if (Boolean.parseBoolean(properties.getProperty("log.email.enabled", "false"))) {
            String host = properties.getProperty("log.email.smtp.host", "localhost");
            int port = Integer.parseInt(properties.getProperty("log.email.smtp.port", "25"));
            String username = properties.getProperty("log.email.smtp.username", "");
            String password = properties.getProperty("log.email.smtp.password", "");
            boolean useSSL = Boolean.parseBoolean(properties.getProperty("log.email.smtp.ssl", "false"));
            String from = properties.getProperty("log.email.from", "application@example.com");
            String to = properties.getProperty("log.email.to", "admin@example.com");
            String subject = properties.getProperty("log.email.subject", "Log Alert");

            Level minLevel = null;
            String levelStr = properties.getProperty("log.email.level", "ERROR");
            switch (levelStr.toUpperCase()) {
                case "ERROR":
                    minLevel = Level.ERROR;
                    break;
                case "WARN":
                    minLevel = Level.WARN;
                    break;
                case "INFO":
                    minLevel = Level.INFO;
                    break;
                case "DEBUG":
                    minLevel = Level.DEBUG;
                    break;
                case "TRACE":
                    minLevel = Level.TRACE;
                    break;
                case "OFF":
                    minLevel = Level.OFF;
                    break;
                default:
                    minLevel = Level.ERROR;
            }

            configureEmailAppender(host, port, username, password, useSSL, from, to, subject, minLevel);
        }
    }
}