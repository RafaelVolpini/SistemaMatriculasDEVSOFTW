package br.pucminas.matriculas.persistencia;

import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.externo.Notificacao;
import br.pucminas.matriculas.externo.SistemaCobranca;
import br.pucminas.matriculas.model.Aluno;
import br.pucminas.matriculas.model.Cadastros;
import br.pucminas.matriculas.model.Curso;
import br.pucminas.matriculas.model.Disciplina;
import br.pucminas.matriculas.model.Matricula;
import br.pucminas.matriculas.model.PeriodoMatriculas;
import br.pucminas.matriculas.model.Professor;
import br.pucminas.matriculas.model.Secretaria;
import br.pucminas.matriculas.model.Semestre;
import br.pucminas.matriculas.model.enums.StatusDisciplina;
import br.pucminas.matriculas.model.enums.StatusMatricula;
import br.pucminas.matriculas.model.enums.StatusPeriodo;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Infraestrutura (não modelada no diagrama): mantém os objetos do domínio em memória e os grava em arquivos JSON.
 *
 * <p>O grafo do domínio tem ciclos (Aluno ↔ Matricula ↔ Disciplina ↔ Professor/Curso), então cada arquivo guarda
 * registros planos que se referenciam por id; ao carregar, as referências são religadas.
 */
public class BancoDeDados implements Cadastros {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (valor, tipo, ctx) -> new JsonPrimitive(valor.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, tipo, ctx) -> LocalDateTime.parse(json.getAsString()))
            .create();

    private final Path diretorio;
    private final Map<String, Curso> cursos = new LinkedHashMap<>();
    private final Map<String, Disciplina> disciplinas = new LinkedHashMap<>();
    private final Map<String, Professor> professores = new LinkedHashMap<>();
    private final Map<String, Aluno> alunos = new LinkedHashMap<>();
    private final Map<String, Secretaria> secretarias = new LinkedHashMap<>();
    private final Map<String, Semestre> semestres = new LinkedHashMap<>();

    public BancoDeDados(Path diretorio) {
        this.diretorio = diretorio;
    }

    // ---------------------------------------------------------------- Cadastros

    @Override
    public boolean existeUsuario(String id) {
        return professores.containsKey(id) || alunos.containsKey(id) || secretarias.containsKey(id);
    }

    @Override
    public boolean existeCurso(String nome) {
        return cursos.containsKey(nome);
    }

    @Override
    public boolean existeDisciplina(String codigo) {
        return disciplinas.containsKey(codigo);
    }

    @Override
    public boolean existeSemestre(String identificador) {
        return semestres.containsKey(identificador);
    }

    @Override
    public void adicionarCurso(Curso curso) {
        cursos.put(curso.getNome(), curso);
    }

    @Override
    public void adicionarDisciplina(Disciplina disciplina) {
        disciplinas.put(disciplina.getCodigo(), disciplina);
    }

    @Override
    public void adicionarProfessor(Professor professor) {
        professores.put(professor.getId(), professor);
    }

    @Override
    public void adicionarAluno(Aluno aluno) {
        alunos.put(aluno.getId(), aluno);
    }

    @Override
    public void adicionarSemestre(Semestre semestre) {
        semestres.put(semestre.getIdentificador(), semestre);
    }

    @Override
    public List<Disciplina> listarDisciplinas() {
        return List.copyOf(disciplinas.values());
    }

    @Override
    public List<Semestre> listarSemestres() {
        return List.copyOf(semestres.values());
    }

    public void adicionarSecretaria(Secretaria secretaria) {
        secretaria.setCadastros(this);
        secretarias.put(secretaria.getId(), secretaria);
    }

    // ---------------------------------------------------------------- consultas

    public List<Curso> listarCursos() {
        return List.copyOf(cursos.values());
    }

    public List<Professor> listarProfessores() {
        return List.copyOf(professores.values());
    }

    public List<Aluno> listarAlunos() {
        return List.copyOf(alunos.values());
    }

    public List<Secretaria> listarSecretarias() {
        return List.copyOf(secretarias.values());
    }

    public Optional<Curso> curso(String nome) {
        return Optional.ofNullable(cursos.get(nome));
    }

    public Optional<Disciplina> disciplina(String codigo) {
        return Optional.ofNullable(disciplinas.get(codigo));
    }

    public Optional<Professor> professor(String id) {
        return Optional.ofNullable(professores.get(id));
    }

    public Optional<Aluno> aluno(String id) {
        return Optional.ofNullable(alunos.get(id));
    }

    public Optional<Semestre> semestre(String identificador) {
        return Optional.ofNullable(semestres.get(identificador));
    }

    public Optional<Semestre> semestreQueOferece(Disciplina disciplina) {
        return semestres.values().stream().filter(s -> s.oferece(disciplina)).findFirst();
    }

    public boolean vazio() {
        return secretarias.isEmpty();
    }

    // ---------------------------------------------------------------- arquivos

    public boolean existemArquivos() {
        return Files.exists(diretorio.resolve("secretarias.json"));
    }

    public void salvar() {
        try {
            Files.createDirectories(diretorio);
            escrever("cursos.json", cursos.values().stream()
                    .map(c -> new CursoRegistro(c.getNome(), c.getNumeroCreditos())).toList());
            escrever("professores.json", professores.values().stream()
                    .map(p -> new UsuarioRegistro(p.getId(), p.getNome(), p.getEmail(), p.getSenha(), null)).toList());
            escrever("alunos.json", alunos.values().stream()
                    .map(a -> new UsuarioRegistro(a.getId(), a.getNome(), a.getEmail(), a.getSenha(), a.getRegistroAcademico())).toList());
            escrever("secretarias.json", secretarias.values().stream()
                    .map(s -> new UsuarioRegistro(s.getId(), s.getNome(), s.getEmail(), s.getSenha(), null)).toList());
            escrever("disciplinas.json", disciplinas.values().stream()
                    .map(d -> new DisciplinaRegistro(d.getCodigo(), d.getNome(), d.getStatus(),
                            d.getCurso() == null ? null : d.getCurso().getNome(),
                            d.getProfessor() == null ? null : d.getProfessor().getId()))
                    .toList());
            escrever("matriculas.json", alunos.values().stream()
                    .flatMap(a -> a.listarMatriculas().stream())
                    .sorted(Comparator.comparing(Matricula::getDataMatricula))
                    .map(m -> new MatriculaRegistro(m.getAluno().getId(), m.getDisciplina().getCodigo(),
                            m.getTipo(), m.getStatus(), m.getDataMatricula()))
                    .toList());
            escrever("semestres.json", semestres.values().stream()
                    .map(s -> new SemestreRegistro(s.getIdentificador(),
                            s.getDisciplinasOfertadas().stream().map(Disciplina::getCodigo).toList(),
                            s.getPeriodo() == null ? null : new PeriodoRegistro(
                                    s.getPeriodo().getDataInicio(), s.getPeriodo().getDataFim(), s.getPeriodo().getStatus())))
                    .toList());
            escrever("notificacoes.json", MuralNotificacoes.getInstance().todas());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar dados em " + diretorio, e);
        }
    }

    public void carregar() {
        limparMemoria();
        for (CursoRegistro r : ler("cursos.json", new TypeToken<List<CursoRegistro>>() {})) {
            cursos.put(r.nome(), new Curso(r.nome(), r.numeroCreditos()));
        }
        for (UsuarioRegistro r : lerUsuarios("professores.json")) {
            professores.put(r.id(), new Professor(r.id(), r.nome(), r.email(), r.senha()));
        }
        for (UsuarioRegistro r : lerUsuarios("alunos.json")) {
            alunos.put(r.id(), new Aluno(r.id(), r.nome(), r.email(), r.senha(), r.registroAcademico()));
        }
        for (UsuarioRegistro r : lerUsuarios("secretarias.json")) {
            adicionarSecretaria(new Secretaria(r.id(), r.nome(), r.email(), r.senha()));
        }
        for (DisciplinaRegistro r : ler("disciplinas.json",
                new TypeToken<List<DisciplinaRegistro>>() {})) {
            Disciplina disciplina = new Disciplina(r.codigo(), r.nome());
            if (r.curso() != null && cursos.containsKey(r.curso())) {
                cursos.get(r.curso()).adicionarDisciplina(disciplina);
            }
            if (r.professorId() != null) {
                disciplina.definirProfessor(professores.get(r.professorId()));
            }
            disciplina.restaurarStatus(r.status());
            disciplinas.put(r.codigo(), disciplina);
        }
        for (MatriculaRegistro r : ler("matriculas.json",
                new TypeToken<List<MatriculaRegistro>>() {})) {
            Aluno aluno = alunos.get(r.alunoId());
            Disciplina disciplina = disciplinas.get(r.disciplinaCodigo());
            if (aluno != null && disciplina != null) {
                Matricula.restaurar(aluno, disciplina, r.tipo(), r.status(), r.dataMatricula());
            }
        }
        for (SemestreRegistro r : ler("semestres.json",
                new TypeToken<List<SemestreRegistro>>() {})) {
            Semestre semestre = new Semestre(r.identificador());
            semestre.gerarCurriculo(r.disciplinas().stream().map(disciplinas::get).filter(d -> d != null).toList());
            if (r.periodo() != null) {
                semestre.setPeriodo(new PeriodoMatriculas(r.periodo().inicio(), r.periodo().fim(), r.periodo().status()));
            }
            semestres.put(r.identificador(), semestre);
        }
        MuralNotificacoes.getInstance().restaurar(ler("notificacoes.json",
                new TypeToken<List<Notificacao>>() {}));
        SistemaCobranca.getInstance().sincronizar(listarAlunos());
    }

    /** Apaga os arquivos e a memória (botão "Resetar dados" da interface). */
    public void apagarTudo() {
        limparMemoria();
        MuralNotificacoes.getInstance().limpar();
        SistemaCobranca.getInstance().limpar();
        try {
            if (Files.exists(diretorio)) {
                try (var arquivos = Files.list(diretorio)) {
                    for (Path arquivo : arquivos.toList()) {
                        Files.deleteIfExists(arquivo);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void limparMemoria() {
        cursos.clear();
        disciplinas.clear();
        professores.clear();
        alunos.clear();
        secretarias.clear();
        semestres.clear();
    }

    private void escrever(String arquivo, Object conteudo) throws IOException {
        Path destino = diretorio.resolve(arquivo);
        Path temporario = diretorio.resolve(arquivo + ".tmp");
        Files.writeString(temporario, GSON.toJson(conteudo), StandardCharsets.UTF_8);
        try {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<UsuarioRegistro> lerUsuarios(String arquivo) {
        return ler(arquivo, new TypeToken<List<UsuarioRegistro>>() {});
    }

    private <T> List<T> ler(String arquivo, TypeToken<List<T>> tipoLista) {
        Path origem = diretorio.resolve(arquivo);
        if (!Files.exists(origem)) {
            return List.of();
        }
        try {
            List<T> itens = GSON.fromJson(Files.readString(origem, StandardCharsets.UTF_8), tipoLista);
            return itens == null ? List.of() : new ArrayList<>(itens);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + origem, e);
        }
    }

    // ---------------------------------------------------------------- registros gravados em disco

    private record CursoRegistro(String nome, int numeroCreditos) {
    }

    private record UsuarioRegistro(String id, String nome, String email, String senha, String registroAcademico) {
    }

    private record DisciplinaRegistro(String codigo, String nome, StatusDisciplina status, String curso, String professorId) {
    }

    private record MatriculaRegistro(String alunoId, String disciplinaCodigo, TipoInscricao tipo,
                                     StatusMatricula status, LocalDateTime dataMatricula) {
    }

    private record SemestreRegistro(String identificador, List<String> disciplinas, PeriodoRegistro periodo) {
    }

    private record PeriodoRegistro(LocalDateTime inicio, LocalDateTime fim, StatusPeriodo status) {
    }
}
