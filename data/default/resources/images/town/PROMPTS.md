# Arte da cidade: prompts e formato

Imagens para deixar a tela da colônia mais realista, como nos conceitos. O código
monta a cena em camadas com estas peças. **Qualquer imagem que faltar continua
desenhada pelo código como hoje**, então dá para gerar aos poucos, na ordem
desta lista.

## Onde salvar

Salve cada imagem nesta mesma pasta, com o nome exato indicado:

```
C:\Users\Lucas\Desktop\Freecol 2.0\data\default\resources\images\town\
```

## Formato (vale para todas)

- **Comece sempre pedindo a imagem.** Todo prompt abre com "Generate an
  image:". Sem isso, o Gemini às vezes responde em texto em vez de gerar.
  Cole um prompt por vez, numa conversa nova se ele começar a misturar.
- **PNG.** Se a ferramenta entregar JPG ou WEBP, salve assim mesmo, que eu
  converto.
- **Tamanho:** o maior que a ferramenta fizer, na proporção indicada. Se ela só
  fizer quadrado, tudo bem: eu recorto e redimensiono. No Gemini dá para
  escrever no fim do prompt, por exemplo, "wide 3:1 panoramic format".
- **Fundo magenta** (#FF00FF) nas peças avulsas (cercas, árvores, animais...).
  O Gemini não faz fundo transparente; o jogo remove o magenta sozinho. Se a
  ferramenta fizer transparência de verdade, como o ChatGPT, pode trocar por
  "transparent background".
- **Sem texto, letras, moldura ou marca d'água.**
- **Mesma luz e mesmo ângulo em todas:** luz de fim de tarde vindo de cima à
  esquerda, câmera em 3/4 olhando de cima, como nas construções do jogo. Por
  isso todo prompt começa com o mesmo bloco de estilo. Mantenha esse bloco
  igual em todos.
- **Os prompts não citam jogos pelo nome.** Pedir "no estilo de" um jogo pode
  fazer o gerador recusar. Se quiser reforçar o estilo, mande junto uma
  captura da tela da colônia como referência e escreva "match the style of
  the attached screenshot".
- Gere duas ou três versões de cada uma e escolha a que mais combinar com as
  outras. Consistência entre as peças importa mais que uma peça perfeita.
- **Se ainda assim não gerar,** me mande a mensagem que aparecer, que eu ajusto
  o prompt.

---

## Parte 1: essenciais (mudam mais a cara da cidade)

### 1. `sky_temperate.png`: céu e horizonte
Proporção **3:1** (ex.: 3072x1024). Sem transparência.
Fica no alto da tela, atrás da paliçada e da faixa com o nome.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
Wide panoramic landscape seen from a low hill: a soft blue sky with a few light clouds, a distant horizon of gentle rolling green hills and dark temperate forests fading into atmospheric haze, a few lone trees in the middle distance. The horizon line sits at about 40% from the top. The bottom quarter of the image is an even meadow of short green grass with no objects, paths or flowers, so it can blend seamlessly into the ground of a town. No buildings, no people, no animals, no roads, no fences, no text, no border.
```

### 2. `ground_grass.png`: grama (textura contínua)
Proporção **1:1** (ex.: 1024x1024). Sem transparência.
O chão da cidade inteira.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors.
Seamless tileable texture of short meadow grass with small patches of clover and a little bare earth, viewed straight from above, evenly lit with no strong shadows or highlights, no flowers or distinct objects that would make the repetition obvious. Must tile seamlessly in both directions. No text, no border.
```

### 3. `ground_dirt.png`: terra batida (textura contínua)
Proporção **1:1**. Sem transparência.
Ruas e quintais.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors.
Seamless tileable texture of packed brown dirt road with fine gravel, a few small pebbles and faint footprints, viewed straight from above, evenly lit with no strong shadows, no wheel ruts or lines in any direction. Must tile seamlessly in both directions. No text, no border.
```

### 4. `ground_cobble.png`: calçamento da praça (textura contínua)
Proporção **1:1**. Sem transparência.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors.
Seamless tileable texture of a rustic town square paved with rounded grey and sandy cobblestones of uneven sizes, with a little dirt and moss in the joints, viewed straight from above, evenly lit with no strong shadows. Must tile seamlessly in both directions. No text, no border.
```

### 5. `palisade.png`: paliçada (segmento contínuo na horizontal)
Proporção **4:1** (ex.: 2048x512). **Fundo magenta.**
Muro de troncos atrás da cidade. Aparece quando a colônia constrói a paliçada.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A straight section of a colonial log palisade wall made of tall vertical sharpened logs bound together, seen from the front and slightly above (about 25 degrees). The section fills the whole width and must tile seamlessly horizontally: the left and right edges must match so copies can be placed side by side. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no grass, no cast shadow, no gate, no text, no border.
```

### 6. `fence_rail.png`: cerca de trilho (segmento contínuo na horizontal)
Proporção **8:1** (ex.: 2048x256). **Fundo magenta.**
Cerca dos lotes e das plantações.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A straight section of a rustic split-rail wooden fence: weathered wooden posts with two horizontal rails, seen from the front and slightly above (about 25 degrees). The fence fills the whole width and must tile seamlessly horizontally: the left and right edges must match. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no grass, no cast shadow, no text, no border.
```

### 7. `hedge.png`: sebe ou cerca viva (segmento contínuo na horizontal)
Proporção **6:1** (ex.: 2048x340). **Fundo magenta.**

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A straight section of a low, neatly trimmed green hedge, about knee height, seen from the front and slightly above (about 25 degrees). It fills the whole width and must tile seamlessly horizontally: the left and right edges must match. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no cast shadow, no flowers, no text, no border.
```

### 8. `well.png`: poço da praça
Proporção **1:1**. **Fundo magenta.**

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A single round stone village well with a small wooden roof, a crank and a hanging wooden bucket. Three-quarter view from the front, camera elevated about 35 degrees, like an isometric strategy game. Centered, filling about 80% of the frame. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no cast shadow, no text, no border.
```

### 9. `tree_broadleaf.png`: árvore folhosa avulsa
Proporção **1:1**. **Fundo magenta.**

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A single isolated oak tree with a full round leafy crown and a visible trunk. Three-quarter view from the front, camera elevated about 35 degrees, like an isometric strategy game. Centered, filling about 85% of the frame. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no grass, no cast shadow, no text, no border.
```

### 10. `tree_conifer.png`: pinheiro avulso
Proporção **1:1** (ou 2:3 em pé). **Fundo magenta.**

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A single isolated tall pine tree, dark green, with a slender trunk. Three-quarter view from the front, camera elevated about 35 degrees, like an isometric strategy game. Centered, filling about 85% of the frame height. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no grass, no cast shadow, no text, no border.
```

### 11. `banner_scroll.png`: pergaminho para o nome da colônia
Proporção **4:1** (ex.: 2048x512). **Fundo magenta.**
**Sem nenhum texto**: o jogo escreve o nome por cima, no centro.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Warm, slightly desaturated colors, soft light from the upper left.
An old parchment scroll banner, unrolled horizontally, with curled rolled ends on the left and right and a slightly worn cream surface. The middle of the scroll is a wide, plain, empty area with no writing at all. Front view. Solid flat magenta background (#FF00FF) with nothing else on it. Absolutely no text, no letters, no symbols, no seals, no border.
```

---

## Parte 2: extras (vida e variedade)

### 12 a 14. Plantações (texturas contínuas em fileiras)
Proporção **2:1** (ex.: 2048x1024). Sem transparência.
Salve como `field_wheat.png`, `field_vegetables.png` e `field_tobacco.png`.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors.
Seamless tileable texture of a farm field of ripe golden wheat planted in straight horizontal rows, viewed from above at a slight angle, evenly lit. The rows run left to right across the whole image. Must tile seamlessly in both directions. No paths, no fences, no text, no border.
```

Para as outras duas, troque o trecho do tipo de plantação:
- `field_vegetables.png`: *"a vegetable garden of cabbages, beans and squash planted in straight horizontal rows of dark soil"*
- `field_tobacco.png`: *"a field of tall broad-leaved green tobacco plants in straight horizontal rows"*

### 15 a 18. Objetos (avulsos)
Proporção **1:1**. **Fundo magenta.**
Salve como `prop_cart.png`, `prop_haystack.png`, `prop_barrels.png` e `prop_logs.png`.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A single wooden two-wheeled hand cart loaded with a few barrels and sacks. Three-quarter view from the front, camera elevated about 35 degrees, like an isometric strategy game. Centered, filling about 80% of the frame. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no cast shadow, no people, no text, no border.
```

Para os outros, troque a primeira frase do objeto:
- `prop_haystack.png`: *"A single round haystack of golden straw"*
- `prop_barrels.png`: *"A small stack of wooden barrels and crates"*
- `prop_logs.png`: *"A neat pile of cut tree logs stacked on their sides"*

### 19 a 22. Animais (avulsos)
Proporção **1:1**. **Fundo magenta.**
Salve como `animal_horse.png`, `animal_cow.png`, `animal_pig.png` e `animal_sheep.png`.
Os cavalos vão aparecer no pasto quando a colônia tiver cavalos.

```
Generate an image: a semi-realistic painterly illustration for a historical strategy video game set in 17th-century colonial North America. Natural, warm, slightly desaturated colors, soft late-afternoon light coming from the upper left.
A single brown horse standing calmly, seen from the side and slightly above (camera elevated about 35 degrees), facing left. Centered, filling about 80% of the frame. Solid flat magenta background (#FF00FF) with nothing else on it. No ground, no grass, no cast shadow, no saddle, no rider, no text, no border.
```

Para os outros, troque o animal: *"A single brown and white dairy cow"*, *"A single pink farm pig"*, *"A single woolly white sheep"*. Mantenha "facing left".

### 23 a 25. Céus de outros terrenos
Mesmo formato do nº 1 (**3:1**, sem transparência). Use o prompt do nº 1, trocando a frase da paisagem:
- `sky_arid.png` (deserto, savana, cerrado): *"a warm pale sky, a distant horizon of dry golden plains, low mesas and scattered scrub trees in a light haze. The bottom quarter is an even area of dry yellowish grass and sand with no objects"*
- `sky_cold.png` (tundra, neve, floresta boreal): *"a cold pale blue-grey sky, a distant horizon of snowy hills and dark spruce forests in a light haze. The bottom quarter is an even area of short pale grass with patches of snow and no objects"*
- `sky_tropical.png` (pântano, floresta tropical): *"a humid bright sky with tall white clouds, a distant horizon of dense tropical jungle and palm trees in a light haze. The bottom quarter is an even area of lush green grass with no objects"*

### 26 a 28. Chão de outros terrenos
Mesmo formato do nº 2 (**1:1**, contínua). Use o prompt do nº 2, trocando o tipo de chão:
- `ground_grass_dry.png`: *"dry yellowish savanna grass with patches of bare earth"*
- `ground_sand.png`: *"pale desert sand with a few small stones and sparse dry tufts"*
- `ground_snow.png`: *"short pale tundra grass partly covered by thin patches of snow"*

---

## Depois de gerar

Me avise quais imagens você salvou na pasta. Eu:

1. converto para PNG, removo o fundo magenta se precisar e ajusto o tamanho;
2. confiro se as texturas emendam sem costura visível e corrijo se for preciso;
3. ligo cada uma no código, sem desligar o desenho atual para as que faltarem;
4. gero prévias em PNG para você aprovar antes de ir para o jogo.
