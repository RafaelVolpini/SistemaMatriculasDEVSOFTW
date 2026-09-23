package br.pucminas.matriculas.app;

import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.model.Aluno;
import br.pucminas.matriculas.model.Curso;
import br.pucminas.matriculas.model.Disciplina;
import br.pucminas.matriculas.model.Professor;
import br.pucminas.matriculas.model.Secretaria;
import br.pucminas.matriculas.model.Semestre;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import br.pucminas.matriculas.persistencia.BancoDeDados;

import java.time.LocalDateTime;

/** Dados de exemplo, criados pelos próprios métodos da Secretaria. Todas as senhas são "123". */
public final class DataSeeder {

    private DataSeeder() {
    }

    public static void popular(BancoDeDados banco) {
        Secretaria secretaria = new Secretaria("secretaria", "Maria (Secretaria)", "secretaria@puc.edu", "123");
        banco.adicionarSecretaria(secretaria);

        Professor hugo = new Professor("prof.hugo", "Hugo Bastos", "hugo@puc.edu", "123");
        Professor lucia = new Professor("prof.lucia", "Lúcia Andrade", "lucia@puc.edu", "123");
        secretaria.cadastrarProfessor(hugo);
        secretaria.cadastrarProfessor(lucia);

        String[][] alunos = {
                {"ana", "Ana Souza", "2026001"},
                {"bruno", "Bruno Lima", "2026002"},
                {"carla", "Carla Mendes", "2026003"},
                {"diego", "Diego Rocha", "2026004"},
                {"elisa", "Elisa Prado", "2026005"},
        };
        for (String[] a : alunos) {
            secretaria.cadastrarAluno(new Aluno(a[0], a[1], a[0] + "@aluno.puc.edu", "123", a[2]));
        }

        Curso es = new Curso("Engenharia de Software", 240);
        secretaria.cadastrarCurso(es);
        Object[][] disciplinas = {
                {"ES101", "Algoritmos e Estruturas de Dados", hugo},
                {"ES102", "Banco de Dados", lucia},
                {"ES103", "Engenharia de Requisitos", lucia},
                {"ES104", "Programação Modular", hugo},
                {"ES105", "Computação Gráfica", hugo},
                {"ES106", "Tópicos em Inteligência Artificial", lucia},
        };
        for (Object[] d : disciplinas) {
            Disciplina disciplina = new Disciplina((String) d[0], (String) d[1]);
            es.adicionarDisciplina(disciplina);
            disciplina.definirProfessor((Professor) d[2]);
            secretaria.cadastrarDisciplina(disciplina);
        }

        Semestre semestre = new Semestre("2026/2");
        semestre.gerarCurriculo(secretaria.disciplinasDisponiveis());
        secretaria.gerarCurriculoSemestre(semestre);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        secretaria.definirPeriodoMatriculas(semestre, agora.minusMinutes(1), agora.plusMinutes(10));

        // Algumas matrículas prévias, para o encerramento ter disciplinas confirmadas e canceladas.
        MuralNotificacoes.getInstance().silenciar(() -> {
            matricular(banco, "ana", "ES101", TipoInscricao.OBRIGATORIA);
            matricular(banco, "ana", "ES102", TipoInscricao.OBRIGATORIA);
            matricular(banco, "bruno", "ES101", TipoInscricao.OBRIGATORIA);
            matricular(banco, "bruno", "ES102", TipoInscricao.OBRIGATORIA);
            matricular(banco, "bruno", "ES103", TipoInscricao.OBRIGATORIA);
            matricular(banco, "carla", "ES101", TipoInscricao.OBRIGATORIA);
            matricular(banco, "carla", "ES103", TipoInscricao.OBRIGATORIA);
            matricular(banco, "carla", "ES105", TipoInscricao.OPTATIVA);
            matricular(banco, "diego", "ES102", TipoInscricao.OBRIGATORIA);
            return null;
        });
    }

    private static void matricular(BancoDeDados banco, String alunoId, String codigo, TipoInscricao tipo) {
        banco.aluno(alunoId).orElseThrow().matricularEmDisciplina(banco.disciplina(codigo).orElseThrow(), tipo);
    }
}
