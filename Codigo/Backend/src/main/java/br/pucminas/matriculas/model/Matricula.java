package br.pucminas.matriculas.model;

import br.pucminas.matriculas.model.enums.StatusMatricula;
import br.pucminas.matriculas.model.enums.TipoInscricao;

import java.time.LocalDateTime;

public class Matricula {

    private LocalDateTime dataMatricula;
    private TipoInscricao tipo;
    private StatusMatricula status;

    // navegação exigida pelas associações Aluno 1 — 0..* Matricula e Disciplina 1 — 0..* Matricula ("Recebe")
    private final Aluno aluno;
    private final Disciplina disciplina;

    Matricula(Aluno aluno, Disciplina disciplina, TipoInscricao tipo) {
        this(aluno, disciplina, tipo, StatusMatricula.ATIVA, LocalDateTime.now());
    }

    private Matricula(Aluno aluno, Disciplina disciplina, TipoInscricao tipo,
                      StatusMatricula status, LocalDateTime dataMatricula) {
        this.aluno = aluno;
        this.disciplina = disciplina;
        this.tipo = tipo;
        this.status = status;
        this.dataMatricula = dataMatricula;
    }

    /** Reconstrói uma matrícula já existente (uso da camada de persistência), ligando os dois lados. */
    public static Matricula restaurar(Aluno aluno, Disciplina disciplina, TipoInscricao tipo,
                                      StatusMatricula status, LocalDateTime dataMatricula) {
        Matricula matricula = new Matricula(aluno, disciplina, tipo, status, dataMatricula);
        aluno.adicionarMatricula(matricula);
        disciplina.adicionarMatricula(matricula);
        return matricula;
    }

    public void cancelar() {
        if (status == StatusMatricula.CANCELADA) {
            throw new RegraNegocioException("Matrícula em " + disciplina.getCodigo() + " já está cancelada.");
        }
        status = StatusMatricula.CANCELADA;
    }

    public boolean estaAtiva() {
        return status == StatusMatricula.ATIVA;
    }

    public LocalDateTime getDataMatricula() {
        return dataMatricula;
    }

    public TipoInscricao getTipo() {
        return tipo;
    }

    public StatusMatricula getStatus() {
        return status;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }
}
