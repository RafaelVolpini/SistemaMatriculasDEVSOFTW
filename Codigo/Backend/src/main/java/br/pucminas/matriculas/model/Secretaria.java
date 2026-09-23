package br.pucminas.matriculas.model;

import java.time.LocalDateTime;
import java.util.List;

public class Secretaria extends Usuario {

    // não é atributo do diagrama: é o destino das dependências "Gerencia" (injetado pela aplicação)
    private transient Cadastros cadastros;

    public Secretaria(String id, String nome, String email, String senha) {
        super(id, nome, email, senha);
    }

    public void cadastrarCurso(Curso curso) {
        exigirTexto(curso.getNome(), "Nome do curso");
        if (curso.getNumeroCreditos() <= 0) {
            throw new RegraNegocioException("O curso deve ter um número de créditos positivo.");
        }
        if (cadastros().existeCurso(curso.getNome())) {
            throw new RegraNegocioException("Já existe um curso chamado " + curso.getNome() + ".");
        }
        cadastros().adicionarCurso(curso);
    }

    /** A disciplina deve chegar com curso e professor definidos pela chamada (curso ◆ compõe, professor leciona). */
    public void cadastrarDisciplina(Disciplina disciplina) {
        exigirTexto(disciplina.getCodigo(), "Código da disciplina");
        exigirTexto(disciplina.getNome(), "Nome da disciplina");
        if (cadastros().existeDisciplina(disciplina.getCodigo())) {
            throw new RegraNegocioException("Já existe uma disciplina com código " + disciplina.getCodigo() + ".");
        }
        if (disciplina.getCurso() == null) {
            throw new RegraNegocioException("Toda disciplina precisa compor um curso.");
        }
        if (disciplina.getProfessor() == null) {
            throw new RegraNegocioException("Toda disciplina precisa de um professor.");
        }
        cadastros().adicionarDisciplina(disciplina);
    }

    public void cadastrarProfessor(Professor professor) {
        validarUsuario(professor);
        cadastros().adicionarProfessor(professor);
    }

    public void cadastrarAluno(Aluno aluno) {
        validarUsuario(aluno);
        exigirTexto(aluno.getRegistroAcademico(), "Registro acadêmico");
        cadastros().adicionarAluno(aluno);
    }

    /**
     * Registra o currículo montado com {@link Semestre#gerarCurriculo(List)}: exige ao menos uma disciplina e que
     * nenhuma delas já seja ofertada em outro semestre (cada Disciplina é a turma de um semestre, com status e vagas próprios).
     */
    public void gerarCurriculoSemestre(Semestre semestre) {
        exigirTexto(semestre.getIdentificador(), "Identificador do semestre");
        if (semestre.getDisciplinasOfertadas().isEmpty()) {
            throw new RegraNegocioException("Selecione ao menos uma disciplina para o currículo de "
                    + semestre.getIdentificador() + ".");
        }
        for (Disciplina disciplina : semestre.getDisciplinasOfertadas()) {
            cadastros().listarSemestres().stream()
                    .filter(s -> !s.getIdentificador().equals(semestre.getIdentificador()) && s.oferece(disciplina))
                    .findFirst()
                    .ifPresent(s -> {
                        throw new RegraNegocioException(disciplina.getCodigo() + " já é ofertada no semestre "
                                + s.getIdentificador() + ".");
                    });
        }
        if (!cadastros().existeSemestre(semestre.getIdentificador())) {
            cadastros().adicionarSemestre(semestre);
        }
    }

    /** Disciplinas cadastradas que ainda não estão no currículo de nenhum semestre. */
    public List<Disciplina> disciplinasDisponiveis() {
        List<Semestre> semestres = cadastros().listarSemestres();
        return cadastros().listarDisciplinas().stream()
                .filter(d -> semestres.stream().noneMatch(s -> s.oferece(d)))
                .toList();
    }

    /** Define e abre o período; se o semestre ainda não existe, ele passa a existir aqui (o currículo pode vir depois). */
    public void definirPeriodoMatriculas(Semestre semestre, LocalDateTime inicio, LocalDateTime fim) {
        exigirTexto(semestre.getIdentificador(), "Identificador do semestre");
        if (semestre.periodoEncerrado()) {
            throw new RegraNegocioException("O período de " + semestre.getIdentificador() + " já foi encerrado.");
        }
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe o início e o fim do período.");
        }
        PeriodoMatriculas periodo = new PeriodoMatriculas(inicio, fim);
        periodo.abrir();
        semestre.setPeriodo(periodo);
        if (!cadastros().existeSemestre(semestre.getIdentificador())) {
            cadastros().adicionarSemestre(semestre);
        }
    }

    public void encerrarPeriodoMatriculas(Semestre semestre) {
        semestre.encerrarPeriodo();
    }

    private void validarUsuario(Usuario usuario) {
        exigirTexto(usuario.getId(), "Login");
        exigirTexto(usuario.getNome(), "Nome");
        exigirTexto(usuario.getSenha(), "Senha");
        if (cadastros().existeUsuario(usuario.getId())) {
            throw new RegraNegocioException("Já existe um usuário com login " + usuario.getId() + ".");
        }
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(campo + " é obrigatório.");
        }
    }

    private Cadastros cadastros() {
        if (cadastros == null) {
            throw new IllegalStateException("Secretaria sem cadastros configurados.");
        }
        return cadastros;
    }

    public void setCadastros(Cadastros cadastros) {
        this.cadastros = cadastros;
    }
}
