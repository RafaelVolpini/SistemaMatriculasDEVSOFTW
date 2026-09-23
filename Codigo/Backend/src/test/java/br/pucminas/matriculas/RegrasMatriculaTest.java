package br.pucminas.matriculas;

import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.externo.Notificacao;
import br.pucminas.matriculas.externo.SistemaCobranca;
import br.pucminas.matriculas.model.Aluno;
import br.pucminas.matriculas.model.Curso;
import br.pucminas.matriculas.model.Disciplina;
import br.pucminas.matriculas.model.Matricula;
import br.pucminas.matriculas.model.Professor;
import br.pucminas.matriculas.model.RegraNegocioException;
import br.pucminas.matriculas.model.Secretaria;
import br.pucminas.matriculas.model.Semestre;
import br.pucminas.matriculas.model.enums.StatusDisciplina;
import br.pucminas.matriculas.model.enums.StatusMatricula;
import br.pucminas.matriculas.model.enums.StatusPeriodo;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import br.pucminas.matriculas.persistencia.BancoDeDados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegrasMatriculaTest {

    @TempDir
    Path dados;

    private BancoDeDados banco;
    private Secretaria secretaria;
    private Professor professor;
    private Curso curso;

    @BeforeEach
    void preparar() {
        MuralNotificacoes.getInstance().limpar();
        SistemaCobranca.getInstance().limpar();
        banco = new BancoDeDados(dados);
        secretaria = new Secretaria("sec", "Secretaria", "sec@puc", "123");
        banco.adicionarSecretaria(secretaria);
        professor = new Professor("prof", "Professor", "prof@puc", "123");
        secretaria.cadastrarProfessor(professor);
        curso = new Curso("ES", 200);
        secretaria.cadastrarCurso(curso);
    }

    private Disciplina disciplina(String codigo) {
        Disciplina d = new Disciplina(codigo, "Disciplina " + codigo);
        curso.adicionarDisciplina(d);
        d.definirProfessor(professor);
        secretaria.cadastrarDisciplina(d);
        return d;
    }

    private Aluno aluno(String id) {
        Aluno a = new Aluno(id, "Aluno " + id, id + "@puc", "123", "RA-" + id);
        secretaria.cadastrarAluno(a);
        return a;
    }

    private List<Notificacao> avisosCobranca() {
        return MuralNotificacoes.getInstance().todas().stream()
                .filter(n -> Notificacao.ORIGEM_COBRANCA.equals(n.origem())).toList();
    }

    @Test
    void autenticarConfereASenha() {
        Aluno a = aluno("ana");
        assertTrue(a.autenticar("123"));
        assertFalse(a.autenticar("errada"));
    }

    @Test
    void matriculaCriaMatriculaAtivaENotificaCobranca() {
        Aluno a = aluno("ana");
        Disciplina d = disciplina("ES101");

        Matricula m = a.matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);

        assertEquals(StatusMatricula.ATIVA, m.getStatus());
        assertEquals(1, d.totalMatriculadosAtivos());
        assertEquals(List.of(m), a.listarMatriculas());
        assertEquals(1, avisosCobranca().size());
        assertTrue(avisosCobranca().get(0).mensagem().contains("ES101"));
    }

    @Test
    void limiteDeQuatroObrigatoriasEDuasOptativas() {
        Aluno a = aluno("ana");
        for (int i = 1; i <= 4; i++) {
            a.matricularEmDisciplina(disciplina("OB" + i), TipoInscricao.OBRIGATORIA);
        }
        assertThrows(RegraNegocioException.class,
                () -> a.matricularEmDisciplina(disciplina("OB5"), TipoInscricao.OBRIGATORIA));

        a.matricularEmDisciplina(disciplina("OP1"), TipoInscricao.OPTATIVA);
        a.matricularEmDisciplina(disciplina("OP2"), TipoInscricao.OPTATIVA);
        assertThrows(RegraNegocioException.class,
                () -> a.matricularEmDisciplina(disciplina("OP3"), TipoInscricao.OPTATIVA));

        assertEquals(4, a.contarMatriculasPorTipo(TipoInscricao.OBRIGATORIA));
        assertEquals(2, a.contarMatriculasPorTipo(TipoInscricao.OPTATIVA));
    }

    @Test
    void naoPermiteMatricularDuasVezesNaMesmaDisciplina() {
        Aluno a = aluno("ana");
        Disciplina d = disciplina("ES101");
        a.matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        assertThrows(RegraNegocioException.class, () -> a.matricularEmDisciplina(d, TipoInscricao.OPTATIVA));
    }

    @Test
    void aoAtingir60AlunosAsInscricoesSaoEncerradas() {
        Disciplina d = disciplina("ES101");
        for (int i = 0; i < 60; i++) {
            aluno("a" + i).matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        }
        assertTrue(d.atingiuLimiteVagas());
        assertEquals(StatusDisciplina.ATIVA, d.getStatus());
        Aluno atrasado = aluno("atrasado");
        assertThrows(RegraNegocioException.class, () -> atrasado.matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA));
        assertEquals(60, d.totalMatriculadosAtivos());
    }

    @Test
    void cancelarLiberaVagaEReabreDisciplinaLotada() {
        Disciplina d = disciplina("ES101");
        Aluno primeiro = aluno("a0");
        Matricula m = primeiro.matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        for (int i = 1; i < 60; i++) {
            aluno("a" + i).matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        }
        assertEquals(StatusDisciplina.ATIVA, d.getStatus());

        primeiro.cancelarMatricula(m);

        assertEquals(StatusMatricula.CANCELADA, m.getStatus());
        assertEquals(59, d.totalMatriculadosAtivos());
        assertEquals(StatusDisciplina.EM_ABERTO, d.getStatus());
        assertEquals(0, primeiro.contarMatriculasPorTipo(TipoInscricao.OBRIGATORIA));
        assertThrows(RegraNegocioException.class, m::cancelar);
    }

    @Test
    void encerramentoCancelaDisciplinasSemQuorumENotificaCobrancaDosAfetados() {
        Disciplina cheia = disciplina("ES101");
        Disciplina vazia = disciplina("ES105");
        Semestre semestre = new Semestre("2026/2");
        semestre.gerarCurriculo(secretaria.disciplinasDisponiveis());
        secretaria.gerarCurriculoSemestre(semestre);
        secretaria.definirPeriodoMatriculas(semestre, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(5));

        Aluno ana = aluno("ana");
        ana.matricularEmDisciplina(cheia, TipoInscricao.OBRIGATORIA);
        ana.matricularEmDisciplina(vazia, TipoInscricao.OPTATIVA);
        aluno("bruno").matricularEmDisciplina(cheia, TipoInscricao.OBRIGATORIA);
        aluno("carla").matricularEmDisciplina(cheia, TipoInscricao.OBRIGATORIA);
        int avisosAntes = avisosCobranca().size();

        secretaria.encerrarPeriodoMatriculas(semestre);

        assertEquals(StatusPeriodo.ENCERRADO, semestre.getPeriodo().getStatus());
        assertEquals(StatusDisciplina.ATIVA, cheia.getStatus());
        assertEquals(StatusDisciplina.CANCELADA, vazia.getStatus());
        assertEquals(0, vazia.totalMatriculadosAtivos());
        assertEquals(List.of(cheia), ana.disciplinasAtivas());

        List<Notificacao> novos = avisosCobranca().subList(avisosAntes, avisosCobranca().size());
        assertEquals(1, novos.size());
        assertTrue(novos.get(0).mensagem().contains("Removida da cobrança: ES105"));
    }

    @Test
    void periodoSoEstaAbertoDentroDaJanelaEAntesDeEncerrar() {
        disciplina("ES101");
        Semestre semestre = new Semestre("2026/2");
        semestre.gerarCurriculo(secretaria.disciplinasDisponiveis());
        secretaria.gerarCurriculoSemestre(semestre);

        secretaria.definirPeriodoMatriculas(semestre, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(10));
        assertFalse(semestre.getPeriodo().estaAberto(), "ainda não começou");

        secretaria.definirPeriodoMatriculas(semestre, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().minusMinutes(1));
        assertFalse(semestre.getPeriodo().estaAberto(), "já terminou");
        assertTrue(semestre.getPeriodo().venceu());

        secretaria.definirPeriodoMatriculas(semestre, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        assertTrue(semestre.getPeriodo().estaAberto());

        secretaria.encerrarPeriodoMatriculas(semestre);
        assertFalse(semestre.getPeriodo().estaAberto());
        assertThrows(RegraNegocioException.class, () -> secretaria.encerrarPeriodoMatriculas(semestre));
        assertThrows(RegraNegocioException.class, () -> secretaria.definirPeriodoMatriculas(semestre,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(1)));
    }

    @Test
    void professorListaSomenteAlunosAtivosDasSuasDisciplinas() {
        Disciplina d = disciplina("ES101");
        Aluno ana = aluno("ana");
        Aluno bruno = aluno("bruno");
        ana.matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        Matricula mb = bruno.matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        bruno.cancelarMatricula(mb);

        assertEquals(List.of(ana), professor.listarAlunosMatriculados(d));

        Professor outro = new Professor("outro", "Outro", "o@puc", "123");
        assertThrows(RegraNegocioException.class, () -> outro.listarAlunosMatriculados(d));
    }

    @Test
    void secretariaRejeitaCadastrosDuplicados() {
        aluno("ana");
        assertThrows(RegraNegocioException.class, () -> aluno("ana"));
        assertThrows(RegraNegocioException.class, () -> secretaria.cadastrarCurso(new Curso("ES", 10)));
        disciplina("ES101");
        Disciplina repetida = new Disciplina("ES101", "Outra");
        repetida.definirProfessor(professor);
        assertThrows(RegraNegocioException.class, () -> secretaria.cadastrarDisciplina(repetida));
    }

    @Test
    void persistenciaJsonPreservaOEstado() {
        Disciplina d = disciplina("ES101");
        Semestre semestre = new Semestre("2026/2");
        semestre.gerarCurriculo(secretaria.disciplinasDisponiveis());
        secretaria.gerarCurriculoSemestre(semestre);
        secretaria.definirPeriodoMatriculas(semestre, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(5));
        Aluno ana = aluno("ana");
        ana.matricularEmDisciplina(d, TipoInscricao.OPTATIVA);
        Matricula cancelada = aluno("bruno").matricularEmDisciplina(d, TipoInscricao.OBRIGATORIA);
        cancelada.getAluno().cancelarMatricula(cancelada);
        banco.salvar();

        BancoDeDados recarregado = new BancoDeDados(dados);
        recarregado.carregar();

        Disciplina d2 = recarregado.disciplina("ES101").orElseThrow();
        Aluno ana2 = recarregado.aluno("ana").orElseThrow();
        assertEquals(1, d2.totalMatriculadosAtivos());
        assertEquals(2, d2.getMatriculas().size());
        assertEquals("prof", d2.getProfessor().getId());
        assertEquals("ES", d2.getCurso().getNome());
        assertEquals(List.of(d2), recarregado.professor("prof").orElseThrow().getDisciplinas());
        assertEquals(1, ana2.contarMatriculasPorTipo(TipoInscricao.OPTATIVA));
        assertTrue(recarregado.semestre("2026/2").orElseThrow().getPeriodo().estaAberto());
        assertTrue(recarregado.semestre("2026/2").orElseThrow().oferece(d2));
        assertTrue(recarregado.listarSecretarias().get(0).autenticar("123"));
    }
}
