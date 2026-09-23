package br.pucminas.matriculas.app;

import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.externo.Notificacao;
import br.pucminas.matriculas.model.Aluno;
import br.pucminas.matriculas.model.Curso;
import br.pucminas.matriculas.model.Disciplina;
import br.pucminas.matriculas.model.Matricula;
import br.pucminas.matriculas.model.Professor;
import br.pucminas.matriculas.model.RegraNegocioException;
import br.pucminas.matriculas.model.Secretaria;
import br.pucminas.matriculas.model.Semestre;
import br.pucminas.matriculas.model.enums.StatusDisciplina;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import br.pucminas.matriculas.persistencia.BancoDeDados;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Camada de aplicação: localiza os objetos, chama os métodos do domínio, publica os avisos "[Sistema]" e persiste.
 * Um único lock serializa as requisições HTTP e o agendador de encerramento automático.
 */
public class ServicoAcademico {

    private final BancoDeDados banco;
    private final ReentrantLock lock = new ReentrantLock();

    public ServicoAcademico(BancoDeDados banco) {
        this.banco = banco;
    }

    /** Executa qualquer leitura/escrita com o lock (a interface renderiza as telas por aqui). */
    public <T> T comLock(Function<BancoDeDados, T> acao) {
        lock.lock();
        try {
            return acao.apply(banco);
        } finally {
            lock.unlock();
        }
    }

    private void escrever(Runnable acao) {
        comLock(b -> {
            acao.run();
            b.salvar();
            return null;
        });
    }

    public Secretaria secretaria() {
        return banco.listarSecretarias().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma secretaria cadastrada."));
    }

    // ------------------------------------------------------------------ Secretaria

    public void cadastrarCurso(String nome, int creditos) {
        escrever(() -> secretaria().cadastrarCurso(new Curso(limpo(nome), creditos)));
    }

    public void cadastrarDisciplina(String codigo, String nome, String nomeCurso, String professorId) {
        escrever(() -> {
            Curso curso = banco.curso(nomeCurso).orElseThrow(() -> new RegraNegocioException("Selecione um curso."));
            Professor professor = banco.professor(professorId)
                    .orElseThrow(() -> new RegraNegocioException("Selecione um professor."));
            String codigoLimpo = limpo(codigo).toUpperCase();
            if (banco.existeDisciplina(codigoLimpo)) {
                throw new RegraNegocioException("Já existe uma disciplina com código " + codigoLimpo + ".");
            }
            Disciplina disciplina = new Disciplina(codigoLimpo, limpo(nome));
            curso.adicionarDisciplina(disciplina);
            disciplina.definirProfessor(professor);
            try {
                secretaria().cadastrarDisciplina(disciplina);
            } catch (RuntimeException e) {
                curso.removerDisciplina(disciplina);
                disciplina.definirProfessor(null);
                throw e;
            }
        });
    }

    public void cadastrarProfessor(String login, String nome, String email, String senha) {
        escrever(() -> secretaria().cadastrarProfessor(new Professor(limpo(login), limpo(nome), limpo(email), senha)));
    }

    public void cadastrarAluno(String login, String nome, String email, String senha, String ra) {
        escrever(() -> secretaria().cadastrarAluno(new Aluno(limpo(login), limpo(nome), limpo(email), senha, limpo(ra))));
    }

    /** Cria o semestre (se ainda não existir) e inclui no currículo as disciplinas marcadas na tela. */
    public void gerarCurriculo(String identificador, List<String> codigos) {
        escrever(() -> {
            String id = limpo(identificador);
            if (id == null || id.isBlank()) {
                throw new RegraNegocioException("Identificador do semestre é obrigatório.");
            }
            Semestre semestre = banco.semestre(id).orElseGet(() -> new Semestre(id));
            List<Disciplina> selecionadas = codigos.stream().map(this::disciplina).toList();
            // valida antes de alterar o semestre, para não deixar o currículo pela metade
            List<Disciplina> disponiveis = secretaria().disciplinasDisponiveis();
            for (Disciplina d : selecionadas) {
                if (!disponiveis.contains(d) && !semestre.oferece(d)) {
                    throw new RegraNegocioException(d.getCodigo() + " já é ofertada no semestre "
                            + banco.semestreQueOferece(d).map(Semestre::getIdentificador).orElse("?") + ".");
                }
            }
            int antes = semestre.getDisciplinasOfertadas().size();
            semestre.gerarCurriculo(selecionadas);
            secretaria().gerarCurriculoSemestre(semestre);
            int novas = semestre.getDisciplinasOfertadas().size() - antes;
            sistema(Notificacao.Nivel.SUCESSO, "Currículo de " + id + " gerado: " + novas + " disciplina(s) adicionada(s), "
                    + semestre.getDisciplinasOfertadas().size() + " no total.");
        });
    }

    public void definirPeriodo(String identificador, LocalDateTime inicio, LocalDateTime fim) {
        escrever(() -> {
            String id = limpo(identificador);
            boolean novo = id != null && banco.semestre(id).isEmpty();
            Semestre semestre = banco.semestre(id == null ? "" : id).orElseGet(() -> new Semestre(id));
            secretaria().definirPeriodoMatriculas(semestre, inicio, fim);
            sistema(Notificacao.Nivel.SUCESSO, (novo ? "Semestre " + id + " criado. " : "")
                    + "Período de matrículas de " + id + ": " + Formatos.dataHora(inicio) + " até " + Formatos.dataHora(fim) + "."
                    + (semestre.getDisciplinasOfertadas().isEmpty()
                    ? " Ainda não há disciplinas ofertadas — gere o currículo para os alunos poderem se matricular." : ""));
        });
    }

    public void encerrarPeriodo(String identificador) {
        escrever(() -> encerrar(semestre(identificador), false));
    }

    /** Chamado pelo agendador: encerra sozinho os períodos cuja data de fim já passou. */
    public void encerrarPeriodosVencidos() {
        comLock(b -> {
            List<Semestre> vencidos = b.listarSemestres().stream()
                    .filter(s -> s.getPeriodo() != null && s.getPeriodo().venceu())
                    .toList();
            if (!vencidos.isEmpty()) {
                vencidos.forEach(s -> encerrar(s, true));
                b.salvar();
            }
            return null;
        });
    }

    private void encerrar(Semestre semestre, boolean automatico) {
        sistema(Notificacao.Nivel.INFO, (automatico ? "Prazo esgotado: encerrando automaticamente" : "Encerrando")
                + " o período de matrículas de " + semestre.getIdentificador() + "...");
        // Anuncia antes as disciplinas sem quórum, para que os avisos da cobrança (disparados pelo domínio) venham depois.
        Map<Disciplina, Integer> ativosAntes = semestre.getDisciplinasOfertadas().stream()
                .collect(Collectors.toMap(d -> d, Disciplina::totalMatriculadosAtivos));
        semestre.getDisciplinasOfertadas().stream()
                .filter(d -> d.getStatus() != StatusDisciplina.CANCELADA && !d.atingiuMinimoAlunos())
                .forEach(d -> sistema(Notificacao.Nivel.ERRO, d.getCodigo() + " não atingiu o mínimo de "
                        + d.getMinimosAlunos() + " alunos (" + ativosAntes.get(d) + "/" + d.getMinimosAlunos()
                        + ") e será CANCELADA."));

        secretaria().encerrarPeriodoMatriculas(semestre);

        List<String> confirmadas = semestre.getDisciplinasOfertadas().stream()
                .filter(d -> d.getStatus() == StatusDisciplina.ATIVA)
                .map(d -> d.getCodigo() + " (" + d.totalMatriculadosAtivos() + ")")
                .toList();
        long canceladas = semestre.getDisciplinasOfertadas().stream()
                .filter(d -> d.getStatus() == StatusDisciplina.CANCELADA).count();
        sistema(Notificacao.Nivel.INFO, "Período de " + semestre.getIdentificador() + " encerrado. Confirmadas: "
                + (confirmadas.isEmpty() ? "nenhuma" : String.join(", ", confirmadas))
                + ". Canceladas: " + canceladas + ".");
    }

    /** Ferramenta de teste (US16): cria alunos fictícios e os matricula até a disciplina lotar. */
    public void lotarDisciplina(String codigo) {
        escrever(() -> {
            Disciplina disciplina = disciplina(codigo);
            exigirPeriodoAberto(disciplina);
            MuralNotificacoes.Silenciado<Integer> r = MuralNotificacoes.getInstance().silenciar(() -> {
                int criados = 0;
                while (disciplina.getStatus() == StatusDisciplina.EM_ABERTO && !disciplina.atingiuLimiteVagas()) {
                    int n = banco.listarAlunos().size() + 1;
                    String login = String.format("teste%03d", n);
                    Aluno aluno = new Aluno(login, String.format("Aluno Teste %03d", n),
                            login + "@teste.edu", "123", String.format("T%05d", n));
                    secretaria().cadastrarAluno(aluno);
                    aluno.matricularEmDisciplina(disciplina, TipoInscricao.OPTATIVA);
                    criados++;
                }
                return criados;
            });
            MuralNotificacoes.getInstance().publicar(Notificacao.ORIGEM_COBRANCA, Notificacao.Nivel.INFO,
                    r.avisosOmitidos() + " cobranças enviadas para os " + r.resultado() + " alunos de teste matriculados em "
                            + disciplina.getCodigo() + " (avisos individuais omitidos).");
            avisarSeLotou(disciplina, StatusDisciplina.EM_ABERTO);
        });
    }

    public void resetarDados() {
        comLock(b -> {
            b.apagarTudo();
            DataSeeder.popular(b);
            sistema(Notificacao.Nivel.INFO, "Dados reiniciados com o conjunto de exemplo.");
            b.salvar();
            return null;
        });
    }

    // ------------------------------------------------------------------ Aluno

    public void matricular(String alunoId, String codigo, TipoInscricao tipo) {
        escrever(() -> {
            Aluno aluno = aluno(alunoId);
            Disciplina disciplina = disciplina(codigo);
            exigirPeriodoAberto(disciplina);
            StatusDisciplina antes = disciplina.getStatus();
            aluno.matricularEmDisciplina(disciplina, tipo);
            sistema(Notificacao.Nivel.SUCESSO, aluno.getNome() + " matriculado(a) em " + disciplina.getCodigo()
                    + " (" + Formatos.tipo(tipo) + ").");
            avisarSeLotou(disciplina, antes);
        });
    }

    public void cancelarMatricula(String alunoId, String codigo) {
        escrever(() -> {
            Aluno aluno = aluno(alunoId);
            Disciplina disciplina = disciplina(codigo);
            exigirPeriodoAberto(disciplina);
            Matricula matricula = aluno.listarMatriculas().stream()
                    .filter(m -> m.estaAtiva() && m.getDisciplina() == disciplina)
                    .findFirst()
                    .orElseThrow(() -> new RegraNegocioException(aluno.getNome() + " não está matriculado(a) em " + codigo + "."));
            StatusDisciplina antes = disciplina.getStatus();
            aluno.cancelarMatricula(matricula);
            sistema(Notificacao.Nivel.INFO, aluno.getNome() + " cancelou a matrícula em " + disciplina.getCodigo() + ".");
            if (antes == StatusDisciplina.ATIVA && disciplina.getStatus() == StatusDisciplina.EM_ABERTO) {
                sistema(Notificacao.Nivel.INFO, "Vaga liberada: inscrições em " + disciplina.getCodigo() + " reabertas.");
            }
        });
    }

    // ------------------------------------------------------------------ login

    public boolean autenticar(String usuarioId, String senha) {
        return comLock(b -> b.aluno(usuarioId).map(u -> u.autenticar(senha))
                .or(() -> b.professor(usuarioId).map(u -> u.autenticar(senha)))
                .orElse(false));
    }

    // ------------------------------------------------------------------ auxiliares

    private void exigirPeriodoAberto(Disciplina disciplina) {
        Semestre semestre = banco.semestreQueOferece(disciplina)
                .orElseThrow(() -> new RegraNegocioException(disciplina.getCodigo() + " não está no currículo de nenhum semestre."));
        if (semestre.getPeriodo() == null || !semestre.getPeriodo().estaAberto()) {
            throw new RegraNegocioException("O período de matrículas de " + semestre.getIdentificador() + " não está aberto.");
        }
    }

    private void avisarSeLotou(Disciplina disciplina, StatusDisciplina antes) {
        if (antes == StatusDisciplina.EM_ABERTO && disciplina.getStatus() == StatusDisciplina.ATIVA) {
            sistema(Notificacao.Nivel.ALERTA, disciplina.getCodigo() + " atingiu " + disciplina.getVagasMaximas()
                    + " alunos — inscrições encerradas por lotação.");
        }
    }

    private Aluno aluno(String id) {
        return banco.aluno(id).orElseThrow(() -> new RegraNegocioException("Aluno " + id + " não encontrado."));
    }

    private Disciplina disciplina(String codigo) {
        return banco.disciplina(codigo).orElseThrow(() -> new RegraNegocioException("Disciplina " + codigo + " não encontrada."));
    }

    private Semestre semestre(String identificador) {
        return banco.semestre(identificador)
                .orElseThrow(() -> new RegraNegocioException("Semestre " + identificador + " não encontrado."));
    }

    private static void sistema(Notificacao.Nivel nivel, String mensagem) {
        MuralNotificacoes.getInstance().publicar(Notificacao.ORIGEM_SISTEMA, nivel, mensagem);
    }

    private static String limpo(String valor) {
        return valor == null ? null : valor.trim();
    }
}
