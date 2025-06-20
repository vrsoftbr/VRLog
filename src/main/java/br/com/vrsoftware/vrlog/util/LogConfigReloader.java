package br.com.vrsoftware.vrlog.util;

import br.com.vrsoftware.vrlog.LogbackConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Componente que monitora e recarrega o arquivo de configuração do log quando ele muda.
 */
public class LogConfigReloader {
    private static final Logger logger = LoggerFactory.getLogger(LogConfigReloader.class);

    private final Path configFile;
    private final WatchService watchService;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Future<?> watchTask;

    /**
     * Cria um monitor de configuração de log.
     * @param configFilePath Caminho para o arquivo de configuração
     * @throws IOException Se ocorrer um erro ao acessar o arquivo
     */
    public LogConfigReloader(String configFilePath) throws IOException {
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new IOException("Arquivo de configuração não encontrado: " + configFilePath);
        }

        this.configFile = file.toPath();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "log-config-reload-thread");
            t.setDaemon(true);
            return t;
        });

        // Registrar diretório do arquivo para monitoramento
        Path dir = this.configFile.getParent();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /**
     * Inicia o monitoramento do arquivo de configuração.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            watchTask = executor.submit(this::watchLoop);
            logger.info("Monitoramento de configuração iniciado: {}", configFile);
        }
    }

    /**
     * Para o monitoramento do arquivo de configuração.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                watchService.close();
                if (watchTask != null) {
                    watchTask.cancel(true);
                }
                executor.shutdown();
                logger.info("Monitoramento de configuração parado");
            } catch (IOException e) {
                logger.error("Erro ao parar monitoramento", e);
            }
        }
    }

    /**
     * Loop de monitoramento que detecta alterações no arquivo.
     */
    private void watchLoop() {
        try {
            while (running.get()) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Path fileName = ((WatchEvent<Path>) event).context();
                    Path fullPath = configFile.getParent().resolve(fileName);

                    // Verificar se é o arquivo que estamos monitorando
                    if (fullPath.equals(configFile)) {
                        logger.info("Detectada alteração no arquivo de configuração: {}", configFile);
                        reloadConfiguration();
                    }
                }

                // Resetar a chave para continuar recebendo eventos
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Monitoramento de configuração interrompido");
        } catch (Exception e) {
            logger.error("Erro no loop de monitoramento", e);
        }
    }

    /**
     * Recarrega a configuração do log.
     */
    private void reloadConfiguration() {
        try {
            // Aguardar um pouco para garantir que o arquivo foi completamente escrito
            Thread.sleep(500);

            Properties properties = new Properties();
            try (InputStream inputStream = Files.newInputStream(configFile.toFile().toPath())) {
                properties.load(inputStream);
            }

            // Reconfigurar o Logback
            new LogbackConfigurator(properties).configure();

            logger.info("Configuração de log recarregada com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao recarregar configuração", e);
        }
    }

    /**
     * Força uma recarga da configuração.
     */
    public void forceReload() {
        logger.info("Recarga de configuração forçada");
        reloadConfiguration();
    }
}