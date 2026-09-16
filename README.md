# Sistema de Matrículas Universitárias

Grupo: Bernardo Parreiras, Gabriel Marcondes, João Paulo Aguiar e Rafael Volpini

## Visão Geral

A ideia do sistema é informatizar as matrículas de uma universidade. Hoje a secretaria é quem monta o currículo de cada semestre e mantém tudo sobre disciplinas, professores e alunos na mão; um curso tem nome, uma quantidade de créditos e reúne várias disciplinas.

Na prática, durante o período de matrículas o aluno entra no sistema, escolhe até 4 disciplinas obrigatórias (a 1ª opção) e pode complementar com mais 2 optativas. Ele também consegue cancelar uma matrícula feita antes, contanto que ainda esteja dentro do período. No fim do período, o sistema confere quantos alunos ficaram inscritos em cada disciplina: se não chegou a 3, a disciplina é cancelada; se já bateu 60 (o teto de vagas), as inscrições daquela disciplina são fechadas antes mesmo do prazo acabar. Toda vez que uma matrícula é confirmada, o sistema avisa a cobrança para que o aluno seja cobrado pelas disciplinas daquele semestre. Já os professores só precisam entrar para ver quem está matriculado nas turmas deles. E, claro, ninguém acessa nada sem login e senha.

## Atores

| Ator | Descrição |
|---|---|
| *Aluno* | Se matricula e cancela matrículas nas disciplinas do semestre. |
| *Professor* | Entra no sistema só para ver quem está matriculado em cada disciplina sua. |
| *Secretaria* | Cadastra cursos, disciplinas, professores e alunos, monta o currículo do semestre e abre/fecha o período de matrículas. |
| *Sistema de Cobrança* | Fica do lado de fora do sistema de matrículas, mas recebe um aviso a cada matrícula feita. |

## Diagrama de Casos de Uso

<img width="1384" height="1600" alt="casodeusoMatricula" src="https://github.com/user-attachments/assets/0262fc28-db84-43fa-9277-031c1e36ebe3" />

## Casos de Uso

| Código | Caso de Uso | Ator(es) | Descrição resumida |
|---|---|---|---|
| UC1 | Efetuar Login | Aluno, Professor, Secretaria | Entrar no sistema com usuário e senha. |
| UC2 | Gerar Currículo do Semestre | Secretaria | Monta a grade de disciplinas que vai ser oferecida no semestre. |
| UC3 | Cadastrar Disciplinas | Secretaria | Cadastra ou edita disciplinas e associa a um curso. |
| UC4 | Cadastrar Professores | Secretaria | Cadastra ou edita os dados de um professor. |
| UC5 | Cadastrar Alunos | Secretaria | Cadastra ou edita os dados de um aluno. |
| UC6 | Definir Período de Matrículas | Secretaria | Abre a janela em que os alunos podem se matricular ou cancelar. |
| UC7 | Matricular-se em Disciplina Obrigatória | Aluno | Inscreve o aluno em até 4 disciplinas de 1ª opção. |
| UC8 | Matricular-se em Disciplina Optativa | Aluno | Inscreve o aluno em até 2 disciplinas optativas. |
| UC9 | Cancelar Matrícula | Aluno | Desfaz uma matrícula feita antes, ainda dentro do período. |
| UC10 | Consultar Alunos Matriculados | Professor | Mostra ao professor quem está matriculado em cada disciplina dele. |
| UC11 | Encerrar Período de Matrículas | Secretaria | Fecha o período e dispara a checagem do mínimo de alunos por disciplina. |
| UC12 | Validar Login | Sistema | Confere usuário e senha; entra em todo caso de uso que exige acesso. |
| UC13 | Verificar Limite de Vagas | Sistema | Barra a matrícula numa disciplina que já chegou a 60 alunos. |
| UC14 | Verificar Número Mínimo de Alunos | Sistema | No fim do período, confere se a disciplina fechou com pelo menos 3 alunos. |
| UC15 | Notificar Sistema de Cobrança | Sistema | Avisa a cobrança sempre que uma matrícula é confirmada. |
| UC16 | Cancelar Disciplina | Sistema | Estende UC14: cancela a disciplina que não bateu o mínimo de alunos. |

## Histórias de Usuário

### Secretaria

*US01 — Cadastrar disciplinas do semestre*
Como secretaria, quero cadastrar as disciplinas de um curso para que elas entrem no currículo oferecido aos alunos naquele semestre.

*US02 — Cadastrar professores*
Como secretaria, quero cadastrar os professores com sua senha de acesso, assim eles conseguem entrar no sistema e acompanhar suas turmas.

*US03 — Cadastrar alunos*
Como secretaria, quero cadastrar os alunos com sua senha de acesso, para que eles já possam se matricular nas disciplinas do semestre.

*US04 — Gerar o currículo do semestre*
Como secretaria, quero montar o currículo do semestre com base nas disciplinas cadastradas, para que os alunos vejam o que está disponível para matrícula.

*US05 — Definir o período de matrículas*
Como secretaria, quero marcar o início e o fim do período de matrículas, de forma que aluno só consiga se matricular ou cancelar dentro desse prazo.

*US06 — Encerrar o período de matrículas*
Como secretaria, quero encerrar o período de matrículas para que o sistema confira sozinho quais disciplinas bateram o mínimo de 3 alunos e cancele as que ficaram abaixo disso.

### Aluno

*US07 — Efetuar login*
Como aluno, quero entrar no sistema com meu usuário e senha para acessar minha matrícula com segurança.

*US08 — Matricular-se em disciplinas obrigatórias*
Como aluno, quero me matricular em até 4 disciplinas obrigatórias (a 1ª opção) para fechar a grade principal do meu semestre.

*US09 — Matricular-se em disciplinas optativas*
Como aluno, quero me matricular em até 2 disciplinas optativas para completar a grade do semestre.

*US10 — Ser impedido de matricular em disciplina lotada*
Como aluno, quero que o sistema recuse minha matrícula numa disciplina que já tem 60 alunos, para que o limite de vagas seja respeitado.

*US11 — Cancelar matrícula*
Como aluno, quero poder cancelar uma matrícula que já fiz, enquanto o período ainda estiver aberto, caso eu mude de ideia sobre a disciplina.

*US12 — Ser cobrado pelas disciplinas cursadas*
Como aluno, quero que a cobrança seja avisada assim que eu me matricular, para ser cobrado certinho pelas disciplinas daquele semestre.

### Professor

*US13 — Efetuar login*
Como professor, quero entrar no sistema com meu usuário e senha para acessar minhas turmas com segurança.

*US14 — Consultar alunos matriculados*
Como professor, quero ver a lista de alunos matriculados em cada disciplina minha, para já ir me organizando para o semestre.

### Sistema (regras automáticas)

*US15 — Cancelamento automático por falta de quórum*
Como sistema, preciso checar, no fim do período de matrículas, se cada disciplina fechou com pelo menos 3 alunos, cancelando automaticamente as que não chegarem lá.

*US16 — Encerramento automático de inscrições*
Como sistema, preciso fechar as inscrições de uma disciplina assim que ela chegar a 60 alunos matriculados, para não estourar o limite de vagas.

