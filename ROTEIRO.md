# Roteiro do Freecol 2.0

O jogo já tem mecânicas sólidas. O que cansa é o volume de cliques e de
interrupções, e a informação escondida. Este roteiro junta o que aprendemos
com as críticas do Civilization V e organiza as mudanças em fases.

A pergunta que guia tudo: **em cada turno, o jogador está decidindo ou só
trabalhando?** Decisão é diversão. Trabalho braçal se automatiza, espera se
corta e confusão se explica.

Legenda: ✅ feito · 🔨 em andamento · ⬜ a fazer

## Já feito

- ✅ Tela da colônia como uma cidade, com terreno, ruas, paliçada que evolui e coluna da direita em pergaminho
- ✅ Prévia do que o colono vai produzir ao ser arrastado
- ✅ Agenda dos próximos turnos no mapa, clicável
- ✅ Objetivos como sequência de conquistas, com recompensa
- ✅ Tela da Europa como mesa de comerciante
- ✅ Explorar sozinho (tropas e navios) e Navegar para a Europa num clique
- ✅ Linha da Europa na agenda, com os colonos esperando no cais
- ✅ Mover com o botão direito, como no Civilization
- ✅ Guia do iniciante, que pode ser pulado
- ✅ Encontro com outros europeus simplificado
- ✅ Europa clara: como comprar e vender, e os espaços de cada navio
- ✅ Protesto contra a Coroa diz o que vai ser jogado fora
- ✅ Tela inicial com menos coisas ao mesmo tempo

## Fase 1 — Menos trabalho braçal

- ✅ **Foco da colônia**: botões Equilíbrio / Comida / Martelos / Sinos que distribuem os colonos sozinhos
- ✅ **Mover em grupo**: Ctrl + botão direito leva todas as unidades da casa
- ⬜ **Rota colônia ↔ Europa num clique**: vende o excedente e traz os colonos do cais
- ⬜ **Pioneiro automático**: melhora os terrenos das colônias sozinho
- ⬜ Foco da colônia que se mantém: os colonos novos já entram no lugar certo

## Fase 2 — Menos interrupções

- ⬜ **Avisos sem bloquear a tela**: painel lateral com os avisos agrupados, no lugar das janelas em sequência
- ⬜ **Propostas recusadas não voltam** por vários turnos
- ⬜ Aviso na agenda quando o armazém vai transbordar
- ⬜ **Fim de turno que diz o que falta** ("2 unidades esperando ordens")
- ⬜ "Decidir depois" nas janelas que não precisam de resposta na hora
- ⬜ Diário da partida: o jogo grava tempo por turno, ordens dadas e janelas abertas, para medir onde cansa

## Fase 3 — Um meio de jogo vivo

- ⬜ **Acontecimentos**: tempestade, epidemia, piratas na costa, revolta de colonos leais à Coroa, imigrante famoso, contrato de compra
- ⬜ Freios visíveis: avisar quando o Rei reforça o exército, e por quê
- ⬜ Tendência dos preços no mercado (subindo, caindo)
- ⬜ Números que explicam: passar o mouse e ver de onde vem cada parte da produção

## Fase 4 — Vontade de jogar de novo

- ⬜ **Modo Vingança: o Holandês Voador**. Já existe: quando o jogador é derrotado, volta como o navio amaldiçoado para se vingar. Melhorias:
  - nome que se usa no Brasil ("O Holandês Voador")
  - mensagem de derrota que explica o modo, em vez da citação solta
  - alvos da vingança e placar próprio
  - arte do navio fantasma e da tripulação, e o mapa com ar sombrio
  - um fim próprio para a vingança
- ⬜ **Vitórias alternativas**: econômica, por influência com os nativos, por pontos num ano-limite
- ⬜ **Conquistas no estilo Steam**, para platinar, guardadas entre partidas: plano em `CONQUISTAS.md`
- ⬜ **Retrospectiva no fim**: gráfico dos pontos, colônias fundadas, pais fundadores, batalhas
- ⬜ Congresso Continental com o efeito de cada Pai Fundador claro

## Fase 5 — Espetáculo

- ⬜ Mapa vivo: fumaça nas colônias, rastro dos navios, animais, colônias que mudam ao crescer
- ⬜ Arte gerada a partir dos prompts em `data/default/resources/images/town/PROMPTS.md`
- ⬜ Retratos dos líderes e humor na diplomacia
- ⬜ Cenários com objetivos próprios

## Fase 6 — Uma IA mais esperta

- ⬜ IA que usa o terreno (colinas, florestas, fortificação) em vez de trapacear nos níveis difíceis
- ⬜ Diplomacia previsível: humor visível, memória dos favores, motivos claros
- ⬜ IA que transporta tropas por mar sem se expor

## Para estudar

- Artigo "MDA: A Formal Approach to Game Design" (Hunicke, LeBlanc e Zubek)
- Palestra "Interesting Decisions", de Sid Meier (GDC 2012)
- *A Arte de Game Design*, de Jesse Schell
- *Designing Games*, de Tynan Sylvester
- *Game Mechanics: Advanced Game Design*, de Adams e Dormans
- Podcast "Designer Notes", de Soren Johnson
