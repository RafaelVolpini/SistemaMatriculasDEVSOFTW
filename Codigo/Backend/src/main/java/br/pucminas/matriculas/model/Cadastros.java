package br.pucminas.matriculas.model;

import java.util.List;

/**
 * Onde a Secretaria registra o que cadastra (dependências "Gerencia" do diagrama).
 * Interface do domínio; a implementação fica na camada de persistência.
 */
public interface Cadastros {

    boolean existeUsuario(String id);

    boolean existeCurso(String nome);

    boolean existeDisciplina(String codigo);

    boolean existeSemestre(String identificador);

    void adicionarCurso(Curso curso);

    void adicionarDisciplina(Disciplina disciplina);

    void adicionarProfessor(Professor professor);

    void adicionarAluno(Aluno aluno);

    void adicionarSemestre(Semestre semestre);

    List<Disciplina> listarDisciplinas();

    List<Semestre> listarSemestres();
}
