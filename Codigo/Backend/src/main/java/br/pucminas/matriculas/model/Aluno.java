package br.pucminas.matriculas.model;

import br.pucminas.matriculas.model.enums.StatusMatricula;
import br.pucminas.matriculas.model.enums.TipoInscricao;

import java.util.ArrayList;
import java.util.List;

public class Aluno extends Usuario {

    public static final int MAX_OBRIGATORIAS = 4;
    public static final int MAX_OPTATIVAS = 2;

    private String registroAcademico;
    private List<Matricula> matriculas = new ArrayList<>();

    public Aluno(String id, String nome, String email, String senha, String registroAcademico) {
        super(id, nome, email, senha);
        this.registroAcademico = registroAcademico;
    }

    public Matricula matricularEmDisciplina(Disciplina disciplina, TipoInscricao tipo) {
        boolean jaMatriculado = matriculas.stream()
                .anyMatch(m -> m.estaAtiva() && m.getDisciplina() == disciplina);
        if (jaMatriculado) {
            throw new RegraNegocioException(nome + " já está matriculado(a) em " + disciplina.getCodigo() + ".");
        }
        int limite = tipo == TipoInscricao.OBRIGATORIA ? MAX_OBRIGATORIAS : MAX_OPTATIVAS;
        if (contarMatriculasPorTipo(tipo) >= limite) {
            throw new RegraNegocioException("Limite de " + limite + " disciplinas "
                    + (tipo == TipoInscricao.OBRIGATORIA ? "obrigatórias" : "optativas") + " atingido.");
        }
        return disciplina.matricular(this, tipo);
    }

    public void cancelarMatricula(Matricula matricula) {
        if (!matriculas.contains(matricula)) {
            throw new RegraNegocioException("Matrícula não pertence ao aluno " + nome + ".");
        }
        matricula.getDisciplina().cancelarMatricula(matricula);
    }

    public List<Matricula> listarMatriculas() {
        return List.copyOf(matriculas);
    }

    public int contarMatriculasPorTipo(TipoInscricao tipo) {
        return (int) matriculas.stream()
                .filter(m -> m.getStatus() == StatusMatricula.ATIVA && m.getTipo() == tipo)
                .count();
    }

    /** Disciplinas com matrícula ativa — base do que é enviado ao Sistema de Cobrança. */
    public List<Disciplina> disciplinasAtivas() {
        return matriculas.stream()
                .filter(Matricula::estaAtiva)
                .map(Matricula::getDisciplina)
                .toList();
    }

    /** Usado pela Disciplina ao criar a matrícula (lado Aluno da associação 1 — 0..*). */
    void adicionarMatricula(Matricula matricula) {
        matriculas.add(matricula);
    }

    public String getRegistroAcademico() {
        return registroAcademico;
    }
}
