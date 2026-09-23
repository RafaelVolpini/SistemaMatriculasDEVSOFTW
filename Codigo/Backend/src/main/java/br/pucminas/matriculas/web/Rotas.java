package br.pucminas.matriculas.web;

import br.pucminas.matriculas.app.ServicoAcademico;
import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.model.RegraNegocioException;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import br.pucminas.matriculas.persistencia.BancoDeDados;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

/**
 * Infraestrutura (não modelada): rotas HTTP chamadas pelo HTMX. Cada ação devolve a aba re-renderizada;
 * erros de regra de negócio voltam como toast de erro, e os avisos gerados chegam pelo poller de notificações.
 */
public final class Rotas {

    private static final String COOKIE_ALUNO = "aluno";
    private static final String COOKIE_PROFESSOR = "professor";

    private final ServicoAcademico servico;

    public Rotas(ServicoAcademico servico) {
        this.servico = servico;
    }

    public void registrar(Javalin app) {
        app.get("/ui/aba/secretaria", ctx -> render(ctx, Paginas::secretaria));
        app.get("/ui/secretaria/status", ctx -> render(ctx, Paginas::secretariaStatus));
        app.get("/ui/secretaria/avisos", ctx -> ctx.html(Paginas.avisos()));
        app.get("/ui/aba/professor", this::abaProfessor);
        app.get("/ui/aba/aluno", this::abaAluno);
        app.get("/ui/notificacoes", this::notificacoes);

        acaoSecretaria(app, "/ui/secretaria/curso", ctx ->
                servico.cadastrarCurso(ctx.formParam("nome"), inteiro(ctx.formParam("creditos"), "Nº de créditos")));
        acaoSecretaria(app, "/ui/secretaria/disciplina", ctx -> servico.cadastrarDisciplina(
                ctx.formParam("codigo"), ctx.formParam("nome"), ctx.formParam("curso"), ctx.formParam("professor")));
        acaoSecretaria(app, "/ui/secretaria/professor", ctx -> servico.cadastrarProfessor(
                ctx.formParam("login"), ctx.formParam("nome"), ctx.formParam("email"), ctx.formParam("senha")));
        acaoSecretaria(app, "/ui/secretaria/aluno", ctx -> servico.cadastrarAluno(
                ctx.formParam("login"), ctx.formParam("nome"), ctx.formParam("email"), ctx.formParam("senha"), ctx.formParam("ra")));
        acaoSecretaria(app, "/ui/secretaria/curriculo", ctx -> servico.gerarCurriculo(
                ctx.formParam("identificador"), ctx.formParams("disciplinas")));
        acaoSecretaria(app, "/ui/secretaria/periodo", ctx -> servico.definirPeriodo(ctx.formParam("semestre"),
                dataHora(ctx.formParam("inicio"), "Início"), dataHora(ctx.formParam("fim"), "Fim")));
        acaoSecretaria(app, "/ui/secretaria/periodo-rapido", ctx -> {
            LocalDateTime agora = LocalDateTime.now().withNano(0);
            servico.definirPeriodo(ctx.formParam("semestre"), agora.minusSeconds(1),
                    agora.plusMinutes(inteiro(ctx.formParam("minutos"), "Minutos")));
        });
        acaoSecretaria(app, "/ui/secretaria/encerrar", ctx -> servico.encerrarPeriodo(ctx.formParam("semestre")));
        acaoSecretaria(app, "/ui/secretaria/lotar", ctx -> servico.lotarDisciplina(ctx.formParam("codigo")));
        acaoSecretaria(app, "/ui/secretaria/resetar", ctx -> {
            servico.resetarDados();
            ctx.removeCookie(COOKIE_ALUNO);
            ctx.removeCookie(COOKIE_PROFESSOR);
        });

        app.post("/ui/professor/entrar", ctx -> entrar(ctx, COOKIE_PROFESSOR, this::paginaProfessor));
        app.post("/ui/aluno/entrar", ctx -> entrar(ctx, COOKIE_ALUNO, this::paginaAluno));
        app.post("/ui/aluno/matricular", ctx -> acaoAluno(ctx, () -> servico.matricular(
                ctx.formParam("id"), ctx.formParam("codigo"), tipo(ctx.formParam("tipo")))));
        app.post("/ui/aluno/cancelar", ctx -> acaoAluno(ctx, () ->
                servico.cancelarMatricula(ctx.formParam("id"), ctx.formParam("codigo"))));
    }

    // ------------------------------------------------------------------ abas

    private void abaProfessor(Context ctx) {
        ctx.html(paginaProfessor(usuarioDaSessao(ctx, COOKIE_PROFESSOR)));
    }

    private void abaAluno(Context ctx) {
        ctx.html(paginaAluno(usuarioDaSessao(ctx, COOKIE_ALUNO)));
    }

    /** "Sessão" simplificada: só um cookie com o login (segurança não é o foco do protótipo). */
    private String usuarioDaSessao(Context ctx, String cookie) {
        if (ctx.queryParam("sair") != null) {
            ctx.removeCookie(cookie);
            return "";
        }
        String id = ctx.cookie(cookie);
        return id == null ? "" : id;
    }

    private String paginaProfessor(String id) {
        return servico.comLock(b -> b.professor(id)
                .map(p -> Paginas.professor(b, p))
                .orElseGet(() -> Paginas.login("professor", b.listarProfessores(), "Entrar como professor")));
    }

    private String paginaAluno(String id) {
        return servico.comLock(b -> b.aluno(id)
                .map(a -> Paginas.aluno(b, a))
                .orElseGet(() -> Paginas.login("aluno", b.listarAlunos(), "Entrar como aluno")));
    }

    private void entrar(Context ctx, String cookie, Function<String, String> pagina) {
        String id = ctx.formParam("id");
        if (servico.autenticar(id, ctx.formParam("senha"))) {
            ctx.cookie(cookie, id);
            ctx.html(pagina.apply(id));
        } else {
            ctx.html(pagina.apply("") + Html.toastErro("Usuário ou senha inválidos (Usuario.autenticar retornou false)."));
        }
    }

    // ------------------------------------------------------------------ ações

    private void acaoSecretaria(Javalin app, String caminho, AcaoHttp acao) {
        app.post(caminho, ctx -> {
            String erro = executar(() -> acao.executar(ctx));
            responder(ctx, servico.comLock(Paginas::secretaria), erro);
        });
    }

    private void acaoAluno(Context ctx, Runnable acao) {
        String erro = executar(acao);
        responder(ctx, paginaAluno(ctx.formParam("id")), erro);
    }

    private String executar(Runnable acao) {
        try {
            acao.run();
            return null;
        } catch (RegraNegocioException e) {
            return e.getMessage();
        }
    }

    private void responder(Context ctx, String pagina, String erro) {
        ctx.header("HX-Trigger", "novas-notificacoes");
        ctx.html(erro == null ? pagina : pagina + Html.toastErro(erro));
    }

    private void notificacoes(Context ctx) {
        long desde = ctx.queryParamAsClass("desde", Long.class).getOrDefault(-1L);
        MuralNotificacoes mural = MuralNotificacoes.getInstance();
        long ultimo = mural.ultimoId();
        // desde < 0: primeira carga da página, só sincroniza o id (não repete o histórico)
        ctx.html(desde < 0 ? Paginas.poller(ultimo) : Paginas.notificacoes(mural.desde(desde), Math.max(ultimo, desde)));
    }

    private void render(Context ctx, Function<BancoDeDados, String> pagina) {
        ctx.html(servico.comLock(pagina));
    }

    // ------------------------------------------------------------------ conversões

    private static int inteiro(String valor, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (RuntimeException e) {
            throw new RegraNegocioException(campo + " deve ser um número inteiro.");
        }
    }

    private static LocalDateTime dataHora(String valor, String campo) {
        try {
            return LocalDateTime.parse(valor);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RegraNegocioException(campo + " inválido.");
        }
    }

    private static TipoInscricao tipo(String valor) {
        try {
            return TipoInscricao.valueOf(valor);
        } catch (RuntimeException e) {
            throw new RegraNegocioException("Tipo de inscrição inválido.");
        }
    }

    @FunctionalInterface
    private interface AcaoHttp {
        void executar(Context ctx);
    }
}
