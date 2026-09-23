package br.pucminas.matriculas.app;

import br.pucminas.matriculas.persistencia.BancoDeDados;
import br.pucminas.matriculas.web.Rotas;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Main {

    private static final int PORTA = Integer.getInteger("porta", 8080);
    private static final Path DADOS = Path.of(System.getProperty("dados", "data"));

    private Main() {
    }

    public static void main(String[] args) {
        BancoDeDados banco = new BancoDeDados(DADOS);
        if (banco.existemArquivos()) {
            banco.carregar();
            System.out.println("Dados carregados de " + DADOS.toAbsolutePath());
        } else {
            DataSeeder.popular(banco);
            banco.salvar();
            System.out.println("Dados de exemplo criados em " + DADOS.toAbsolutePath());
        }

        ServicoAcademico servico = new ServicoAcademico(banco);

        // Encerramento automático (US06/US15): verifica a cada 5s se algum período aberto já passou da data de fim.
        ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "encerramento-automatico");
            t.setDaemon(true);
            return t;
        });
        agendador.scheduleWithFixedDelay(() -> {
            try {
                servico.encerrarPeriodosVencidos();
            } catch (RuntimeException e) {
                System.err.println("Falha no encerramento automático: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);

        Path frontend = localizarFrontend();
        Javalin app = Javalin.create(config -> config.staticFiles.add(arquivos -> {
            arquivos.hostedPath = "/";
            arquivos.directory = frontend.toString();
            arquivos.location = Location.EXTERNAL;
        }));
        new Rotas(servico).registrar(app);
        app.start(PORTA);

        System.out.println("Frontend servido de " + frontend);
        System.out.println("Abra http://localhost:" + PORTA);
    }

    /** Aceita rodar de Codigo/Backend (mvn exec:java), de Codigo/ ou da raiz do repositório. */
    private static Path localizarFrontend() {
        for (String candidato : List.of("../Frontend/static", "Frontend/static", "Codigo/Frontend/static")) {
            Path caminho = Path.of(candidato).toAbsolutePath().normalize();
            if (Files.isDirectory(caminho)) {
                return caminho;
            }
        }
        throw new IllegalStateException("Pasta Frontend/static não encontrada. Rode a partir de Codigo/Backend.");
    }
}
