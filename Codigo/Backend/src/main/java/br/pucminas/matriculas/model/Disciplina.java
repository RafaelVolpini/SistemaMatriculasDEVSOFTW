package br.pucminas.matriculas.model;

import br.pucminas.matriculas.externo.SistemaCobranca;
import br.pucminas.matriculas.model.enums.StatusDisciplina;
import br.pucminas.matriculas.model.enums.TipoInscricao;

import java.util.ArrayList;
import java.util.List;

public class Disciplina {

    private String codigo;
    private String nome;
    private int vagasMaximas = 60;
    private int minimosAlunos = 3;
    private StatusDisciplina status = StatusDisciplina.EM_ABERTO;

    // navegação exigida pelas associações "Recebe", "leciona" e "compõe"
    private final List<Matricula> matriculas = new ArrayList<>();
    private Professor professor;
    private Curso curso;

    public Disciplina(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public Matricula matricular(Aluno aluno, TipoInscricao tipo) {
        if (status != StatusDisciplina.EM_ABERTO) {
            throw new RegraNegocioException("Inscrições em " + codigo + " não estão abertas (status " + status + ").");
        }
        if (atingiuLimiteVagas()) {
            throw new RegraNegocioException(codigo + " já atingiu o limite de " + vagasMaximas + " vagas.");
        }
        Matricula matricula = new Matricula(aluno, this, tipo);
        adicionarMatricula(matricula);
        aluno.adicionarMatricula(matricula);

        SistemaCobranca.getInstance().notificarCobranca(aluno, aluno.disciplinasAtivas());

        if (atingiuLimiteVagas()) {
            encerrarInscricoesPorLotacao();
        }
        return matricula;
    }

    public void cancelarMatricula(Matricula matricula) {
        if (matricula.getDisciplina() != this) {
            throw new RegraNegocioException("Matrícula não pertence a " + codigo + ".");
        }
        matricula.cancelar();
        // Decisão do grupo: se a disciplina estava fechada por lotação, a vaga liberada reabre as inscrições.
        if (status == StatusDisciplina.ATIVA && !atingiuLimiteVagas()) {
            status = StatusDisciplina.EM_ABERTO;
        }
        SistemaCobranca.getInstance().notificarCobranca(matricula.getAluno(), matricula.getAluno().disciplinasAtivas());
    }

    public int totalMatriculadosAtivos() {
        return (int) matriculas.stream().filter(Matricula::estaAtiva).count();
    }

    public boolean atingiuLimiteVagas() {
        return totalMatriculadosAtivos() >= vagasMaximas;
    }

    public boolean atingiuMinimoAlunos() {
        return totalMatriculadosAtivos() >= minimosAlunos;
    }

    public void encerrarInscricoesPorLotacao() {
        status = StatusDisciplina.ATIVA;
    }

    public void avaliarPermanenciaAoFimDoPeriodo() {
        if (status == StatusDisciplina.CANCELADA) {
            return;
        }
        if (atingiuMinimoAlunos()) {
            status = StatusDisciplina.ATIVA;
            return;
        }
        status = StatusDisciplina.CANCELADA;
        for (Matricula matricula : matriculas) {
            if (matricula.estaAtiva()) {
                matricula.cancelar();
                Aluno aluno = matricula.getAluno();
                SistemaCobranca.getInstance().notificarCobranca(aluno, aluno.disciplinasAtivas());
            }
        }
    }

    /** Liga os dois lados da associação "leciona". */
    public void definirProfessor(Professor novoProfessor) {
        if (professor != null) {
            professor.removerDisciplina(this);
        }
        professor = novoProfessor;
        if (novoProfessor != null) {
            novoProfessor.adicionarDisciplina(this);
        }
    }

    /** Restaura o status salvo (uso exclusivo da camada de persistência). */
    public void restaurarStatus(StatusDisciplina statusSalvo) {
        this.status = statusSalvo;
    }

    void adicionarMatricula(Matricula matricula) {
        matriculas.add(matricula);
    }

    void definirCurso(Curso curso) {
        this.curso = curso;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public int getVagasMaximas() {
        return vagasMaximas;
    }

    public int getMinimosAlunos() {
        return minimosAlunos;
    }

    public StatusDisciplina getStatus() {
        return status;
    }

    public List<Matricula> getMatriculas() {
        return List.copyOf(matriculas);
    }

    public Professor getProfessor() {
        return professor;
    }

    public Curso getCurso() {
        return curso;
    }
}
