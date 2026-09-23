package br.pucminas.matriculas;

import br.pucminas.matriculas.app.DataSeeder;
import br.pucminas.matriculas.app.ServicoAcademico;
import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.externo.Notificacao;
import br.pucminas.matriculas.externo.SistemaCobranca;
import br.pucminas.matriculas.model.RegraNegocioException;
import br.pucminas.matriculas.model.enums.StatusDisciplina;
import br.pucminas.matriculas.model.enums.StatusPeriodo;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import br.pucminas.matriculas.persistencia.BancoDeDados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicoAcademicoTest {

    @TempDir
    Path dados;

    private BancoDeDados banco;
    private ServicoAcademico servico;

    @BeforeEach
    void preparar() {
        MuralNotificacoes.getInstance().limpar();
        SistemaCobranca.getInstance().limpar();
        banco = new BancoDeDados(dados);
        DataSeeder.popular(banco);
        servico = new ServicoAcademico(banco);
    }

    @Test
    void matriculaPelaAplicacaoPersisteEmArquivo() {
        servico.matricular("elisa", "ES104", TipoInscricao.OBRIGATORIA);
        assertTrue(Files.exists(dados.resolve("matriculas.json")));

        BancoDeDados recarregado = new BancoDeDados(dados);
        recarregado.carregar();
        assertEquals(1, recarregado.aluno("elisa").orElseThrow().contarMatriculasPorTipo(TipoInscricao.OBRIGATORIA));
    }

    @Test
    void periodoFechadoBloqueiaMatriculaECancelamento() {
        servico.encerrarPeriodo("2026/2");
        assertThrows(RegraNegocioException.class, () -> servico.matricular("elisa", "ES101", TipoInscricao.OBRIGATORIA));
        assertThrows(RegraNegocioException.class, () -> servico.cancelarMatricula("ana", "ES101"));
    }

    @Test
    void encerramentoAutomaticoQuandoOPrazoVence() {
        LocalDateTime agora = LocalDateTime.now();
        servico.definirPeriodo("2026/2", agora.minusMinutes(10), agora.minusSeconds(1));

        servico.encerrarPeriodosVencidos();

        assertEquals(StatusPeriodo.ENCERRADO, banco.semestre("2026/2").orElseThrow().getPeriodo().getStatus());
        // seed: ES101 e ES102 com 3 alunos; ES103 (2), ES104 (0), ES105 (1), ES106 (0) sem quórum
        assertEquals(StatusDisciplina.ATIVA, banco.disciplina("ES101").orElseThrow().getStatus());
        assertEquals(StatusDisciplina.CANCELADA, banco.disciplina("ES105").orElseThrow().getStatus());
        assertTrue(MuralNotificacoes.getInstance().todas().stream().anyMatch(n ->
                Notificacao.ORIGEM_COBRANCA.equals(n.origem()) && n.mensagem().contains("Carla")
                        && n.mensagem().contains("Removida da cobrança: ES103")));
    }

    @Test
    void novoSemestreRecebeAsDisciplinasSelecionadas() {
        servico.cadastrarDisciplina("ES201", "Arquitetura de Software", "Engenharia de Software", "prof.hugo");
        servico.cadastrarDisciplina("ES202", "Testes de Software", "Engenharia de Software", "prof.lucia");
        servico.cadastrarDisciplina("ES203", "DevOps", "Engenharia de Software", "prof.lucia");

        servico.gerarCurriculo("2027/1", java.util.List.of("ES201", "ES202"));

        var semestre = banco.semestre("2027/1").orElseThrow();
        assertEquals(2, semestre.getDisciplinasOfertadas().size());
        assertTrue(semestre.oferece(banco.disciplina("ES201").orElseThrow()));

        // disciplina já ofertada em outro semestre é recusada, e o currículo não muda
        assertThrows(RegraNegocioException.class, () -> servico.gerarCurriculo("2027/1", java.util.List.of("ES203", "ES101")));
        assertEquals(2, semestre.getDisciplinasOfertadas().size());
        // sem nenhuma disciplina marcada, o semestre não é criado
        assertThrows(RegraNegocioException.class, () -> servico.gerarCurriculo("2027/2", java.util.List.of()));
        assertTrue(banco.semestre("2027/2").isEmpty());
    }

    @Test
    void definirPeriodoCriaSemestreNovoEOCurriculoPodeVirDepois() {
        LocalDateTime agora = LocalDateTime.now();
        servico.definirPeriodo("2027/1", agora.minusMinutes(1), agora.plusMinutes(30));

        var semestre = banco.semestre("2027/1").orElseThrow();
        assertTrue(semestre.getPeriodo().estaAberto());
        assertTrue(semestre.getDisciplinasOfertadas().isEmpty());

        servico.cadastrarDisciplina("ES201", "Arquitetura de Software", "Engenharia de Software", "prof.hugo");
        servico.gerarCurriculo("2027/1", java.util.List.of("ES201"));
        servico.matricular("elisa", "ES201", TipoInscricao.OBRIGATORIA);
        assertEquals(1, banco.disciplina("ES201").orElseThrow().totalMatriculadosAtivos());

        // fim antes do início é recusado e não cria semestre
        assertThrows(RegraNegocioException.class, () -> servico.definirPeriodo("2027/2", agora, agora.minusMinutes(5)));
        assertTrue(banco.semestre("2027/2").isEmpty());
    }

    @Test
    void lotarDisciplinaCriaAlunosAteSessenta() {
        servico.lotarDisciplina("ES106");
        assertEquals(60, banco.disciplina("ES106").orElseThrow().totalMatriculadosAtivos());
        assertEquals(StatusDisciplina.ATIVA, banco.disciplina("ES106").orElseThrow().getStatus());
        assertThrows(RegraNegocioException.class, () -> servico.matricular("elisa", "ES106", TipoInscricao.OPTATIVA));
    }
}
