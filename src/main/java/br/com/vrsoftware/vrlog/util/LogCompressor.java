package br.com.vrsoftware.vrlog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilitário para compressão de arquivos de log.
 * Esta classe pode ser utilizada independentemente para compactar logs.
 */
public class LogCompressor {

    private static final Logger logger = LoggerFactory.getLogger(LogCompressor.class);

    /**
     * Comprime todos os arquivos de log de uma data específica.
     * @param logDirectory Diretório onde estão os logs
     * @param date Data no formato LocalDate
     * @throws IOException Em caso de erro no acesso aos arquivos
     */
    public static void compressLogsByDate(Path logDirectory, LocalDate date) throws IOException {
        String datePattern = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        compressLogsByPattern(logDirectory, datePattern);
    }

    /**
     * Comprime todos os arquivos de log que correspondem a um padrão.
     * @param logDirectory Diretório onde estão os logs
     * @param pattern Padrão para corresponder nos nomes de arquivo
     * @throws IOException Em caso de erro no acesso aos arquivos
     */
    public static void compressLogsByPattern(Path logDirectory, String pattern) throws IOException {
        if (!Files.exists(logDirectory)) throw new IOException("Diretório de logs não existe: " + logDirectory);

        // Nome do arquivo ZIP
        String zipFileName = "logs-" + pattern + ".zip";
        Path zipFilePath = logDirectory.resolve(zipFileName);

        // Criar arquivo ZIP
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            // Contador de arquivos comprimidos
            final int[] compressedFiles = {0};

            // Percorrer todos os arquivos de log com o padrão especificado
            Files.walkFileTree(logDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();

                    // Verificar se o arquivo corresponde ao padrão e não é um arquivo ZIP
                    if (fileName.contains(pattern) && !fileName.endsWith(".zip")) {
                        // Adicionar ao ZIP
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zipOut.putNextEntry(zipEntry);
                        Files.copy(file, zipOut);
                        zipOut.closeEntry();

                        // Incrementar contador
                        compressedFiles[0]++;

                        // Excluir o arquivo original
                        Files.delete(file);
                        logger.debug("Arquivo comprimido e removido: {}", fileName);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            logger.info("Compressão concluída. {} arquivos foram comprimidos para {}",
                    compressedFiles[0], zipFileName);

            // Se nenhum arquivo foi comprimido, remover o ZIP vazio
            if (compressedFiles[0] == 0) {
                Files.deleteIfExists(zipFilePath);
                logger.info("Nenhum arquivo encontrado para compressão. Arquivo ZIP removido.");
            }
        }
    }

    /**
     * Limpa arquivos de log mais antigos que o número especificado de dias.
     * @param logDirectory Diretório onde estão os logs
     * @param daysToKeep Número de dias para manter os logs
     * @throws IOException Em caso de erro no acesso aos arquivos
     */
    public static void cleanupOldLogs(Path logDirectory, int daysToKeep) throws IOException {
        if (!Files.exists(logDirectory)) return;

        final LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);

        Files.walkFileTree(logDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Verificar se o arquivo é um arquivo ZIP de log
                String fileName = file.getFileName().toString();
                if (fileName.startsWith("logs-") && fileName.endsWith(".zip")) {
                    // Extrair a data do nome do arquivo (formato: logs-YYYY-MM-DD.zip)
                    try {
                        String dateStr = fileName.substring(5, 15); // Extrai YYYY-MM-DD
                        LocalDate fileDate = LocalDate.parse(dateStr);

                        // Remover se for mais antigo que a data de corte
                        if (fileDate.isBefore(cutoffDate)) {
                            Files.delete(file);
                            logger.info("Arquivo de log antigo removido: {}", fileName);
                        }
                    } catch (Exception e) {
                        logger.warn("Não foi possível processar o arquivo de log: {}", fileName, e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}