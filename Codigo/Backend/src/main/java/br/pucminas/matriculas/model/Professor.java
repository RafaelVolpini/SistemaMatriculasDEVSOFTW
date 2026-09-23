package br.pucminas.matriculas.model;

import java.util.ArrayList;
import java.util.List;

public class Professor extends Usuario {

    private List<Disciplina> disciplinas = new ArrayList<>();

    public Professor(String id, String nome, String email, String senha) {
        super(id, nome, email, senha);
    }

    public List<Aluno> listarAlunosMatriculados(Disciplina disciplina) {
        if (!disciplinas.contains(disciplina)) {
            throw new RegraNegocioException(nome + " não leciona " + disciplina.getCodigo() + ".");
        }
        return disciplina.getMatriculas().stream()
                .filter(Matricula::estaAtiva)
                .map(Matricula::getAluno)
                .toList();
    }

    /** Lado Professor da associação "leciona"; mantido pela Disciplina. */
    void adicionarDisciplina(Disciplina disciplina) {
        if (!disciplinas.contains(disciplina)) {
            disciplinas.add(disciplina);
        }
    }

    void removerDisciplina(Disciplina disciplina) {
        disciplinas.remove(disciplina);
    }

    public List<Disciplina> getDisciplinas() {
        return List.copyOf(disciplinas);
    }
}
