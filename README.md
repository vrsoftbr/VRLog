# Biblioteca de Logging para Java

Uma biblioteca simples para gerenciamento de logs em aplicações Java, baseada no Logback com configuração programática.

## Características

- Configuração via arquivo de propriedades ou programaticamente
- Gerenciamento automático de tamanho de arquivos de log (rolagem em 10MB)
- Compressão automática de logs diários
- Suporte a diferentes níveis de log (TRACE, DEBUG, INFO, WARN, ERROR)
- Configuração de loggers específicos por pacote

## Requisitos

- Java 8 ou superior
- Gradle 6.0 ou superior

## Dependências

- Logback 1.2.3
- SLF4J API 1.7.30
- Apache Commons Configuration 1.10
- Apache Commons Compress 1.21

## Instalação

### Gradle

```groovy
implementation 'com.example:simple-logging-library:1.0.0'
```

## Local
# settings.gradle
```groovy
include ":VRLog"
project(":VRLog").projectDir = file("../VRLog")
```
# build.gradle
```groovy
implementation project(":VRLog")
```

### Maven

```xml

<dependency>
    <groupId>com.example</groupId>
    <artifactId>simple-logging-library</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Uso Básico

### 1. Crie um arquivo de propriedades

Crie um arquivo `logging.properties` com as configurações de log:

```properties
# Configurações básicas de logging
log.level=INFO
log.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
log.file.path=./logs
log.file.name=application.log
log.file.maxSize=10MB
log.file.maxHistory=30
log.archive=true
log.console=true
# Níveis de logger por pacote específico (opcional)
log.level.com.example=DEBUG
log.level.org.springframework=WARN
```

### 2. Inicialize o LogManager no início da aplicação

```java
import com.example.logging.LogManager;

public class Application {
    public static void main(String[] args) {
        // Inicializar com arquivo de propriedades
        LogManager.initialize("logging.properties");

        // Resto da aplicação...
    }
}
```

### 3. Obtenha e use um logger em suas classes

```java
import com.example.logging.LogManager;
import org.slf4j.Logger;

public class MeuServico {
    private static final Logger logger = LogManager.getLogger(MeuServico.class);

    public void executarOperacao() {
        logger.debug("Iniciando operação...");

        try {
            // Lógica do serviço...
            logger.info("Operação concluída com sucesso");
        } catch (Exception e) {
            logger.error("Erro na operação", e);
        }
    }
}
```

## Configuração Programática

Também é possível configurar a biblioteca programaticamente:

```java
import com.example.logging.LogManager;

import java.util.Properties;

public class Application {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("log.level", "DEBUG");
        props.setProperty("log.file.path", "./logs");
        props.setProperty("log.file.name", "minha-aplicacao.log");
        props.setProperty("log.file.maxSize", "10MB");
        props.setProperty("log.console", "true");

        LogManager.initialize(props);
    }
}
```

## Propriedades Disponíveis

| Propriedade         | Descrição                                     | Valor Padrão                                                        |
|---------------------|-----------------------------------------------|---------------------------------------------------------------------|
| log.level           | Nível de log global                           | INFO                                                                |
| log.pattern         | Padrão de formatação dos logs                 | %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n |
| log.file.path       | Caminho para o diretório de logs              | ./logs                                                              |
| log.file.name       | Nome do arquivo de log                        | application.log                                                     |
| log.file.maxSize    | Tamanho máximo de cada arquivo de log         | 10MB                                                                |
| log.file.maxHistory | Número de dias para manter os arquivos de log | 30                                                                  |
| log.archive         | Habilita a compressão automática de logs      | true                                                                |
| log.console         | Habilita a saída de logs no console           | true                                                                |
| log.level.[pacote]  | Nível de log específico para um pacote        | -                                                                   |

## Gerenciamento Avançado de Logs

### Compressão Manual de Logs

É possível comprimir logs manualmente:

```java
import com.example.logging.util.LogCompressor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class LogMaintenance {
    public static void main(String[] args) throws Exception {
        Path logPath = Paths.get("./logs");

        // Comprimir logs de uma data específica
        LogCompressor.compressLogsByDate(logPath, LocalDate.now().minusDays(1));

        // Ou comprimir logs que correspondem a um padrão
        LogCompressor.compressLogsByPattern(logPath, "2023-05");

        // Limpar logs mais antigos que 60 dias
        LogCompressor.cleanupOldLogs(logPath, 60);
    }
}
```

### Encerrando o LogManager

Em alguns casos, como em aplicações web, você pode querer encerrar o LogManager adequadamente:

```java
import com.example.logging.LogManager;

public class Application {
    public void shutdown() {
        // Encerrar o LogManager
        LogManager.getInstance().shutdown();
    }
}
```

## Customização Avançada

### Extensão do LogbackConfigurator

É possível estender a classe `LogbackConfigurator` para customizar ainda mais a configuração:

```java
import com.example.logging.LogbackConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.filter.Filter;

public class CustomLogbackConfigurator extends LogbackConfigurator {
    public CustomLogbackConfigurator(Properties properties) {
        super(properties);
    }

    @Override
    protected void configureAppenders(LoggerContext context) {
        super.configureAppenders(context);

        // Adicionar filtros personalizados ou outras customizações
    }
}
```

## Licença

MIT

## Contribuições

Contribuições são bem-vindas! Por favor, sinta-se à vontade para submeter um Pull Request.