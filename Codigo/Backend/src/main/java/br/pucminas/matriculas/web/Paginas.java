package br.pucminas.matriculas.web;

import br.pucminas.matriculas.app.Formatos;
import br.pucminas.matriculas.externo.MuralNotificacoes;
import br.pucminas.matriculas.externo.Notificacao;
import br.pucminas.matriculas.model.Aluno;
import br.pucminas.matriculas.model.Disciplina;
import br.pucminas.matriculas.model.Matricula;
import br.pucminas.matriculas.model.PeriodoMatriculas;
import br.pucminas.matriculas.model.Professor;
import br.pucminas.matriculas.model.Semestre;
import br.pucminas.matriculas.model.Usuario;
import br.pucminas.matriculas.model.enums.StatusDisciplina;
import br.pucminas.matriculas.model.enums.StatusMatricula;
import br.pucminas.matriculas.model.enums.StatusPeriodo;
import br.pucminas.matriculas.model.enums.TipoInscricao;
import br.pucminas.matriculas.persistencia.BancoDeDados;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static br.pucminas.matriculas.web.Html.chip;
import static br.pucminas.matriculas.web.Html.each;
import static br.pucminas.matriculas.web.Html.esc;
import static br.pucminas.matriculas.web.Html.iniciais;
import static br.pucminas.matriculas.web.Html.option;
import static br.pucminas.matriculas.web.Html.vals;

/** Fragmentos HTML de cada aba. Só lê o domínio; toda alteração passa pelo ServicoAcademico. */
final class Paginas {

    private Paginas() {
    }

    // ================================================================== abas

    static String abas(String ativa) {
        return "<nav id=\"abas\" class=\"abas\" hx-swap-oob=\"true\">"
                + aba("secretaria", "Secretaria", ativa)
                + aba("professor", "Professor", ativa)
                + aba("aluno", "Aluno", ativa)
                + "</nav>";
    }

    private static String aba(String id, String rotulo, String ativa) {
        return "<button class=\"aba" + (id.equals(ativa) ? " ativa" : "") + "\" hx-get=\"/ui/aba/" + id
                + "\" hx-target=\"#conteudo\">" + rotulo + "</button>";
    }

    // ================================================================== secretaria

    static String secretaria(BancoDeDados b) {
        LocalDateTime agora = LocalDateTime.now().withSecond(0);

        String definirPeriodo = card("Definir período de matrículas",
                "Libera as matrículas no intervalo informado. Use um semestre existente ou digite um novo (ex.: 2027/1): "
                        + "ele é criado na hora. No fim do prazo o sistema encerra o período sozinho.",
                "<form hx-post=\"/ui/secretaria/periodo\" hx-target=\"#conteudo\">"
                        + campo("Semestre", "<input name=\"semestre\" required placeholder=\"2027/1\" "
                        + "list=\"semestres-existentes\" autocomplete=\"off\">")
                        + "<div class=\"linha-campos\">"
                        + campo("Início", "<input type=\"datetime-local\" name=\"inicio\" required value=\"" + Formatos.input(agora) + "\">")
                        + campo("Fim", "<input type=\"datetime-local\" name=\"fim\" required value=\""
                        + Formatos.input(agora.plusMinutes(30)) + "\">")
                        + "</div>"
                        + "<button class=\"btn btn-primario\">Definir e abrir período</button></form>");

        String cadastros = "<div class=\"grade grade-4\">"
                + card("Curso", "Nome e total de créditos que o aluno precisa cumprir para se formar (ex.: 240). "
                        + "Os créditos são só descritivos: não entram em nenhuma regra de matrícula.",
                "<form hx-post=\"/ui/secretaria/curso\" hx-target=\"#conteudo\">"
                        + campo("Nome", "<input name=\"nome\" required placeholder=\"Engenharia de Software\">")
                        + campo("Nº de créditos", "<input type=\"number\" name=\"creditos\" min=\"1\" required placeholder=\"240\">")
                        + "<button class=\"btn\">Cadastrar curso</button></form>"
                        + (b.listarCursos().isEmpty() ? "" : "<ul class=\"lista-simples\">" + each(b.listarCursos(), c ->
                        "<li><span>" + esc(c.getNome()) + "</span><small>" + c.getNumeroCreditos() + " créditos · "
                                + c.getDisciplinas().size() + " disc.</small></li>") + "</ul>"))
                + card("Disciplina", "Toda disciplina compõe um curso e tem um professor responsável.",
                "<form hx-post=\"/ui/secretaria/disciplina\" hx-target=\"#conteudo\">"
                        + "<div class=\"linha-campos\">"
                        + campo("Código", "<input name=\"codigo\" required placeholder=\"ES201\">")
                        + campo("Nome", "<input name=\"nome\" required placeholder=\"Arquitetura\">")
                        + "</div>"
                        + campo("Curso", "<select name=\"curso\" required>"
                        + each(b.listarCursos(), c -> option(c.getNome(), c.getNome(), false)) + "</select>")
                        + campo("Professor", "<select name=\"professor\" required>"
                        + each(b.listarProfessores(), p -> option(p.getId(), p.getNome(), false)) + "</select>")
                        + "<button class=\"btn\">Cadastrar disciplina</button></form>")
                + card("Professor", "Recebe login e senha para consultar as próprias turmas.",
                "<form hx-post=\"/ui/secretaria/professor\" hx-target=\"#conteudo\">" + camposUsuario()
                        + "<button class=\"btn\">Cadastrar professor</button></form>")
                + card("Aluno", "Recebe login e senha para se matricular.",
                "<form hx-post=\"/ui/secretaria/aluno\" hx-target=\"#conteudo\">" + camposUsuario()
                        + campo("Registro acadêmico", "<input name=\"ra\" required placeholder=\"2026010\">")
                        + "<button class=\"btn\">Cadastrar aluno</button></form>")
                + "</div>";

        return abas("secretaria")
                + "<datalist id=\"semestres-existentes\">"
                + each(b.listarSemestres(), s -> option(s.getIdentificador(), s.getIdentificador(), false)) + "</datalist>"
                + cabecalhoPagina("Secretaria", "Abra períodos de matrícula, monte o currículo de cada semestre e mantenha os cadastros.",
                "<button class=\"btn btn-fantasma btn-perigo\" hx-post=\"/ui/secretaria/resetar\" hx-target=\"#conteudo\" "
                        + "hx-confirm=\"Apagar todos os dados e recriar o conjunto de exemplo?\">Resetar dados de exemplo</button>")
                + "<div class=\"layout\">"
                + "<div class=\"principal\">"
                + "<div hx-get=\"/ui/secretaria/status\" hx-trigger=\"every 3s\" hx-swap=\"innerHTML\">" + secretariaStatus(b) + "</div>"
                + secao("Configurar semestre", "Passo 1: defina o período. Passo 2: escolha as disciplinas ofertadas (pode ser com o período já aberto).",
                "<div class=\"grade grade-2\">" + definirPeriodo + card("Gerar currículo do semestre", null, curriculo(b)) + "</div>")
                + secao("Cadastros", null, cadastros)
                + "</div>"
                + "<aside class=\"lateral\" hx-get=\"/ui/secretaria/avisos\" hx-trigger=\"every 3s\" hx-swap=\"innerHTML\">"
                + avisos() + "</aside>"
                + "</div>";
    }

    /** Parte da aba Secretaria que se atualiza sozinha a cada 3s (números, semestres e disciplinas). */
    static String secretariaStatus(BancoDeDados b) {
        long abertos = b.listarSemestres().stream().filter(s -> s.getPeriodo() != null && s.getPeriodo().estaAberto()).count();
        long ofertadas = b.listarDisciplinas().stream().filter(d -> b.semestreQueOferece(d).isPresent()).count();
        long matriculasAtivas = b.listarDisciplinas().stream().mapToLong(Disciplina::totalMatriculadosAtivos).sum();

        String numeros = "<div class=\"numeros\">"
                + numero("Semestres", String.valueOf(b.listarSemestres().size()), abertos + " com matrículas abertas")
                + numero("Disciplinas", String.valueOf(b.listarDisciplinas().size()), ofertadas + " em algum currículo")
                + numero("Alunos", String.valueOf(b.listarAlunos().size()), b.listarProfessores().size() + " professores")
                + numero("Matrículas ativas", String.valueOf(matriculasAtivas), "somando todas as disciplinas")
                + "</div>";

        String semestres = b.listarSemestres().isEmpty()
                ? vazio("Nenhum semestre ainda", "Defina um período de matrículas abaixo para criar o primeiro.")
                : "<div class=\"grade grade-semestres\">" + each(b.listarSemestres(), Paginas::cartaoSemestre) + "</div>";

        String disciplinas = b.listarDisciplinas().isEmpty()
                ? vazio("Nenhuma disciplina cadastrada", "Use o cadastro de disciplina abaixo.")
                : tabela(List.of("Disciplina", "Professor", "Semestre", "Ocupação", "Situação", ""),
                each(b.listarDisciplinas(), d -> "<tr>"
                        + "<td>" + nomeDisciplina(d, d.getCurso() == null ? null : d.getCurso().getNome()) + "</td>"
                        + "<td class=\"nowrap\">" + esc(d.getProfessor() == null ? "—" : d.getProfessor().getNome()) + "</td>"
                        + "<td>" + esc(b.semestreQueOferece(d).map(Semestre::getIdentificador).orElse("—")) + "</td>"
                        + "<td>" + ocupacao(d) + "</td>"
                        + "<td>" + chipDisciplina(d) + "</td>"
                        + "<td class=\"col-acoes\"><div class=\"acoes\">" + (d.getStatus() == StatusDisciplina.EM_ABERTO
                        ? "<button class=\"btn btn-mini btn-fantasma\" title=\"Cria alunos de teste e os matricula até lotar (US16)\" "
                        + "hx-post=\"/ui/secretaria/lotar\" hx-target=\"#conteudo\"" + vals("codigo", d.getCodigo())
                        + ">Lotar (teste)</button>" : "")
                        + "</div></td></tr>"));

        return numeros
                + secao("Semestres", null, semestres)
                + secao("Disciplinas", null, "<div class=\"card card-tabela\">" + disciplinas + "</div>");
    }

    private static String cartaoSemestre(Semestre s) {
        PeriodoMatriculas p = s.getPeriodo();
        boolean encerrado = p != null && p.getStatus() == StatusPeriodo.ENCERRADO;
        String curriculo = s.getDisciplinasOfertadas().isEmpty()
                ? "<p class=\"dica alerta\">Sem disciplinas: gere o currículo para os alunos poderem se matricular.</p>"
                : "<div class=\"tags\">" + each(s.getDisciplinasOfertadas(), d -> "<span class=\"tag tag-"
                + d.getStatus().name().toLowerCase() + "\" title=\"" + esc(d.getNome()) + "\">" + esc(d.getCodigo()) + "</span>") + "</div>";

        String acoes = encerrado ? "<span class=\"dica\">Período encerrado: currículo e matrículas congelados.</span>"
                : "<button class=\"btn btn-mini\" hx-post=\"/ui/secretaria/periodo-rapido\" hx-target=\"#conteudo\""
                + vals("semestre", s.getIdentificador(), "minutos", "2") + " title=\"Atalho para testar o encerramento automático\">"
                + "Abrir por 2 min</button>"
                + (p != null && p.getStatus() == StatusPeriodo.ABERTO
                ? "<button class=\"btn btn-mini btn-perigo\" hx-post=\"/ui/secretaria/encerrar\" hx-target=\"#conteudo\""
                + vals("semestre", s.getIdentificador())
                + " hx-confirm=\"Encerrar o período de " + esc(s.getIdentificador()) + " agora? Disciplinas com menos de 3 alunos serão canceladas.\">"
                + "Encerrar agora</button>" : "");

        return "<article class=\"card semestre\">"
                + "<header><h3>" + esc(s.getIdentificador()) + "</h3>" + chipPeriodo(p) + "</header>"
                + "<p class=\"janela\">" + janela(p) + "</p>"
                + curriculo
                + "<footer class=\"acoes\">" + acoes + "</footer>"
                + "</article>";
    }

    static String avisos() {
        List<Notificacao> avisos = new ArrayList<>(MuralNotificacoes.getInstance().ultimas(20));
        Collections.reverse(avisos);
        String lista = avisos.isEmpty() ? vazio("Sem avisos", "Os avisos do Sistema de Cobrança aparecem aqui.")
                : "<ol class=\"avisos\">" + each(avisos, n -> "<li class=\"aviso aviso-" + n.nivel().name().toLowerCase()
                + (Notificacao.ORIGEM_COBRANCA.equals(n.origem()) ? " aviso-cobranca" : "") + "\">"
                + "<div class=\"aviso-topo\"><strong>[" + esc(n.origem()) + "]</strong><small>" + Formatos.hora(n.dataHora())
                + "</small></div><p>" + esc(n.mensagem()) + "</p></li>") + "</ol>";
        return "<div class=\"card\"><h3>Avisos</h3><p class=\"dica\">Tudo que o [Sistema Cobranca] e o sistema enviaram.</p>"
                + lista + "</div>";
    }

    private static String curriculo(BancoDeDados b) {
        List<Disciplina> disponiveis = b.listarSecretarias().isEmpty() ? List.of()
                : b.listarSecretarias().get(0).disciplinasDisponiveis();
        String explicacao = "<p class=\"dica\">Marque as disciplinas que o semestre vai ofertar. Cada disciplina é a turma de "
                + "um semestre (status e vagas próprios), por isso só aparece aqui se ainda não estiver em nenhum currículo.</p>";
        if (disponiveis.isEmpty()) {
            return explicacao + vazio("Nenhuma disciplina disponível",
                    "Todas já estão em algum semestre. Cadastre novas disciplinas em Cadastros para montar outro currículo.");
        }
        String sugerido = b.listarSemestres().size() == 1 ? b.listarSemestres().get(0).getIdentificador() : "";
        return explicacao
                + "<form hx-post=\"/ui/secretaria/curriculo\" hx-target=\"#conteudo\">"
                + campo("Semestre", "<input name=\"identificador\" required placeholder=\"2027/1\" value=\"" + esc(sugerido)
                + "\" list=\"semestres-existentes\" autocomplete=\"off\">")
                + "<fieldset class=\"checks\"><legend>Disciplinas disponíveis</legend>"
                + each(disponiveis, d -> "<label class=\"check\"><input type=\"checkbox\" name=\"disciplinas\" value=\""
                + esc(d.getCodigo()) + "\" checked><span><strong>" + esc(d.getCodigo()) + "</strong> " + esc(d.getNome())
                + "</span></label>")
                + "</fieldset>"
                + "<button class=\"btn btn-primario\">Gerar currículo</button></form>";
    }

    private static String camposUsuario() {
        return "<div class=\"linha-campos\">"
                + campo("Login", "<input name=\"login\" required autocomplete=\"off\">")
                + campo("Senha", "<input type=\"password\" name=\"senha\" required value=\"123\">")
                + "</div>"
                + campo("Nome", "<input name=\"nome\" required>")
                + campo("E-mail", "<input type=\"email\" name=\"email\">");
    }

    // ================================================================== login (aluno e professor)

    static String login(String papel, List<? extends Usuario> usuarios, String titulo) {
        String opcoes = usuarios.isEmpty() ? vazio("Nenhum usuário cadastrado", "Cadastre pela aba Secretaria.")
                : "<div class=\"opcoes-usuario\">" + each(usuarios, u -> "<label class=\"opcao-usuario\">"
                + "<input type=\"radio\" name=\"id\" value=\"" + esc(u.getId()) + "\" required"
                + (u == usuarios.get(0) ? " checked" : "") + ">"
                + "<span class=\"avatar\">" + esc(iniciais(u.getNome())) + "</span>"
                + "<span class=\"opcao-texto\"><strong>" + esc(u.getNome()) + "</strong><small>" + esc(u.getId()) + "</small></span>"
                + "</label>") + "</div>";
        return abas(papel)
                + "<section class=\"login\"><div class=\"card\">"
                + "<h2>" + esc(titulo) + "</h2>"
                + "<p class=\"dica\">Escolha o usuário. A senha de todos os usuários de exemplo é <strong>123</strong> "
                + "(conferida por Usuario.autenticar).</p>"
                + "<form hx-post=\"/ui/" + papel + "/entrar\" hx-target=\"#conteudo\">"
                + opcoes
                + campo("Senha", "<input type=\"password\" name=\"senha\" value=\"123\" required>")
                + "<button class=\"btn btn-primario btn-bloco\">Entrar</button></form>"
                + "</div></section>";
    }

    private static String perfil(String papel, Usuario u, String detalhe) {
        return "<div class=\"perfil\">"
                + "<span class=\"avatar avatar-grande\">" + esc(iniciais(u.getNome())) + "</span>"
                + "<div class=\"perfil-texto\"><h1>" + esc(u.getNome()) + "</h1><p>" + detalhe + "</p></div>"
                + "<button class=\"btn btn-fantasma\" hx-get=\"/ui/aba/" + papel + "?sair=1\" hx-target=\"#conteudo\">Trocar usuário</button>"
                + "</div>";
    }

    // ================================================================== professor

    static String professor(BancoDeDados b, Professor p) {
        String turmas = p.getDisciplinas().isEmpty() ? vazio("Nenhuma disciplina", "A secretaria ainda não atribuiu turmas a você.")
                : "<div class=\"grade grade-2\">" + each(p.getDisciplinas(), d -> {
            List<Aluno> alunos = p.listarAlunosMatriculados(d);
            String lista = alunos.isEmpty() ? vazio("Nenhum aluno matriculado", null)
                    : tabela(List.of("#", "Aluno", "RA"), each(alunos, a -> "<tr><td class=\"num\">" + (alunos.indexOf(a) + 1)
                    + "</td><td><div class=\"celula-dupla\"><strong>" + esc(a.getNome()) + "</strong><small>" + esc(a.getEmail())
                    + "</small></div></td><td>" + esc(a.getRegistroAcademico()) + "</td></tr>"));
            return "<article class=\"card\">"
                    + "<header class=\"card-topo\"><div><h3>" + esc(d.getCodigo()) + " · " + esc(d.getNome()) + "</h3>"
                    + "<p class=\"dica\">Semestre " + esc(b.semestreQueOferece(d).map(Semestre::getIdentificador).orElse("—"))
                    + "</p></div>" + chipDisciplina(d) + "</header>"
                    + ocupacao(d) + lista + "</article>";
        }) + "</div>";
        return abas("professor")
                + perfil("professor", p, esc(p.getEmail()) + " · " + p.getDisciplinas().size() + " disciplina(s)")
                + secao("Minhas turmas", "Alunos com matrícula ativa em cada disciplina (Professor.listarAlunosMatriculados).", turmas);
    }

    // ================================================================== aluno

    static String aluno(BancoDeDados b, Aluno a) {
        int obrig = a.contarMatriculasPorTipo(TipoInscricao.OBRIGATORIA);
        int opt = a.contarMatriculasPorTipo(TipoInscricao.OPTATIVA);
        List<Disciplina> cobradas = a.disciplinasAtivas();

        String resumo = "<div class=\"numeros\">"
                + limite("Obrigatórias", obrig, Aluno.MAX_OBRIGATORIAS)
                + limite("Optativas", opt, Aluno.MAX_OPTATIVAS)
                + "<div class=\"numero numero-largo\"><span class=\"numero-rotulo\">Sendo cobradas pelo Sistema de Cobrança</span>"
                + (cobradas.isEmpty() ? "<p class=\"dica\">Nenhuma disciplina no momento.</p>"
                : "<div class=\"tags\">" + each(cobradas, d -> "<span class=\"tag\" title=\"" + esc(d.getNome()) + "\">"
                + esc(d.getCodigo()) + "</span>") + "</div>")
                + "</div></div>";

        String semestres = b.listarSemestres().isEmpty() ? vazio("Nenhum semestre", "A secretaria ainda não abriu matrículas.")
                : each(b.listarSemestres(), s -> semestreDoAluno(a, s, obrig, opt));

        List<Matricula> historico = a.listarMatriculas().stream()
                .sorted(Comparator.comparing(Matricula::getDataMatricula).reversed()).toList();
        String minhas = historico.isEmpty() ? vazio("Nenhuma matrícula ainda", "Escolha disciplinas acima.")
                : tabela(List.of("Disciplina", "Tipo", "Data", "Situação"), each(historico, m -> "<tr>"
                + "<td>" + nomeDisciplina(m.getDisciplina(), null) + "</td>"
                + "<td>" + esc(capitalizar(Formatos.tipo(m.getTipo()))) + "</td>"
                + "<td class=\"num\">" + Formatos.dataHora(m.getDataMatricula()) + "</td>"
                + "<td>" + (m.getStatus() == StatusMatricula.ATIVA ? chip("Ativa", "verde", "StatusMatricula.ATIVA")
                : chip("Cancelada", "cinza", "StatusMatricula.CANCELADA")) + "</td></tr>"));

        return abas("aluno")
                + perfil("aluno", a, "RA " + esc(a.getRegistroAcademico()) + " · " + esc(a.getEmail()))
                + resumo
                + semestres
                + secao("Histórico de matrículas", null, "<div class=\"card card-tabela\">" + minhas + "</div>");
    }

    private static String semestreDoAluno(Aluno a, Semestre s, int obrig, int opt) {
        PeriodoMatriculas p = s.getPeriodo();
        boolean aberto = p != null && p.estaAberto();
        String aviso = aberto ? ""
                : "<div class=\"faixa\">" + (p == null ? "O período de matrículas deste semestre ainda não foi definido."
                : p.getStatus() == StatusPeriodo.ENCERRADO ? "Período encerrado. As matrículas deste semestre estão fechadas."
                : p.venceu() ? "O prazo terminou e o período está sendo encerrado."
                : "As matrículas abrem em " + Formatos.dataHora(p.getDataInicio()) + ".") + "</div>";

        String corpo = s.getDisciplinasOfertadas().isEmpty()
                ? vazio("Currículo ainda não gerado", "A secretaria ainda não escolheu as disciplinas deste semestre.")
                : tabela(List.of("Disciplina", "Professor", "Vagas", "Situação", ""),
                each(s.getDisciplinasOfertadas(), d -> "<tr>"
                        + "<td>" + nomeDisciplina(d, null) + "</td>"
                        + "<td class=\"nowrap\">" + esc(d.getProfessor() == null ? "—" : d.getProfessor().getNome()) + "</td>"
                        + "<td>" + ocupacao(d) + "</td>"
                        + "<td>" + chipDisciplina(d) + "</td>"
                        + "<td class=\"col-acoes\"><div class=\"acoes\">" + acaoAluno(a, d, aberto, obrig, opt) + "</div></td>"
                        + "</tr>"));

        return secao("Semestre " + s.getIdentificador(), null,
                "<div class=\"card card-tabela\"><div class=\"card-faixa-topo\">" + chipPeriodo(p)
                        + "<span class=\"janela\">" + janela(p) + "</span></div>" + aviso + corpo + "</div>");
    }

    private static String acaoAluno(Aluno a, Disciplina d, boolean periodoAberto, int obrig, int opt) {
        Optional<Matricula> ativa = a.listarMatriculas().stream()
                .filter(m -> m.estaAtiva() && m.getDisciplina() == d).findFirst();
        if (ativa.isPresent()) {
            String tipo = Formatos.tipo(ativa.get().getTipo());
            return chip("Matriculado · " + tipo, "roxo")
                    + (periodoAberto ? "<button class=\"btn btn-mini btn-fantasma btn-perigo\" hx-post=\"/ui/aluno/cancelar\" "
                    + "hx-target=\"#conteudo\"" + vals("id", a.getId(), "codigo", d.getCodigo())
                    + " hx-confirm=\"Cancelar a matrícula em " + esc(d.getCodigo()) + "?\">Cancelar</button>" : "");
        }
        if (d.getStatus() != StatusDisciplina.EM_ABERTO || !periodoAberto) {
            return "<span class=\"dica\">—</span>";
        }
        return botaoMatricular(a, d, TipoInscricao.OBRIGATORIA, "Obrigatória", obrig >= Aluno.MAX_OBRIGATORIAS)
                + botaoMatricular(a, d, TipoInscricao.OPTATIVA, "Optativa", opt >= Aluno.MAX_OPTATIVAS);
    }

    private static String botaoMatricular(Aluno a, Disciplina d, TipoInscricao tipo, String rotulo, boolean noLimite) {
        String classe = tipo == TipoInscricao.OBRIGATORIA ? "btn btn-mini btn-primario" : "btn btn-mini";
        if (noLimite) {
            return "<button class=\"" + classe + "\" disabled title=\"Limite de " + rotulo.toLowerCase() + "s atingido\">+ "
                    + rotulo + "</button>";
        }
        return "<button class=\"" + classe + "\" hx-post=\"/ui/aluno/matricular\" hx-target=\"#conteudo\""
                + vals("id", a.getId(), "codigo", d.getCodigo(), "tipo", tipo.name())
                + " title=\"Matricular como " + rotulo.toLowerCase() + "\">+ " + rotulo + "</button>";
    }

    private static String limite(String rotulo, int valor, int maximo) {
        int pct = Math.min(100, valor * 100 / maximo);
        return "<div class=\"numero\"><span class=\"numero-rotulo\">" + rotulo + "</span>"
                + "<strong>" + valor + "<small> / " + maximo + "</small></strong>"
                + "<div class=\"barra\"><span class=\"" + (valor >= maximo ? "cheia" : "ok") + "\" style=\"width:" + pct + "%\"></span></div>"
                + "<span class=\"numero-sub\">" + (valor >= maximo ? "limite atingido" : (maximo - valor) + " restante(s)") + "</span></div>";
    }

    // ================================================================== notificações (toasts)

    static String poller(long desde) {
        return "<div id=\"poller\" hx-get=\"/ui/notificacoes?desde=" + desde + "\" "
                + "hx-trigger=\"every 2s, novas-notificacoes from:body\" hx-swap=\"outerHTML\"></div>";
    }

    static String notificacoes(List<Notificacao> novas, long ultimoId) {
        String toasts = novas.isEmpty() ? "" : "<div hx-swap-oob=\"beforeend:#toasts\">" + each(novas, Html::toast) + "</div>";
        return poller(ultimoId) + toasts;
    }

    // ================================================================== peças comuns

    private static String cabecalhoPagina(String titulo, String subtitulo, String acao) {
        return "<div class=\"cabecalho-pagina\"><div><h1>" + esc(titulo) + "</h1><p>" + esc(subtitulo) + "</p></div>"
                + acao + "</div>";
    }

    private static String secao(String titulo, String descricao, String conteudo) {
        return "<section class=\"secao\"><div class=\"secao-topo\"><h2>" + esc(titulo) + "</h2>"
                + (descricao == null ? "" : "<p>" + esc(descricao) + "</p>") + "</div>" + conteudo + "</section>";
    }

    private static String card(String titulo, String descricao, String corpo) {
        return "<article class=\"card\"><h3>" + esc(titulo) + "</h3>"
                + (descricao == null ? "" : "<p class=\"dica\">" + esc(descricao) + "</p>") + corpo + "</article>";
    }

    private static String numero(String rotulo, String valor, String sub) {
        return "<div class=\"numero\"><span class=\"numero-rotulo\">" + esc(rotulo) + "</span><strong>" + esc(valor)
                + "</strong><span class=\"numero-sub\">" + esc(sub) + "</span></div>";
    }

    private static String vazio(String titulo, String texto) {
        return "<div class=\"vazio\"><strong>" + esc(titulo) + "</strong>" + (texto == null ? "" : "<p>" + esc(texto) + "</p>")
                + "</div>";
    }

    private static String campo(String rotulo, String input) {
        return "<label class=\"campo\"><span>" + esc(rotulo) + "</span>" + input + "</label>";
    }

    private static String tabela(List<String> cabecalhos, String linhas) {
        return "<div class=\"tabela\"><table><thead><tr>" + each(cabecalhos, c -> "<th>" + esc(c) + "</th>")
                + "</tr></thead><tbody>" + linhas + "</tbody></table></div>";
    }

    private static String nomeDisciplina(Disciplina d, String detalhe) {
        return "<div class=\"celula-dupla\"><span><span class=\"codigo\">" + esc(d.getCodigo()) + "</span> "
                + esc(d.getNome()) + "</span>" + (detalhe == null ? "" : "<small>" + esc(detalhe) + "</small>") + "</div>";
    }

    private static String ocupacao(Disciplina d) {
        int ativos = d.totalMatriculadosAtivos();
        int pct = Math.min(100, ativos * 100 / d.getVagasMaximas());
        String tom = d.atingiuLimiteVagas() ? "cheia" : (d.atingiuMinimoAlunos() ? "ok" : "baixa");
        String dica = d.atingiuMinimoAlunos() ? ativos + " de " + d.getVagasMaximas() + " vagas ocupadas"
                : "Abaixo do mínimo de " + d.getMinimosAlunos() + " alunos: será cancelada se terminar assim";
        return "<div class=\"ocupacao\" title=\"" + esc(dica) + "\"><div class=\"barra\"><span class=\"" + tom
                + "\" style=\"width:" + Math.max(pct, ativos > 0 ? 3 : 0) + "%\"></span></div>"
                + "<small><strong>" + ativos + "</strong>/" + d.getVagasMaximas()
                + (d.atingiuMinimoAlunos() ? "" : " · mín. " + d.getMinimosAlunos()) + "</small></div>";
    }

    private static String chipDisciplina(Disciplina d) {
        String enumeracao = "StatusDisciplina." + d.getStatus().name();
        return switch (d.getStatus()) {
            case EM_ABERTO -> chip("Inscrições abertas", "azul", enumeracao);
            case ATIVA -> d.atingiuLimiteVagas() ? chip("Lotada", "ambar", enumeracao) : chip("Confirmada", "verde", enumeracao);
            case CANCELADA -> chip("Cancelada", "vermelho", enumeracao);
        };
    }

    private static String chipPeriodo(PeriodoMatriculas p) {
        if (p == null) {
            return chip("Sem período", "cinza");
        }
        String enumeracao = "StatusPeriodo." + p.getStatus().name();
        if (p.estaAberto()) {
            return chip("Matrículas abertas", "verde", enumeracao);
        }
        if (p.venceu()) {
            return chip("Encerrando…", "ambar", enumeracao);
        }
        if (p.getStatus() == StatusPeriodo.ABERTO) {
            return chip("Agendado", "azul", enumeracao);
        }
        return chip("Encerrado", "cinza", enumeracao);
    }

    private static String janela(PeriodoMatriculas p) {
        if (p == null) {
            return "Período não definido";
        }
        return Formatos.curto(p.getDataInicio()) + " → " + Formatos.curto(p.getDataFim())
                + (p.estaAberto() ? " · fecha às " + Formatos.hora(p.getDataFim()) : "");
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
