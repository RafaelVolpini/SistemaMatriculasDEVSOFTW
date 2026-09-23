package br.pucminas.matriculas.model;

import java.util.ArrayList;
import java.util.List;

public class Curso {

    private String nome;
    private int numeroCreditos;
    private List<Disciplina> disciplinas = new ArrayList<>();

    public Curso(String nome, int numeroCreditos) {
        this.nome = nome;
        this.numeroCreditos = numeroCreditos;
    }

    public void adicionarDisciplina(Disciplina disciplina) {
        if (disciplina.getCurso() != null && disciplina.getCurso() != this) {
            throw new RegraNegocioException(disciplina.getCodigo() + " já compõe o curso " + disciplina.getCurso().getNome() + ".");
        }
        if (!disciplinas.contains(disciplina)) {
            disciplinas.add(disciplina);
            disciplina.definirCurso(this);
        }
    }

    public void removerDisciplina(Disciplina disciplina) {
        if (disciplina.totalMatriculadosAtivos() > 0) {
            throw new RegraNegocioException(disciplina.getCodigo() + " possui alunos matriculados e não pode ser removida.");
        }
        if (disciplinas.remove(disciplina)) {
            disciplina.definirCurso(null);
        }
    }

    public String getNome() {
        return nome;
    }

    public int getNumeroCreditos() {
        return numeroCreditos;
    }

    public List<Disciplina> getDisciplinas() {
        return List.copyOf(disciplinas);
    }
}
