# CADERNO DE FÍSICA
## Do absoluto básico às áreas avançadas — conceitos, fórmulas, derivações e método de resolução

---

## APRESENTAÇÃO: COMO USAR ESTE CADERNO

Física é a ciência que constrói modelos matemáticos e conceituais para descrever e explicar fenômenos naturais: movimento, forças, energia, calor, som, luz, eletricidade, magnetismo, matéria, átomos, partículas, espaço e tempo. Ela combina observação, experimentação, raciocínio matemático, comparação com dados e revisão de modelos quando as evidências exigem.

Este caderno é um roteiro amplo de estudo, começando por medidas e matemática básica e avançando até mecânica clássica, termodinâmica, eletromagnetismo, óptica, relatividade, física quântica, nuclear, partículas, matéria condensada, astrofísica, cosmologia, física estatística, plasma, métodos numéricos e instrumentação. Inclui fórmulas essenciais, condições de uso, exemplos e um procedimento para resolver problemas.

**Limite importante:** a física é vasta demais para que um único documento contenha literalmente todos os detalhes, demonstrações, dados experimentais e métodos de todas as suas áreas. Uma fórmula isolada não representa um curso completo. Este volume organiza o território e desenvolve os fundamentos e métodos mais usados; especialidades avançadas exigem livros, artigos, experimentos, matemática adicional e exercícios próprios. Quando uma fórmula é aproximada ou depende de hipóteses, isso deve ser explicitado.

**Regra central:** nenhuma fórmula deve ser usada sem entender o que significam seus símbolos, quais unidades possuem, que hipóteses assume e em que situações deixa de funcionar.

# PARTE I — O QUE É FÍSICA E COMO A CIÊNCIA FUNCIONA

## 1. Física, modelos e leis

Uma **observação** registra algo que acontece. Uma **hipótese** é uma explicação provisória testável. Um **modelo** representa aspectos selecionados de um sistema, omitindo detalhes considerados irrelevantes para a pergunta. Uma **lei física** resume uma regularidade observada e quantificada; uma **teoria física** é uma estrutura explicativa apoiada por evidências, conceitos e modelos matemáticos. Na ciência, “teoria” não significa simples palpite.

Modelos têm domínio de validade. A mecânica newtoniana funciona muito bem para objetos macroscópicos em velocidades muito menores que a velocidade da luz e em regimes onde efeitos quânticos são desprezíveis. Para velocidades próximas à da luz, usa-se relatividade; para sistemas microscópicos, mecânica quântica. Em muitos casos, teorias novas reproduzem as previsões antigas no limite em que as antigas funcionavam.

A física não se limita a memorizar resultados: é preciso comparar previsões com medições, estimar erros, reproduzir resultados quando possível e aceitar que modelos podem ser refinados.

## 2. Grandezas físicas: escalares e vetores

Uma **grandeza física** é uma propriedade que pode ser expressa por um número e uma unidade, com uma definição de medição. Exemplos: massa, tempo, comprimento, energia e temperatura.

Grandezas **escalares** precisam de magnitude e unidade, mas não de direção: massa, tempo, energia, temperatura e carga elétrica. Grandezas **vetoriais** incluem magnitude, direção e sentido: deslocamento, velocidade, aceleração e força. O momento angular é um vetor em mecânica clássica.

Notação: o módulo do vetor \(\vec v\) é \(|\vec v|\) ou \(v\). Escrever apenas o número sem unidade pode deixar a resposta incompleta: 12 m/s é velocidade; 12 kg é massa.

## 3. Sistema Internacional de Unidades (SI)

As sete unidades de base do SI são:
- comprimento: metro (m);
- massa: quilograma (kg);
- tempo: segundo (s);
- corrente elétrica: ampere (A);
- temperatura termodinâmica: kelvin (K);
- quantidade de substância: mol (mol);
- intensidade luminosa: candela (cd).

Grandezas derivadas:
- velocidade: m/s;
- aceleração: m/s²;
- força: newton, \(1\,N=1\,kg\,m/s^2\);
- energia e trabalho: joule, \(1\,J=1\,N\,m\);
- potência: watt, \(1\,W=1\,J/s\);
- pressão: pascal, \(1\,Pa=1\,N/m^2\);
- carga elétrica: coulomb, \(1\,C=1\,A\,s\);
- tensão: volt, \(1\,V=1\,J/C\);
- resistência: ohm, \(1\,\Omega=1\,V/A\);
- frequência: hertz, \(1\,Hz=1\,s^{-1}\).

Prefixos comuns: kilo (k) = \(10^3\); mega (M) = \(10^6\); giga (G) = \(10^9\); centi (c) = \(10^{-2}\); milli (m) = \(10^{-3}\); micro (µ) = \(10^{-6}\); nano (n) = \(10^{-9}\); pico (p) = \(10^{-12}\). O símbolo da unidade diferencia maiúsculas e minúsculas: m é metro, M é mega como prefixo.

Conversões devem preservar a grandeza:
- \(1\,km=1000\,m\);
- \(1\,h=3600\,s\);
- \(72\,km/h=20\,m/s\), pois \(72/3{,}6=20\).
- \(1\,cm^2=10^{-4}\,m^2\), porque o fator de comprimento fica ao quadrado.
- \(1\,cm^3=10^{-6}\,m^3\), porque o fator de comprimento fica ao cubo.

A temperatura Celsius e a Kelvin se relacionam por \(T(K)=t(°C)+273{,}15\). Diferenças de 1 °C têm o mesmo tamanho que diferenças de 1 K, mas zero Celsius e zero Kelvin não são o mesmo ponto. Em fórmulas termodinâmicas como \(pV=nRT\), usa-se temperatura absoluta em kelvin.

O SI é mantido internacionalmente pelo BIPM. O material oficial foi revisado em 2026; valores exatos das constantes definidoras, como c, h, e e \(k_B\), fazem parte do sistema moderno. Consulte a Brochura do SI quando precisar de definições e convenções atualizadas.

## 4. Notação científica, arredondamento e algarismos significativos

Notação científica escreve um número como \(a\times10^n\), com \(1\leq|a|<10\). Exemplos: \(150000=1{,}5\times10^5\); \(0{,}00042=4{,}2\times10^{-4}\).

Algarismos significativos comunicam a precisão de uma medida. Em um cálculo experimental, não é correto apresentar muitas casas decimais sem que os dados as sustentem. Regras escolares úteis: em multiplicação/divisão, a resposta costuma respeitar o menor número de algarismos significativos dos dados; em soma/subtração, a casa decimal final deve ser compatível com a medida menos precisa. A prática formal depende do método de incerteza e das convenções do campo.

Arredondamento não é o mesmo que truncamento. Mantenha casas extras durante o cálculo e arredonde principalmente no final, para evitar acúmulo de erros.

## 5. Incerteza, erro e medição

Toda medição real possui incerteza. **Erro** é a diferença entre um resultado e um valor de referência; **incerteza** caracteriza a dispersão razoável de valores atribuíveis ao mensurando. Erros sistemáticos deslocam medições de forma consistente; erros aleatórios variam entre repetições.

Uma medição pode ser descrita como \(x=(12{,}4\pm0{,}2)\,cm\), se o procedimento justificar essa incerteza e o significado do intervalo estiver claro. Não se deve confundir incerteza de medição com erro humano ou com um intervalo garantido em qualquer circunstância.

Repetir medições, calibrar instrumentos, registrar condições, controlar variáveis e usar métodos apropriados aumenta a confiabilidade. Em laboratório, a apresentação de dados inclui unidades, incertezas, método e critérios de análise.

## 6. Análise dimensional

Dimensões fundamentais permitem verificar a consistência de equações. Denote comprimento por [L], massa por [M], tempo por [T]. Então:
- velocidade: \([v]=LT^{-1}\);
- aceleração: \([a]=LT^{-2}\);
- força: \([F]=MLT^{-2}\);
- energia: \([E]=ML^2T^{-2}\);
- potência: \([P]=ML^2T^{-3}\).

Exemplo: \(x=vt\) tem dimensão \((L/T)T=L\), portanto pode representar um comprimento. Uma expressão como \(x=v+t\) é dimensionalmente inválida porque soma grandezas com unidades diferentes.

A análise dimensional pode descartar fórmulas impossíveis, mas não prova que uma fórmula esteja correta: diferentes expressões podem ter a mesma dimensão. Ela também não determina constantes numéricas sem informação adicional.

## 7. Método geral para resolver um problema de física

Use esta sequência, mesmo quando o problema parecer simples:

1. **Leia e traduza a pergunta.** Identifique o sistema, o que é pedido e os dados fornecidos.
2. **Desenhe a situação.** Use esquema, eixo de coordenadas, diagrama de forças, circuito, raios de luz ou gráfico, conforme o caso.
3. **Liste dados e incógnitas.** Anote símbolos, valores, unidades e condições; marque incógnitas com letras.
4. **Escolha o modelo físico.** Por exemplo, movimento com aceleração constante, conservação de energia, lei de Ohm ou gás ideal. Declare hipóteses: atrito desprezível? sistema isolado? campo uniforme? velocidade baixa comparada a c?
5. **Selecione a lei antes da fórmula.** Pergunte qual princípio descreve o fenômeno. A fórmula é uma forma específica de escrever essa lei sob certas hipóteses.
6. **Isole a incógnita simbolicamente.** Faça álgebra antes de substituir números sempre que possível.
7. **Converta tudo para unidades compatíveis do SI**, salvo quando a própria fórmula pede outro sistema explícito.
8. **Substitua os valores com unidades** e calcule com cuidado os sinais, parênteses, potências e conversões.
9. **Verifique dimensão e plausibilidade.** O resultado tem a unidade correta? O tamanho faz sentido? O sinal corresponde à direção escolhida?
10. **Interprete a resposta.** Escreva o resultado com unidade e indique o sentido, intervalo ou limitação relevante.
11. **Revise as hipóteses.** Se a hipótese não for válida, escolha um modelo mais completo.

Nunca comece perguntando “qual fórmula tem esses números?”. Comece perguntando “que princípio físico governa esta situação?”.

## 8. Como transformar uma lei numa fórmula utilizável

Uma fórmula é uma relação entre grandezas. Para usá-la:
- identifique cada símbolo e sua unidade;
- descubra se as grandezas são escalares ou vetoriais;
- verifique se a fórmula descreve uma média, um instante, uma aproximação ou uma lei geral;
- verifique as condições;
- isole a incógnita;
- só então insira os números.

Exemplo: densidade uniforme é \(\rho=m/V\). Se a pergunta pede massa, multiplique ambos os lados por V: \(m=\rho V\). Se pede volume, divida por \(\rho\): \(V=m/\rho\). O procedimento algébrico deve preservar a igualdade e as unidades.

## 9. Vetores, componentes e trigonometria para física

Um vetor no plano pode ser escrito \(\vec A=(A_x,A_y)\). Se tem módulo A e ângulo \(\theta\) medido a partir do eixo x positivo:
\(A_x=A\cos\theta\), \(A_y=A\sin\theta\).
O módulo é \(A=\sqrt{A_x^2+A_y^2}\), e o ângulo pode ser encontrado com uma função trigonométrica que respeite o quadrante.

Exemplo: força de 10 N a 30° acima do eixo x:
\(F_x=10\cos30°\approx8{,}66\,N\);
\(F_y=10\sin30°=5\,N\).
A soma de vetores ocorre componente por componente; somar apenas os módulos só é correto quando a direção e o sentido permitem.

Produto escalar: \(\vec A\cdot\vec B=AB\cos\theta=A_xB_x+A_yB_y+A_zB_z\). É escalar e aparece no trabalho \(W=\vec F\cdot\vec d\) quando a força é constante.
Produto vetorial tem módulo \(AB\sin\theta\) e direção perpendicular aos vetores, definida pela regra da mão direita em três dimensões; aparece em torque e momento angular.

---

# PARTE II — MECÂNICA CLÁSSICA

## 10. Referencial, posição, distância e deslocamento

Um referencial define coordenadas e relógios para descrever movimento. Posição indica onde um objeto está no referencial. Distância percorrida é o comprimento total do caminho; deslocamento é a diferença vetorial entre posição final e inicial: \(\Delta\vec r=\vec r_f-\vec r_i\).

Exemplo: andar 3 m para leste e depois 3 m para oeste dá distância total 6 m, mas deslocamento zero.

Velocidade média vetorial: \(\vec v_{m}=\Delta\vec r/\Delta t\). Rapidez média: distância total dividida pelo intervalo de tempo. Elas não são sinônimos em trajetórias que mudam de direção.

Aceleração média: \(\vec a_m=\Delta\vec v/\Delta t\). Aceleração pode mudar o módulo da velocidade, a direção, ou ambos.

## 11. Movimento retilíneo uniforme (MRU)

No MRU, a velocidade é constante e a aceleração é zero. Em uma dimensão:
\(x=x_0+vt\), quando o tempo t é medido a partir do instante inicial adotado.
O deslocamento é \(\Delta x=vt\).

Como usar:
1. confira se a velocidade é constante;
2. defina a origem de posição e de tempo;
3. escolha o sinal positivo do eixo;
4. aplique \(x=x_0+vt\);
5. confira unidades.

Exemplo: um ciclista move-se a 5 m/s durante 12 s sem mudar a velocidade. Deslocamento: \(\Delta x=vt=(5\,m/s)(12\,s)=60\,m\). Se começa em \(x_0=10\,m\), termina em \(x=70\,m\).

## 12. Movimento retilíneo uniformemente variado (MRUV)

No MRUV, a aceleração é constante. As fórmulas, para aceleração a constante e tempo t contado do estado inicial, são:
- \(v=v_0+at\);
- \(x=x_0+v_0t+\frac12at^2\);
- \(v^2=v_0^2+2a(x-x_0)\);
- deslocamento médio \(\Delta x=\frac{v_0+v}{2}t\), porque a velocidade varia linearmente com o tempo.

Cada fórmula tem uma finalidade:
- se não há deslocamento e a incógnita é v ou t, a primeira pode ser conveniente;
- se não há velocidade final, a segunda relaciona posição e tempo;
- se o tempo não foi dado nem pedido, a terceira elimina t;
- a quarta vale quando a aceleração é constante.

Exemplo resolvido: um objeto parte do repouso, \(v_0=0\), com aceleração \(a=2\,m/s^2\) durante \(t=4\,s\). Velocidade final: \(v=0+2\times4=8\,m/s\). Deslocamento: \(\Delta x=0\times4+\frac12(2)(4^2)=16\,m\). A fórmula produz dimensão de comprimento porque \(m/s^2\times s^2=m\).

Queda livre perto da superfície terrestre é aproximadamente MRUV se a resistência do ar for desprezível e g puder ser considerada constante. Perto da Terra, \(g\approx9{,}8\,m/s^2\), mas o valor local varia ligeiramente. Defina o sentido positivo antes de escolher o sinal de g.

## 13. Gráficos de movimento

Gráfico posição-tempo: a inclinação fornece velocidade (ou velocidade instantânea no limite). Gráfico velocidade-tempo: a inclinação fornece aceleração; a área com sinal entre a curva e o eixo do tempo fornece deslocamento. Gráfico aceleração-tempo: a área fornece mudança de velocidade.

Não confunda a altura da curva com sua inclinação. Uma posição constante tem inclinação zero e velocidade zero. Velocidade negativa indica movimento no sentido negativo do eixo, não necessariamente “ficar mais lento”. A rapidez diminui quando velocidade e aceleração têm sinais opostos.

## 14. Movimento em duas e três dimensões

Cada componente pode ser descrita separadamente em coordenadas cartesianas, desde que as relações sejam aplicadas ao mesmo intervalo e referencial. No lançamento oblíquo ideal sem resistência do ar:
- \(x=x_0+v_{0x}t\);
- \(y=y_0+v_{0y}t-\frac12gt^2\), para eixo vertical positivo para cima;
- \(v_x=v_{0x}\);
- \(v_y=v_{0y}-gt\).
As componentes iniciais são \(v_{0x}=v_0\cos\theta\) e \(v_{0y}=v_0\sin\theta\).

Em terreno plano, com altura inicial igual à final, \(R=v_0^2\sin(2\theta)/g\) e o tempo de voo \(T=2v_0\sin\theta/g\). Essas expressões supõem g uniforme, ausência de arrasto e retorno à mesma altura. Se a altura final for diferente, use a equação vertical para encontrar o tempo.

Movimento circular: arco \(s=r\theta\) quando θ está em radianos; velocidade angular \(\omega=d\theta/dt\); rapidez tangencial \(v=r\omega\); aceleração centrípeta \(a_c=v^2/r=r\omega^2\). A aceleração centrípeta aponta para o centro; não é uma força extra, mas a aceleração resultante necessária para mudar a direção da velocidade.

## 15. Leis de Newton

**Primeira lei (inércia):** em referencial inercial, um corpo permanece em repouso ou em movimento retilíneo uniforme se a força resultante é zero.

**Segunda lei:** para massa constante, \(\sum\vec F=m\vec a\). Mais geralmente, a forma fundamental é \(\sum\vec F=d\vec p/dt\), em que \(\vec p=m\vec v\) é o momento linear.

**Terceira lei:** forças de interação aparecem em pares iguais e opostos aplicados a corpos diferentes. Não se cancelam no diagrama de forças de um único corpo porque agem em corpos diferentes.

Unidade de força: \(1\,N=1\,kg\,m/s^2\).

### Como resolver um problema de forças
1. Escolha o corpo que será estudado.
2. Desenhe o diagrama de corpo livre mostrando apenas forças externas atuantes nele.
3. Escolha eixos convenientes.
4. Decomponha as forças em componentes.
5. Escreva \(\sum F_x=ma_x\) e \(\sum F_y=ma_y\), ou a forma vetorial.
6. Acrescente relações geométricas, de atrito, corda ou contato.
7. Resolva as equações simultaneamente.
8. Confira sinais, unidades e se o resultado combina com o movimento.

Forças usuais: peso \(P=mg\) próximo à Terra; força normal perpendicular à superfície; tensão ao longo de uma corda ideal; força elástica \(F=-kx\) para mola ideal no regime linear; atrito estático com módulo que se ajusta até um máximo; atrito cinético aproximado por \(f_k=\mu_kN\) em modelo simples.

A normal não é sempre igual ao peso. Ela depende das outras forças e da aceleração perpendicular à superfície. Em plano inclinado sem outras forças perpendiculares, \(N=mg\cos\theta\).

Atrito estático: \(0\leq f_s\leq\mu_sN\); no limite de deslizamento \(f_{s,\max}=\mu_sN\). Não use automaticamente \(f_s=\mu_sN\) em qualquer caso estático.

## 16. Trabalho, energia e potência

Trabalho de força constante: \(W=\vec F\cdot\Delta\vec r=F d\cos\theta\). Trabalho positivo transfere energia para o sistema pelo mecanismo considerado; trabalho negativo transfere energia para fora; força perpendicular ao deslocamento faz trabalho zero nesse modelo.

Energia cinética: \(K=\frac12mv^2\).
Energia potencial gravitacional perto da Terra: \(U_g=mgh\), escolhendo uma referência para h.
Energia potencial elástica de mola ideal: \(U_e=\frac12kx^2\).
Teorema trabalho–energia: \(W_{\text{resultante}}=\Delta K\).

Se só forças conservativas atuam, a energia mecânica \(E=K+U\) conserva-se. Com atrito ou outras transferências, contabilize o trabalho não conservativo ou formas de energia que não estão no modelo mecânico. “Energia se conserva” não significa que a energia mecânica sempre se conserve.

Potência média \(P=W/\Delta t\); potência instantânea \(P=dW/dt=\vec F\cdot\vec v\) para força aplicada ao ponto de velocidade \(\vec v\). Unidade: watt.

Exemplo: levantar massa de 3 kg por 2 m lentamente, perto da Terra, exige aumento de energia potencial \(\Delta U=mgh\approx3\times9{,}8\times2=58{,}8\,J\). Se o levantamento durar 4 s e a transferência de energia for uniforme, potência média \(P\approx58{,}8/4=14{,}7\,W\). O valor considera aumento ideal da energia potencial, sem perdas.

## 17. Momento linear, impulso e colisões

Momento linear: \(\vec p=m\vec v\). Impulso: \(\vec J=\int\vec F\,dt=\Delta\vec p\); para força média, \(J=F_{\text{média}}\Delta t\).

Se a força externa resultante sobre um sistema é nula ou o impulso externo é desprezível, o momento linear total se conserva. Em colisão elástica, conservam-se momento e energia cinética total; em colisão inelástica, o momento conserva-se sob as condições acima, mas parte da energia cinética pode virar deformação, calor, som etc. Na colisão perfeitamente inelástica, os corpos permanecem juntos.

Procedimento:
1. defina o sistema antes e depois;
2. escolha o eixo e sinais;
3. escreva o momento total inicial igual ao final se o impulso externo for desprezível;
4. se a colisão for elástica, use também a conservação de energia cinética;
5. resolva para as velocidades e confira os sinais.

## 18. Centro de massa e sistemas de partículas

Em uma dimensão, centro de massa de massas discretas: \(x_{CM}=\sum m_ix_i/\sum m_i\). Em três dimensões, aplique às componentes. Para um corpo contínuo, substitua somas por integrais de massa.

O movimento do centro de massa obedece \(M\vec a_{CM}=\vec F_{\text{ext}}\), em sistema de massa total M. Forças internas se cancelam no balanço total de momento, quando corretamente consideradas como pares.

## 19. Rotação e torque

Deslocamento angular \(\theta\), velocidade angular \(\omega=d\theta/dt\) e aceleração angular \(\alpha=d\omega/dt\). Para aceleração angular constante:
- \(\omega=\omega_0+\alpha t\);
- \(\theta=\theta_0+\omega_0t+\frac12\alpha t^2\);
- \(\omega^2=\omega_0^2+2\alpha(\theta-\theta_0)\).

Torque: \(\vec\tau=\vec r\times\vec F\); módulo \(\tau=rF\sin\theta\). O torque depende do ponto de referência/escolha do eixo.

Momento de inércia \(I=\sum m_ir_i^2\), ou integral para distribuição contínua. Para rotação em eixo fixo e I constante, \(\sum\tau=I\alpha\). Energia cinética de rotação \(K_{rot}=\frac12I\omega^2\). Momento angular de corpo rígido em torno de eixo principal: \(L=I\omega\); em geral, a relação vetorial pode ser mais complexa.

Rolamento sem deslizamento: \(v_{CM}=R\omega\). Energia total é a soma das parcelas de translação e rotação, quando a decomposição vale.

## 20. Equilíbrio estático e elasticidade

Equilíbrio translacional: \(\sum\vec F=0\). Equilíbrio rotacional: \(\sum\vec\tau=0\). Ambas devem ser satisfeitas para equilíbrio rígido em situações usuais. Escolher um ponto de torque conveniente pode eliminar forças desconhecidas do cálculo.

Pressão mecânica média \(p=F_\perp/A\). Tensão e deformação descrevem forças internas por área e resposta relativa de um material. No regime elástico linear, lei de Hooke \(F=kx\) em módulo para uma mola, com força restauradora \(F=-kx\). A lei não vale indefinidamente: acima do regime elástico, o material pode deformar permanentemente ou romper.

## 21. Gravitação

Lei da gravitação universal de Newton:
\(F=Gm_1m_2/r^2\), para massas pontuais ou distribuições esfericamente simétricas vistas externamente, com r entre centros. A força é atrativa e aponta ao longo da linha que conecta os corpos.

Campo gravitacional de massa pontual: \(g=GM/r^2\). Peso aproximado \(P=mg\) perto da superfície, para campo local quase uniforme.

Energia potencial gravitacional newtoniana de duas massas, com zero no infinito: \(U=-GMm/r\). A forma \(mgh\) é aproximação para pequenas variações de altura perto da superfície terrestre, em que g é quase constante.

Velocidade orbital circular ideal: \(v=\sqrt{GM/r}\). Período orbital: \(T=2\pi\sqrt{r^3/(GM)}\). Velocidade de escape de massa esférica ideal: \(v_{esc}=\sqrt{2GM/R}\), ignorando atmosfera e outros corpos. Essas expressões supõem gravitação newtoniana e condições ideais.

As leis de Kepler descrevem órbitas planetárias: órbitas elípticas; áreas iguais em tempos iguais; \(T^2\propto a^3\) em torno do mesmo corpo central no regime kepleriano. Relatividade geral torna-se necessária para certos efeitos de alta precisão, campos fortes e cosmologia.

## 22. Fluidos: densidade, pressão, empuxo e fluxo

Densidade: \(\rho=m/V\). Pressão: \(p=F_\perp/A\). Pressão hidrostática em fluido de densidade constante: \(p=p_0+\rho gh\), com h profundidade medida para baixo a partir da referência de pressão \(p_0\).

Princípio de Pascal: mudança de pressão aplicada a um fluido confinado se transmite ao fluido em equilíbrio. Princípio de Arquimedes: empuxo é igual ao peso do fluido deslocado, \(F_E=\rho_f gV_{\text{deslocado}}\), para fluido com densidade apropriada.

Continuidade para fluido incompressível em escoamento estacionário: \(A_1v_1=A_2v_2\). Equação de Bernoulli em escoamento estacionário, incompressível, sem viscosidade e ao longo de uma linha de corrente: \(p+\frac12\rho v^2+\rho gh=\text{constante}\). Não aplicar Bernoulli sem verificar hipóteses, perdas e máquinas adicionando ou retirando energia.

A mecânica dos fluidos avançada inclui viscosidade, turbulência, camada limite, equações de Navier–Stokes, escoamentos compressíveis e estabilidade hidrodinâmica.

---

# PARTE III — OSCILAÇÕES, ONDAS E SOM

## 23. Movimento harmônico simples (MHS)

MHS ideal ocorre quando a força restauradora é proporcional ao deslocamento e oposta a ele: \(F=-kx\). Pela segunda lei, \(ma=-kx\), então \(a=-(k/m)x\). A solução é oscilatória com frequência angular \(\omega=\sqrt{k/m}\) para massa-mola ideal, e período \(T=2\pi\sqrt{m/k}\).

Uma forma da posição é \(x(t)=A\cos(\omega t+\phi)\), com amplitude A e fase inicial \(\phi\). Velocidade e aceleração são derivadas temporais:
\(v(t)=-A\omega\sin(\omega t+\phi)\);
\(a(t)=-A\omega^2\cos(\omega t+\phi)=-\omega^2x(t)\).

Pêndulo simples para pequenos ângulos: \(T\approx2\pi\sqrt{L/g}\). A fórmula é aproximada; para amplitudes grandes, o período depende da amplitude e é preciso tratar o movimento de modo mais completo.

## 24. Ondas: parâmetros e equações

Uma onda transporta energia e informação; em ondas mecânicas, propaga uma perturbação num meio. Comprimento de onda \(\lambda\), frequência f, período \(T=1/f\), velocidade de propagação v obedecem \(v=\lambda f\). Frequência é ciclos por segundo (Hz).

Onda harmônica viajante pode ser escrita \(y(x,t)=A\cos(kx-\omega t+\phi)\), com número de onda \(k=2\pi/\lambda\) e frequência angular \(\omega=2\pi f\). O sinal da fase diferencia direção de propagação conforme a convenção.

Princípio da superposição vale em sistemas lineares: perturbações se somam. Interferência construtiva ocorre quando amplitudes se reforçam; destrutiva quando se reduzem. Ondas estacionárias surgem de superposição de ondas contrárias em condições adequadas, com nós e ventres.

## 25. Ondas em cordas e som

Para uma corda ideal sob tensão \(\mathcal T\) e densidade linear de massa \(\mu\), a rapidez da onda é \(v=\sqrt{\mathcal T/\mu}\), sob hipóteses usuais de pequenas perturbações.

Som é onda mecânica; no ar, é principalmente longitudinal em condições usuais. Sua velocidade depende de propriedades do meio e temperatura. O som não se propaga no vácuo.

Intensidade média de onda: potência por área, \(I=P/A\) para propagação uniforme sobre a área A escolhida. Para fonte pontual isotrópica ideal, \(I=P/(4\pi r^2)\). Nível sonoro: \(\beta=10\log_{10}(I/I_0)\,dB\), com intensidade de referência especificada, geralmente \(I_0=10^{-12}\,W/m^2\) para som no ar. Decibel é uma escala logarítmica; aumento de 10 dB corresponde a um fator 10 na intensidade de referência.

Ressonância ocorre quando uma força periódica acopla eficientemente a um sistema próximo de uma frequência natural, com amplitude determinada também pelo amortecimento. O efeito Doppler é mudança observada de frequência causada por movimento relativo entre fonte e observador; a fórmula clássica depende do meio e da convenção de sinais. Para luz, em velocidades altas, use o Doppler relativístico.

---

# PARTE IV — TERMODINÂMICA E FÍSICA ESTATÍSTICA

## 26. Temperatura, calor e equilíbrio térmico

Temperatura é uma grandeza termodinâmica que, em equilíbrio, caracteriza o estado térmico e determina o sentido espontâneo de fluxo de calor entre corpos em contato. Calor é transferência de energia associada a diferença de temperatura, não uma substância armazenada dentro do corpo.

A Lei Zero da Termodinâmica formaliza o equilíbrio térmico: se A está em equilíbrio térmico com C e B também com C, então A e B estão em equilíbrio térmico entre si, sob as condições da teoria.

Calor sensível em modelo de calor específico constante: \(Q=mc\Delta T\). Aqui Q é energia transferida como calor, m massa, c calor específico e \(\Delta T\) a mudança de temperatura. O sinal depende da convenção adotada para o sistema. Mudança de fase, reação química e variação grande de temperatura podem exigir fórmulas mais completas.

Calor latente: \(Q=mL\) para mudança de fase a temperatura aproximadamente constante nas condições usuais. Durante uma mudança de fase de substância pura a pressão constante, o calor transferido pode alterar a fase sem alterar a temperatura, desde que a idealização se aplique.

## 27. Expansão térmica

Para pequenas variações de temperatura em material isotrópico e intervalo apropriado:
\(\Delta L=\alpha L_0\Delta T\).
Para volume:
\(\Delta V\approx\beta V_0\Delta T\), com relação entre \(\beta\) e \(\alpha\) aproximadamente \(\beta\approx3\alpha\) em sólidos isotrópicos simples. Materiais reais podem ter respostas mais complexas.

## 28. Gases ideais e teoria cinética

Equação do gás ideal: \(pV=nRT=Nk_BT\), com pressão p absoluta, volume V, número de mols n, constante molar R, temperatura absoluta T, número de partículas N e constante de Boltzmann \(k_B\). O modelo supõe partículas puntiformes com interações desprezíveis exceto nas colisões e funciona melhor em regimes de gás diluído longe da condensação.

Lei combinada para quantidade fixa de gás ideal: \(pV/T=\text{constante}\). Não use temperaturas em Celsius nessas relações.

Na teoria cinética clássica de gás ideal monoatômico, a energia cinética translacional média por partícula é \(\langle K\rangle=\frac32 k_BT\). A energia interna é \(U=\frac32 nRT\) para gás ideal monoatômico sem outros graus de liberdade ativos. Gases reais e moléculas com rotação, vibração ou efeitos quânticos exigem ajustes.

## 29. Primeira Lei da Termodinâmica

A primeira lei expressa conservação de energia para um sistema termodinâmico. Uma convenção comum é:
\(\Delta U=Q-W\),
em que Q é calor fornecido ao sistema e W é trabalho realizado pelo sistema. Outra convenção define trabalho feito sobre o sistema e escreve \(\Delta U=Q+W_{\text{sobre}}\). Ambas podem ser corretas; declare a convenção e mantenha-a consistentemente.

Trabalho de expansão quase-estática: \(W=\int_{V_i}^{V_f}p\,dV\). Em um diagrama p-V, o trabalho é a área orientada sob a curva conforme a convenção. Para pressão constante, \(W=p\Delta V\).

Processo isocórico: volume constante, trabalho de fronteira zero; processo isobárico: pressão constante; processo isotérmico: temperatura constante; processo adiabático: não há transferência de calor através da fronteira, Q=0 na idealização. Adiabático não quer dizer necessariamente temperatura constante.

## 30. Segunda Lei, entropia e máquinas térmicas

A segunda lei estabelece restrições sobre a direção dos processos e conversões de energia. Em uma formulação, a entropia total de um sistema isolado não diminui; processos reais irreversíveis geram entropia.

Para processo reversível, \(dS=\delta Q_{rev}/T\) em condições termodinâmicas apropriadas. A entropia é uma função de estado; calor é transferência de energia e não é função de estado.

Máquina térmica absorve calor \(Q_H\) de fonte quente, realiza trabalho W e rejeita \(Q_C\) para fonte fria: \(|W|=|Q_H|-|Q_C|\), usando magnitudes nessa relação. Eficiência \(\eta=W/Q_H=1-Q_C/Q_H\), sob a convenção em que Q_H é calor absorvido e Q_C magnitude de calor rejeitado. Nenhuma máquina cíclica real converte todo o calor absorvido em trabalho.

Eficiência de Carnot entre reservatórios a temperaturas absolutas \(T_H>T_C\): \(\eta_{\max}=1-T_C/T_H\). É limite ideal para máquina operando entre essas temperaturas, não eficiência típica garantida.

Refrigeradores e bombas de calor transportam calor usando trabalho. Coeficiente de desempenho (COP) de refrigerador: \(COP_R=Q_C/W\); de bomba de calor: \(COP_{HP}=Q_H/W\), com definições de calor e trabalho positivos segundo a operação.

## 31. Física estatística

Física estatística conecta comportamento microscópico de muitas partículas a propriedades macroscópicas como temperatura, pressão, calor específico, magnetismo e transições de fase. Microestado descreve detalhe microscópico; macroestado descreve variáveis macroscópicas.

Distribuições de Maxwell–Boltzmann, Fermi–Dirac e Bose–Einstein surgem em regimes e tipos de partículas diferentes. Férmions obedecem ao princípio de exclusão de Pauli; bósons podem ocupar o mesmo estado quântico. Energia livre, funções de partição, flutuações, limites termodinâmicos e teoria de transições de fase são ferramentas centrais.

Entropia estatística de Boltzmann em uma forma fundamental é \(S=k_B\ln\Omega\) para Ω microestados acessíveis equiprováveis, com hipóteses da expressão. Na formulação de Gibbs, usa-se uma distribuição de probabilidades e uma expressão mais geral. Não reduza entropia simplesmente a “desordem”; essa é uma analogia limitada.

---

# PARTE V — ELETRICIDADE E MAGNETISMO

## 32. Carga elétrica e lei de Coulomb

Cargas elétricas podem ser positivas ou negativas; cargas de mesmo sinal se repelem e de sinais opostos se atraem. Em sistemas isolados, a carga total conserva-se. A carga elementar tem módulo e; próton possui +e, elétron −e.

Lei de Coulomb para cargas puntiformes estacionárias no vácuo:
\(F=k_e|q_1q_2|/r^2\),
com direção ao longo da linha entre as cargas; o sentido depende dos sinais. Em forma vetorial, o sinal e os vetores posição determinam atração ou repulsão. Em meios materiais, a interação efetiva pode ser diferente.

## 33. Campo elétrico

Campo elétrico é força por unidade de carga de teste positiva:
\(\vec E=\vec F/q\), com q tomada como carga de teste e no regime em que o campo de fontes não é alterado significativamente por ela. Para carga puntiforme \(Q\), módulo \(E=k_e|Q|/r^2\), direção radial para fora se Q>0 e para dentro se Q<0.

O campo de muitas cargas se soma vetorialmente: princípio da superposição. Linhas de campo são representação visual, não objetos físicos. Sua densidade sugere magnitude e as linhas não se cruzam em campo vetorial bem definido.

## 34. Potencial elétrico e energia

Potencial elétrico \(V\) é energia potencial elétrica por unidade de carga: \(V=U/q\). Diferença de potencial \(\Delta V\) é energia transferida por carga em uma convenção definida. Em campo eletrostático, \(\Delta U=q\Delta V\) para carga q em posições especificadas.

Para carga puntiforme \(Q\), com potencial zero no infinito, \(V=k_eQ/r\). Energia potencial de duas cargas puntiformes é \(U=k_eq_1q_2/r\), com sinal resultante do produto das cargas.

Campo e potencial se relacionam por \(\vec E=-\nabla V\); em uma dimensão \(E_x=-dV/dx\). O campo aponta para a direção em que o potencial diminui para uma carga de teste positiva.

## 35. Lei de Gauss e capacitores

Fluxo elétrico \(\Phi_E=\int\vec E\cdot d\vec A\). Lei de Gauss: \(\oint\vec E\cdot d\vec A=Q_{\text{interna}}/\varepsilon_0\), para superfície fechada. É sempre válida no eletromagnetismo clássico, mas particularmente útil para calcular campos quando há simetria esférica, cilíndrica ou plana.

Capacitância: \(C=Q/\Delta V\). Capacitor de placas paralelas ideal, afastadas pouco em relação às dimensões laterais e desprezando bordas, tem \(C\approx\varepsilon_0A/d\) no vácuo. Com dielétrico homogêneo ideal, a capacitância depende da permissividade. Energia armazenada: \(U=\frac12CV^2=\frac12Q\Delta V=Q^2/(2C)\).

Capacitores em paralelo: a capacitância equivalente soma. Em série: \(1/C_{eq}=\sum_i1/C_i\), para a idealização de elementos independentes conectados em série.

## 36. Corrente, resistência e lei de Ohm

Corrente elétrica \(I=dQ/dt\) é taxa de fluxo de carga. Em regime constante, \(I=\Delta Q/\Delta t\). Resistência \(R=\Delta V/I\) em componente ôhmico e condições definidas. Lei de Ohm \(V=IR\) descreve comportamento ôhmico; não é uma lei universal de todos os componentes.

Resistividade do material \(\rho_e\) e geometria de fio uniforme:
\(R=\rho_e L/A\), quando temperatura e propriedades podem ser tratadas como uniformes.
Potência elétrica: \(P=VI=I^2R=V^2/R\) para elemento resistivo ôhmico e convenção passiva. Energia elétrica transferida em intervalo: \(E=P\Delta t\) se potência constante.

Não confunda densidade de massa \(\rho\) com resistividade elétrica \(\rho_e\); o mesmo símbolo pode ser usado em contextos diferentes, por isso defina as variáveis.

## 37. Circuitos: resistores e leis de Kirchhoff

Resistores em série: mesma corrente; \(R_{eq}=\sum R_i\). Resistores em paralelo: mesma tensão; \(1/R_{eq}=\sum1/R_i\).

Lei dos nós de Kirchhoff: soma algébrica das correntes que entram e saem de um nó é zero, equivalente à conservação de carga em regime estacionário. Lei das malhas: soma algébrica das variações de potencial ao redor de uma malha fechada é zero, equivalente à conservação de energia no modelo de circuito concentrado.

Método para circuito:
1. identifique nós, ramos e malhas;
2. marque sentidos arbitrários para correntes;
3. use relações dos componentes;
4. aplique lei dos nós e/ou das malhas;
5. resolva sistema algébrico;
6. interprete corrente negativa como sentido real oposto ao escolhido;
7. verifique conservação de carga e energia no circuito.

Fontes reais podem ter resistência interna; capacitores e indutores criam transitórios, portanto o circuito depende do tempo.

## 38. Campo magnético e força magnética

A força magnética sobre carga q com velocidade v em campo B:
\(\vec F_B=q\vec v\times\vec B\), módulo \(|q|vB\sin\theta\). Para carga positiva, use regra da mão direita; para carga negativa, inverta o sentido. A força magnética é perpendicular à velocidade instantânea e ao campo, e não realiza trabalho sobre uma partícula pontual em campo magnético ideal, pois \(\vec F_B\cdot\vec v=0\).

Força sobre fio retilíneo em campo uniforme: \(\vec F=I\vec L\times\vec B\) nas condições de fio e corrente orientados consideradas.

Movimento circular de carga em campo magnético uniforme, se v é perpendicular a B e outras forças são desprezíveis: raio \(r=mv/(|q|B)\). A trajetória geral pode ser helicoidal se houver componente de velocidade paralela ao campo.

## 39. Fontes de campo magnético

A lei de Biot–Savart descreve contribuição de elementos de corrente a B; a lei de Ampère relaciona circulação magnética à corrente encerrada em magnetostática, com unidades SI compatíveis. Para fio retilíneo longo ideal, \(B=\mu_0I/(2\pi r)\) no vácuo, fora do fio e sob simetria cilíndrica. Dentro de condutores reais, o perfil depende da distribuição de corrente.

Solenoide longo ideal: \(B\approx\mu_0 nI\) no interior, onde n é o número de espiras por unidade de comprimento, com aproximações de comprimento grande e efeitos de borda desprezados.

Materiais magnéticos respondem ao campo de formas diferentes: diamagnetismo, paramagnetismo, ferromagnetismo e outros estados são explicados por estrutura eletrônica e fenômenos coletivos. A imagem de pequenos ímãs rígidos pode ser insuficiente em física de materiais.

## 40. Indução eletromagnética e indutores

Lei de Faraday–Lenz:
\(\mathcal E=-d\Phi_B/dt\), para uma espira, com fluxo magnético \(\Phi_B=\int\vec B\cdot d\vec A\). Para N espiras idênticas em uma bobina ideal, a força eletromotriz induzida é \(\mathcal E=-N\,d\Phi_B/dt\). O sinal de menos expressa a lei de Lenz: a resposta induzida se opõe à mudança de fluxo que a provoca, segundo a convenção de orientação escolhida.

Indutância L relaciona fluxo concatenado e corrente no regime linear: \(N\Phi_B=LI\) sob uma convenção simples. Tensão em indutor ideal: \(V_L=L\,dI/dt\) na convenção passiva. Energia magnética armazenada: \(U_L=\frac12LI^2\).

Transformadores ideais: \(V_s/V_p=N_s/N_p\); corrente tem relação inversa aproximada se não houver perdas: \(I_s/I_p=N_p/N_s\). A indução exige mudança temporal de fluxo; transformadores convencionais não funcionam com corrente contínua constante em regime estacionário.

## 41. Corrente alternada e circuitos RLC

Corrente alternada pode ser descrita por \(v(t)=V_0\cos(\omega t+\phi)\). Para senoide ideal, valores eficazes são \(V_{rms}=V_0/\sqrt2\) e \(I_{rms}=I_0/\sqrt2\); isso decorre da média quadrática no ciclo.

Reatância de indutor ideal \(X_L=\omega L\); de capacitor ideal \(X_C=1/(\omega C)\). Em circuito RLC série ideal, o módulo da impedância é \(Z=\sqrt{R^2+(X_L-X_C)^2}\), com corrente eficaz \(I_{rms}=V_{rms}/Z\) para fonte senoidal em regime permanente. Ressonância ocorre perto de \(\omega_0=1/\sqrt{LC}\) no modelo ideal; amortecimento e resistência afetam a resposta.

## 42. Equações de Maxwell e ondas eletromagnéticas

As equações de Maxwell unificam eletricidade e magnetismo. Em forma integral no vácuo, usando notação SI:
- Lei de Gauss elétrica: \(\oint\vec E\cdot d\vec A=Q/\varepsilon_0\).
- Lei de Gauss magnética: \(\oint\vec B\cdot d\vec A=0\).
- Lei de Faraday: \(\oint\vec E\cdot d\vec l=-d\Phi_B/dt\).
- Lei de Ampère–Maxwell: \(\oint\vec B\cdot d\vec l=\mu_0I+\mu_0\varepsilon_0\,d\Phi_E/dt\) para superfície e convenções orientadas compatíveis.

Combinadas no vácuo, dão propagação de ondas eletromagnéticas à velocidade \(c=1/\sqrt{\mu_0\varepsilon_0}\) na formulação clássica consistente com SI. A luz é radiação eletromagnética. Campos elétricos e magnéticos variáveis e acoplados transportam energia e momento.

O espectro eletromagnético inclui ondas de rádio, micro-ondas, infravermelho, luz visível, ultravioleta, raios X e raios gama. Todas são radiação eletromagnética; diferem em frequência, comprimento de onda e energia por fóton, não em uma natureza fundamental separada.

---

# PARTE VI — ÓPTICA

## 43. Reflexão, refração e espelhos

Lei da reflexão: ângulo de incidência igual ao ângulo de reflexão, medidos em relação à normal à superfície. Refração muda a direção da luz quando a velocidade de fase muda entre meios.

Lei de Snell: \(n_1\sin\theta_1=n_2\sin\theta_2\), em meios isotrópicos homogêneos e na descrição geométrica usual. Índice de refração \(n=c/v\) para velocidade de fase v no meio e c no vácuo, com ressalvas em meios dispersivos.

Espelho esférico para raios paraxiais: \(1/f=1/d_o+1/d_i\), com convenção de sinais definida. Ampliação linear \(m=h_i/h_o=-d_i/d_o\) nessa convenção usual para espelhos. Os sinais ajudam a diferenciar imagem real/virtual e orientação; nunca misture convenções diferentes na mesma questão.

## 44. Lentes e instrumentos ópticos

Para lente delgada no ar e aproximação paraxial: \(1/f=1/d_o+1/d_i\), com convenção de sinais apropriada. A potência óptica em dioptrias é \(D=1/f\), com f em metros. A equação depende da aproximação de lente fina e do índice/meios assumidos.

A ampliação é \(m=h_i/h_o=-d_i/d_o\) em uma convenção comum. Lentes convergentes e divergentes formam imagens diferentes conforme a posição do objeto. Desenhar raios principais ajuda a verificar sinais e natureza da imagem.

Microscópios, telescópios, câmeras e olhos combinam lentes, aberturas e detectores; resolução depende do comprimento de onda, abertura numérica, difração e qualidade óptica, não somente da ampliação.

## 45. Interferência, difração e polarização

Interferência é resultado da superposição de amplitudes de onda. Para duas fontes coerentes, diferença de caminho \(\Delta r\) determina a fase relativa. Para máximos de interferência construtiva em condições ideais, \(\Delta r=m\lambda\); para mínimos em duas ondas de amplitudes iguais, \(\Delta r=(m+\frac12)\lambda\), com m inteiro.

Dupla fenda ideal de separação d e ângulo pequeno: máximos aproximadamente em \(d\sin\theta=m\lambda\); para pequenas distâncias angulares em tela distante a distância L, separação entre franjas \(\Delta y\approx\lambda L/d\). As condições geométricas devem ser checadas.

Difração é espalhamento/espalhamento geométrico de onda por aberturas e obstáculos. Abertura finita limita a resolução de instrumentos. Polarização descreve a orientação das oscilações de campo transversal; a lei de Malus \(I=I_0\cos^2\theta\) aplica-se à intensidade de luz linearmente polarizada atravessando um analisador ideal em ângulo θ.

Óptica física, óptica não linear, lasers, fibra óptica, guias de onda e óptica quântica são áreas posteriores.

---

# PARTE VII — RELATIVIDADE E FÍSICA MODERNA

## 46. Relatividade especial

A relatividade especial aplica-se a referenciais inerciais e é baseada, em sua formulação usual, na equivalência das leis físicas nesses referenciais e na constância da velocidade da luz no vácuo para observadores inerciais. Ela modifica as relações clássicas entre espaço, tempo, energia e momento quando as velocidades são comparáveis a c.

Fator de Lorentz: \(\gamma=1/\sqrt{1-v^2/c^2}\), com \(v<c\).

Dilatação temporal: \(\Delta t=\gamma\Delta\tau\), onde \(\Delta\tau\) é tempo próprio medido no referencial em que os eventos ocorrem no mesmo ponto. Contração de comprimento ao longo do movimento: \(L=L_0/\gamma\), onde \(L_0\) é comprimento próprio. Simultaneidade depende do referencial.

Energia de repouso: \(E_0=mc^2\). Energia total relativística: \(E=\gamma mc^2\); momento \(\vec p=\gamma m\vec v\). Relação energia-momento: \(E^2=(pc)^2+(mc^2)^2\). Para partícula sem massa, \(E=pc\).

Não aplique a fórmula clássica de energia cinética \(\frac12mv^2\) a velocidades próximas de c; ela é o limite de baixa velocidade da expressão relativística.

## 47. Relatividade geral e gravitação

Relatividade geral descreve gravidade como geometria dinâmica do espaço-tempo, relacionada à distribuição de energia e momento. A equação de campo de Einstein, em uma forma comum com constante cosmológica, é
\(G_{\mu\nu}+\Lambda g_{\mu\nu}=(8\pi G/c^4)T_{\mu\nu}\).
Aqui, os tensores representam geometria e conteúdo de energia-momento; esta fórmula é simbólica e não pode ser resolvida sem especificar a geometria, o conteúdo físico e condições de contorno.

Consequências e testes incluem desvio gravitacional da luz, precessão orbital, dilatação gravitacional do tempo, ondas gravitacionais e modelos cosmológicos. A gravitação newtoniana emerge como aproximação em campo fraco e velocidades baixas.

Buracos negros são regiões cujas propriedades dependem da solução relativística; horizonte de eventos delimita uma região de onde sinais clássicos não escapam para o infinito. A descrição completa envolve geometria, matéria, observação e, em questões extremas, possíveis correções quânticas ainda investigadas.

## 48. Quantização, fótons e matéria

Energia de fóton: \(E=hf=\hbar\omega\), com constante de Planck h e \(\hbar=h/(2\pi)\). Relação entre frequência e comprimento de onda no vácuo: \(c=f\lambda\). Logo \(E=hc/\lambda\).

Efeito fotoelétrico: um fóton pode transferir energia a um elétron; em modelo simples de metal, energia cinética máxima \(K_{max}=hf-\phi\), para frequência suficiente, onde \(\phi\) é função trabalho. A intensidade afeta taxa de fótons e corrente nas condições apropriadas, enquanto a frequência determina energia por fóton. Uma descrição clássica de onda contínua não explica por si só todas as observações quantizadas.

De Broglie associou comprimento de onda \(\lambda=h/p\) à matéria em situações apropriadas. A natureza onda-partícula não significa que objetos clássicos sejam literalmente pequenas bolas e ondas clássicas ao mesmo tempo; é necessário usar previsões quânticas e probabilidades.

## 49. Fundamentos da mecânica quântica

Um estado quântico é representado matematicamente por um vetor de estado ou função de onda, conforme a representação. A função de onda \(\psi\) contém amplitudes complexas; em uma representação de posição, \(|\psi(\vec r,t)|^2\) fornece densidade de probabilidade de posição segundo a regra de Born, com normalização \(\int|\psi|^2d^3r=1\) para estado normalizado.

Equação de Schrödinger dependente do tempo:
\(i\hbar\,\partial\Psi/\partial t=\hat H\Psi\).
Para partícula não relativística de massa m em potencial V:
\(\hat H=-\frac{\hbar^2}{2m}\nabla^2+V\).
A equação independente do tempo \(\hat H\psi=E\psi\) é usada para estados estacionários quando o Hamiltoniano não depende explicitamente do tempo, sob condições apropriadas.

Operadores representam observáveis na formulação padrão; possíveis resultados estão ligados a autovalores dos operadores apropriados. Valores esperados são médias estatísticas previstas em preparações repetidas, e não necessariamente o resultado de uma única medição.

Princípio de incerteza de posição e momento: \(\Delta x\,\Delta p_x\geq\hbar/2\), para as definições padrão de desvio-padrão em estados quânticos. Não é simplesmente uma falha instrumental: expressa uma propriedade matemática dos estados e operadores não comutativos. Também não é correto dizer que medir sempre “destrói” qualquer sistema; o efeito depende da observável, interação e processo de medida.

Superposição significa que uma combinação linear de estados possíveis pode constituir outro estado permitido. Emaranhamento é correlação quântica que não pode ser descrita como simples mistura de estados independentes em determinadas situações. Decoerência explica a supressão prática de interferências entre alternativas devido à interação com o ambiente, mas não resolve sozinha todas as questões interpretativas sobre a medição.

## 50. Átomos, núcleos e química quântica

Átomos consistem em núcleo e elétrons; núcleos contêm prótons e nêutrons (exceto o hidrogênio-1, cujo núcleo tem um próton). O número atômico Z é o número de prótons; número de massa A=Z+N, com N nêutrons. Isótopos têm mesmo Z e diferentes números de nêutrons.

Orbitais são estados quânticos descritos por funções de onda, não órbitas clássicas percorridas como planetas. Os números quânticos descrevem estados eletrônicos, incluindo energia, momento angular orbital e spin, com restrições quantizadas. O princípio de Pauli impede que dois férmions idênticos ocupem o mesmo estado quântico completo; regras de preenchimento são aproximações de organização e possuem exceções.

Espectros atômicos surgem quando o sistema passa entre estados com energias diferentes, absorvendo ou emitindo fótons de energia \(\Delta E=hf\). O modelo de Bohr fornece níveis úteis para sistemas hidrogenoides em aproximações específicas, mas átomos multieletrônicos exigem tratamentos mais complexos, correlações eletrônicas e métodos aproximados.

## 51. Física nuclear

A física nuclear estuda estrutura, estabilidade, decaimento e reações de núcleos. A energia de ligação está relacionada à diferença entre a soma das massas dos constituintes separados e a massa do núcleo ligado: \(E_b=\Delta m\,c^2\) em repouso, usando massas compatíveis e a convenção correta. A força nuclear forte residual mantém nucleons ligados a curtas distâncias; a repulsão elétrica entre prótons também afeta a estabilidade.

Decaimentos radioativos incluem alfa, beta e gama, com conservação de energia, carga, momento e números quânticos apropriados. Lei de decaimento exponencial: \(N(t)=N_0e^{-\lambda t}\) para amostra ideal com taxa constante de decaimento; meia-vida \(t_{1/2}=\ln2/\lambda\). Essa lei estatística descreve uma população, não determina o instante exato em que um único núcleo decairá.

Fissão divide núcleos pesados; fusão combina núcleos leves em condições adequadas. Ambas podem liberar energia dependendo dos núcleos e produtos porque a energia de ligação por nucleon varia com número de massa. Física nuclear também inclui reações, espectroscopia nuclear, estrutura de camadas e aplicações médicas; cada aplicação depende de controle, proteção e regulamentação.

## 52. Física de partículas

O Modelo Padrão organiza partículas elementares conhecidas e interações eletromagnética, fraca e forte, com bósons mediadores apropriados; gravitação não faz parte dele como interação quantizada completa. Quarks formam hádrons como prótons e nêutrons; léptons incluem elétron, múon, tau e neutrinos. O fóton medeia a interação eletromagnética; glúons a forte; bósons W e Z a fraca; o bóson de Higgs está associado ao mecanismo de Higgs.

Conservações e simetrias orientam quais processos são possíveis. A classificação de partículas envolve spin, carga, sabor e outros números quânticos. Experimentos de aceleradores e detectores comparam produtos de colisões a previsões estatísticas de modelos.

O Modelo Padrão é extremamente bem-sucedido em seu domínio, mas não é uma teoria completa de tudo: não incorpora a gravidade quântica e não explica sozinho todos os problemas cosmológicos, como a natureza da matéria escura.

## 53. Matéria condensada e estado sólido

Matéria condensada estuda sistemas com muitas partículas, incluindo sólidos, líquidos, magnetismo, supercondutividade, semicondutores e estados quânticos coletivos. A estrutura periódica de cristais leva a bandas de energia. A diferença entre bandas ocupadas e vazias ajuda a distinguir condutores, semicondutores e isolantes em modelos básicos.

Fônons descrevem quantizações de modos vibracionais de rede; quasipartículas são excitações coletivas que se comportam efetivamente como partículas sob certas condições. Supercondutividade envolve resistência elétrica nula e outras propriedades específicas abaixo de uma temperatura crítica em certos materiais; não é explicada pelo modelo mais simples de condução metálica.

A física de materiais conecta mecânica quântica, estatística, eletromagnetismo, química e técnicas experimentais.

## 54. Cosmologia e astrofísica

Astrofísica aplica física a estrelas, planetas, galáxias, meios interestelares e objetos compactos. Usa gravitação, termodinâmica, física nuclear, radiação, relatividade e modelos de matéria.

Estrelas equilibram gravidade com pressão e transporte de energia em modelos adequados. A fusão nuclear é importante durante fases específicas da evolução estelar; estrelas evoluem conforme massa inicial, composição e processos internos. Anãs brancas, estrelas de nêutrons e buracos negros surgem em trajetórias diferentes e não de qualquer estrela indistintamente.

Cosmologia estuda a evolução em grande escala do universo. O modelo cosmológico padrão descreve expansão, radiação cósmica de fundo, formação de estruturas e componentes como matéria escura e energia escura, cujas naturezas não estão completamente compreendidas. O desvio para o vermelho cosmológico e a expansão não devem ser reduzidos sem ressalvas ao Doppler clássico de uma fonte se movendo através de espaço estático.

## 55. Plasma e física de altas energias

Plasma é um meio com partículas carregadas livres em quantidade suficiente para exibir comportamento coletivo eletromagnético. É comum em estrelas, descargas elétricas e muitos ambientes espaciais. A descrição pode envolver movimento cinético de partículas, campos eletromagnéticos, colisões, ondas e instabilidades.

Magnetohidrodinâmica combina descrição de fluidos condutores com eletromagnetismo sob determinadas escalas e aproximações. Física de plasma também trata de fusão, vento solar, magnetosferas e descargas. Um plasma não é simplesmente “gás muito quente” em qualquer situação; o estado de ionização e o comportamento coletivo importam.

---

# PARTE VIII — FÍSICA AVANÇADA, MODELOS E FRONTEIRAS

## 56. Mecânica analítica: Lagrangiano e Hamiltoniano

Mecânica lagrangiana descreve sistemas por \(L=T-U\) em sistemas conservativos simples, sendo T energia cinética e U potencial. As equações de Euler–Lagrange são:
\(\frac{d}{dt}(\partial L/\partial\dot q_i)-\partial L/\partial q_i=0\).
Elas permitem usar coordenadas generalizadas \(q_i\), restrições e simetrias de maneira sistemática.

A mecânica hamiltoniana usa coordenadas \(q_i\) e momentos conjugados \(p_i\), com Hamiltoniano H e equações \( \dot q_i=\partial H/\partial p_i\), \( \dot p_i=-\partial H/\partial q_i\) em coordenadas canônicas. Em muitos sistemas, H corresponde à energia total, mas isso depende das condições e da formulação.

Teorema de Noether relaciona simetrias contínuas da ação a leis de conservação: invariância temporal se associa à energia; invariância espacial, ao momento linear; invariância rotacional, ao momento angular, sob hipóteses adequadas. É uma ponte conceitual fundamental entre simetria e física.

## 57. Sistemas não lineares e caos

Um sistema não linear não obedece necessariamente à superposição. Sistemas determinísticos podem apresentar caos: sensibilidade forte às condições iniciais, de modo que pequenas diferenças iniciais levam a trajetórias muito diferentes. Caos não significa ausência de leis ou aleatoriedade fundamental; o comportamento pode ser determinístico, porém difícil de prever a longo prazo com dados finitos.

Espaço de fases representa o estado do sistema por coordenadas como posições e momentos. Atratores, bifurcações, estabilidade e expoentes de Lyapunov são conceitos usados para estudar sua dinâmica.

## 58. Métodos computacionais e numéricos em física

Muitos problemas não admitem solução analítica simples. Métodos numéricos aproximam integrais, soluções diferenciais, trajetórias, campos e estados quânticos.

Fluxo de trabalho:
1. formular o problema físico e as equações;
2. declarar condições iniciais, de contorno e parâmetros;
3. escolher uma discretização/método adequado;
4. verificar unidades e escalas;
5. analisar convergência ao refinar a malha ou reduzir o passo;
6. comparar com soluções analíticas, casos-limite ou dados independentes;
7. quantificar erro e incerteza;
8. validar e comunicar limitações.

Métodos incluem Euler e Runge–Kutta para equações diferenciais, diferenças/elementos finitos para problemas de campo, Monte Carlo para problemas estatísticos e otimização para ajustar parâmetros. Um gráfico bonito ou uma simulação que roda não prova que o modelo é correto.

## 59. Física experimental, instrumentação e metrologia

Experimentos exigem definição operacional da grandeza, instrumentos calibrados, controle de variáveis, registro de dados, estimativa de incerteza e documentação do método. Calibração relaciona as indicações do instrumento a padrões ou referências; resolução do aparelho não é a mesma coisa que incerteza total.

Um resultado deve ser confrontado com modelos levando em conta tanto a incerteza experimental quanto as previsões teóricas. Gráficos precisam de eixos rotulados, unidades, escalas adequadas e, quando necessário, barras de erro. Correlação entre duas variáveis não prova causalidade; desenho experimental e análise são indispensáveis.

## 60. Como derivar fórmulas sem apenas decorá-las

Uma derivação começa com uma lei e as hipóteses correspondentes.

Exemplo A — movimento com aceleração constante:
1. Definição de aceleração constante: \(a=dv/dt\).
2. Integrando em relação ao tempo: \(v=v_0+at\).
3. Como \(v=dx/dt\), integre novamente: \(x=x_0+v_0t+\frac12at^2\).
4. Eliminando t entre relações apropriadas obtém-se \(v^2=v_0^2+2a(x-x_0)\).
A derivação explica por que a fórmula é válida apenas quando a aceleração é constante.

Exemplo B — energia cinética pelo trabalho:
1. Para força resultante \(F=ma\) em uma dimensão, \(W=\int F\,dx\).
2. Use \(a=dv/dt=(dv/dx)(dx/dt)=v\,dv/dx\), quando a descrição é regular.
3. Então \(W=\int m v\,dv =\frac12m(v_f^2-v_i^2)\).
4. Portanto, trabalho resultante igual à variação de energia cinética.
Esse resultado não exige que a força seja constante, mas a massa e a mecânica newtoniana devem obedecer às hipóteses consideradas.

Exemplo C — lei de Ohm:
A relação \(V=IR\) é uma descrição empírica de componentes ôhmicos sob condições em que R pode ser tratado como constante. Não é deduzida apenas de conservação de energia, nem é válida para todo dispositivo. Ao calcular, use \(I=V/R\) ou \(R=V/I\), mas confirme que o componente é aproximadamente ôhmico na faixa considerada.

A distinção entre lei fundamental, definição, aproximação e relação empírica é essencial. Uma equação pode ser exata por definição, exata dentro de um modelo ideal, ou aproximada para uma faixa de parâmetros.

## 61. Limites de modelos e escolha da teoria

Escolha o modelo pela escala:
- velocidades pequenas comparadas a c: mecânica newtoniana costuma servir;
- velocidades próximas a c: relatividade especial;
- gravidade intensa ou precisão cosmológica: relatividade geral;
- átomos, fótons e escalas microscópicas: teoria quântica;
- muitas partículas: métodos estatísticos e de muitos corpos;
- sistemas reais com fricção, turbulência, materiais e geometrias complexas: pode ser necessário usar modelos numéricos e dados empíricos.

As teorias não são sempre alternativas excludentes. Uma teoria mais completa deve recuperar a teoria antiga no seu domínio de validade quando a antiga é uma boa aproximação.

## 62. Constantes físicas e quando consultar valores

Algumas constantes e referências usuais:
- velocidade da luz no vácuo: \(c=299\,792\,458\,m/s\), valor exato na definição SI;
- constante de Planck: \(h=6{,}62607015\times10^{-34}\,J\,s\), exata no SI;
- carga elementar: \(e=1{,}602176634\times10^{-19}\,C\), exata no SI;
- constante de Boltzmann: \(k_B=1{,}380649\times10^{-23}\,J/K\), exata no SI;
- aceleração gravitacional perto da superfície terrestre: aproximadamente \(9{,}8\,m/s^2\), não universalmente exata;
- constante gravitacional \(G\): usar valor recomendado e incerteza em fontes metrológicas atualizadas; não tratá-la como exata.

Para trabalhos de alta precisão, consulte CODATA/NIST e defina a versão dos dados. Para problemas escolares, use os valores dados no enunciado ou as aproximações indicadas pelo professor.

---

# PARTE IX — PROCEDIMENTOS PARA TIPOS DE PROBLEMA

## 63. Problemas de cinemática

1. Faça o desenho e determine o eixo positivo.
2. Escreva \(x_0,v_0,a,t,x,v\) e unidades.
3. Identifique se a aceleração é zero, constante ou variável.
4. Escolha a equação que contém os dados e a incógnita sem introduzir incógnitas desnecessárias.
5. Isolar a incógnita simbolicamente antes de substituir.
6. Resolva raízes matemáticas e verifique quais atendem ao tempo e à situação física.
7. Confira com gráfico ou ordem de grandeza.

## 64. Problemas de dinâmica

1. Isole um corpo e desenhe todas as forças externas relevantes.
2. Não inclua forças que o corpo exerce sobre outros corpos no seu próprio diagrama.
3. Decomponha forças inclinadas.
4. Escreva uma equação de Newton por eixo.
5. Acrescente relações de atrito, mola, corda ou contato.
6. Resolva o sistema.
7. Analise se a normal, a tensão e a aceleração obtidas são fisicamente possíveis.

## 65. Problemas de energia e momento

Use energia quando o problema relaciona estados inicial/final e as forças conservativas ou o trabalho total são conhecidos. Use momento quando forças externas são desprezíveis durante o intervalo, especialmente em colisões curtas. Podem ser usados ambos quando existe informação suficiente.

Na energia, identifique a fronteira do sistema e contabilize calor, trabalho e energia potencial pertinentes. No momento, escolha um sistema fechado e avalie o impulso externo. Não aplique “conservação” sem identificar o que está conservado e por quê.

## 66. Problemas elétricos

1. Desenhe polaridades, correntes e componentes.
2. Identifique se é corrente contínua estacionária ou regime transitório/alternado.
3. Para resistores ôhmicos, use V=IR; para nós e malhas, use Kirchhoff.
4. Para capacitores e indutores, inclua relações temporais quando o circuito muda no tempo.
5. Confira unidades: V, A, Ω, F, H, W.
6. Verifique se a potência entregue e dissipada faz sentido.

## 67. Problemas de calor e termodinâmica

1. Declare o sistema e a convenção de sinais para Q e W.
2. Identifique as variáveis de estado e o processo (isotérmico, isobárico, isocórico, adiabático etc.).
3. Determine se o modelo de gás ideal ou calor específico constante é adequado.
4. Use primeira lei e equações de estado compatíveis.
5. Converta temperatura para kelvin quando necessário.
6. Confira energia e sinal do trabalho.
7. Se houver máquina térmica, respeite a segunda lei e os limites de eficiência.

## 68. Problemas de óptica

1. Desenhe o eixo principal, as superfícies e os raios.
2. Defina o meio, índice de refração e ângulos medidos em relação à normal.
3. Escolha reflexão, Snell, equação de lente/espelho ou modelo ondulatório.
4. Declare convenção de sinais para distâncias focais e imagens.
5. Resolva e compare a previsão com o desenho geométrico.
6. Verifique aproximação paraxial, espessura da lente e efeitos de difração se forem relevantes.

## 69. Problemas de física moderna

Em relatividade, compare \(v/c\) e verifique se o regime clássico basta. Em quântica, identifique estado, observável, Hamiltoniano e condições de contorno; não trate probabilidades como trajetórias clássicas. Em nuclear e partículas, mantenha leis de conservação e unidades de energia consistentes, por exemplo eV, MeV, GeV ou joules.

Esses problemas frequentemente exigem equações específicas do curso e matemática avançada. Não escolha fórmulas apenas pela aparência dos símbolos.

---

# PARTE X — ERROS FREQUENTES

## 70. Erros conceituais a evitar

- Confundir distância com deslocamento.
- Confundir rapidez com velocidade vetorial.
- Afirmar que velocidade zero implica aceleração zero.
- Supor que a força resultante é sempre no sentido do movimento.
- Confundir massa com peso.
- Tratar a força centrípeta como uma força adicional separada; ela é a resultante radial.
- Supor que a normal é sempre mg.
- Usar atrito estático igual a \(\mu_sN\) em todos os casos.
- Conservar energia mecânica mesmo quando há trabalho não conservativo sem contabilizá-lo.
- Aplicar \(mgh\) para qualquer altura sem verificar variação de g.
- Aplicar Bernoulli a um fluxo viscoso ou não estacionário sem termos adicionais.
- Usar Celsius em \(pV=nRT\).
- Confundir calor com temperatura.
- Usar V=IR como lei universal para qualquer componente.
- Tratar campo elétrico e potencial elétrico como a mesma grandeza.
- Esquecer que força magnética sobre carga pontual ideal é perpendicular à velocidade.
- Misturar convenções de sinais em lentes e espelhos.
- Aplicar fórmulas clássicas a velocidades próximas de c.
- Tratar a função de onda como uma onda material clássica sem interpretação probabilística.
- Afirmar que a segunda lei diz que energia se perde; energia conserva-se, mas sua capacidade de realizar trabalho pode se degradar.
- Confundir uma simulação ou correlação com prova experimental suficiente.

## 71. Verificações rápidas antes de entregar a resposta

Pergunte:
- A unidade final é a esperada?
- O sinal corresponde ao referencial escolhido?
- A ordem de grandeza faz sentido?
- Usei o dado correto e a hipótese adequada?
- A equação é vetorial ou escalar?
- Há uma solução extra que viola o contexto?
- O resultado exige aproximação que não foi indicada?
- A resposta respeita conservação de energia, carga e/ou momento quando aplicável?
- Expliquei o significado físico, não apenas um número?

---

# PARTE XI — EXERCÍCIOS PROGRESSIVOS

Tente resolver sem consultar o gabarito. Em problemas de cálculo, registre dados, incógnita, lei utilizada, substituição, unidade e verificação.

1. Converta 90 km/h para m/s.
2. Um carro percorre 150 m em 10 s em linha reta. Encontre a velocidade média se não há mudança de direção.
3. Um objeto parte do repouso e acelera a 3 m/s² durante 4 s. Encontre velocidade final e deslocamento.
4. Uma massa de 5 kg tem aceleração de 2 m/s². Qual é a força resultante?
5. Uma força de 20 N atua no mesmo sentido de um deslocamento de 3 m. Calcule o trabalho.
6. Uma massa de 2 kg move-se a 6 m/s. Calcule energia cinética.
7. Um objeto de 4 kg é elevado 2 m perto da Terra, com g=9,8 m/s². Calcule a variação de energia potencial gravitacional.
8. Uma mola de k=200 N/m é comprimida 0,10 m. Calcule a energia potencial elástica ideal.
9. Uma força média de 10 N atua durante 0,5 s. Calcule o impulso.
10. Uma roda gira a 4 rad/s com raio de 0,5 m. Calcule a rapidez tangencial.
11. Água tem densidade aproximada de 1000 kg/m³. Estime a diferença de pressão hidrostática a 2 m de profundidade, com g=9,8 m/s².
12. Uma onda possui frequência 50 Hz e comprimento de onda 4 m. Calcule sua velocidade.
13. Aqueça 0,5 kg de água em 10 °C usando c=4200 J/(kg·°C). Estime o calor sensível.
14. Um gás ideal contém 2 mol a 300 K e ocupa 0,050 m³. Estime a pressão usando R=8,314 J/(mol·K).
15. Um resistor de 6 Ω está submetido a 12 V. Calcule corrente e potência.
16. Duas resistências 4 Ω e 6 Ω estão em série. Calcule a resistência equivalente.
17. Uma carga de 2 C passa em 4 s. Calcule a corrente média.
18. Um fóton tem frequência \(5{,}0\times10^{14}\) Hz. Estime sua energia em joules usando \(h=6{,}626\times10^{-34}\,J\,s\).
19. Em relatividade especial, calcule aproximadamente \(\gamma\) para v=0,6c.
20. Explique por que a fórmula de queda livre constante não serve para qualquer trajetória orbital.
21. Explique a diferença entre força, campo e potencial elétrico.
22. Dê um exemplo de uma situação em que a energia mecânica não se conserva, embora a energia total se conserve.
23. Explique por que as equações do gás ideal exigem temperatura absoluta.
24. Diferencie onda mecânica e onda eletromagnética.
25. Explique o procedimento para resolver qualquer problema de física em uma sequência de etapas, indicando onde checa unidades e hipóteses.

## Gabarito com método resumido

1. \(90/3{,}6=25\,m/s\).
2. \(v_m=\Delta x/\Delta t=150/10=15\,m/s\).
3. \(v=v_0+at=0+3(4)=12\,m/s\). \(\Delta x=v_0t+\frac12at^2=0+\frac12(3)(16)=24\,m\).
4. \(F=ma=5(2)=10\,N\).
5. \(W=Fd\cos0°=20(3)=60\,J\).
6. \(K=\frac12mv^2=\frac12(2)(36)=36\,J\).
7. \(\Delta U=mg\Delta h=4(9{,}8)(2)=78{,}4\,J\).
8. \(U=\frac12kx^2=\frac12(200)(0{,}10)^2=1\,J\).
9. \(J=F\Delta t=10(0{,}5)=5\,N\,s\), equivalente a \(5\,kg\,m/s\).
10. \(v=r\omega=0{,}5(4)=2\,m/s\).
11. \(\Delta p=\rho gh=1000(9{,}8)(2)=19\,600\,Pa\), diferença relativa à superfície.
12. \(v=\lambda f=4(50)=200\,m/s\).
13. \(Q=mc\Delta T=0{,}5(4200)(10)=21\,000\,J\), sem mudança de fase e com c constante.
14. \(p=nRT/V=2(8{,}314)(300)/0{,}050\approx99\,768\,Pa\), perto de \(1{,}0\times10^5\,Pa\).
15. \(I=V/R=12/6=2\,A\); \(P=VI=24\,W\).
16. Série: \(R_{eq}=4+6=10\,\Omega\).
17. \(I=\Delta Q/\Delta t=2/4=0{,}5\,A\).
18. \(E=hf=(6{,}626\times10^{-34})(5{,}0\times10^{14})=3{,}313\times10^{-19}\,J\).
19. \(\gamma=1/\sqrt{1-0{,}6^2}=1/\sqrt{0{,}64}=1{,}25\).
20. Em órbita, aceleração tem direção variável e depende da gravitação; o modelo \(y=y_0+v_0t-\frac12gt^2\) supõe aceleração vertical constante em um referencial local, não movimento orbital global.
21. Força é interação sobre um corpo; campo elétrico é força por unidade de carga; potencial é energia potencial elétrica por unidade de carga. São relacionados, mas têm unidades e significados diferentes.
22. Um bloco que desliza com atrito converte energia mecânica em energia interna e calor; a energia total, contabilizando todas as formas e o ambiente, conserva-se.
23. Temperatura absoluta tem zero no zero termodinâmico e se relaciona à energia térmica no modelo; Celsius possui origem deslocada.
24. Onda mecânica requer um meio material para propagar perturbação; onda eletromagnética é uma oscilação dos campos e pode propagar-se no vácuo.
25. Ler; desenhar; listar dados e incógnita; escolher o modelo e suas hipóteses; escolher a lei; isolar a variável; converter unidades; substituir; calcular; conferir unidade, sinal, magnitude e validade; interpretar a resposta.

---

# PARTE XII — MAPA DE ESTUDOS DO BÁSICO AO AVANÇADO

## 72. Nível 1 — Fundamentos absolutos
- Grandezas, unidades, medidas, notação científica, ordem de grandeza.
- Aritmética, proporções, equações, potências, raízes, gráficos.
- Vetores, componentes, trigonometria e análise dimensional.

## 73. Nível 2 — Física básica
- Cinemática em uma e duas dimensões.
- Leis de Newton, força, atrito, peso, normal, tensão e molas.
- Trabalho, energia, potência, impulso e colisões.
- Densidade, pressão, calor, temperatura, ondas e circuitos simples.

## 74. Nível 3 — Ensino médio completo
- Lançamentos, movimento circular, equilíbrio, torque e gravitação.
- Fluidos, termodinâmica, gases, calorimetria e máquinas térmicas.
- Eletrostática, circuitos, magnetismo, indução e corrente alternada.
- Reflexão, refração, espelhos, lentes, interferência e difração.
- Noções de relatividade, fótons, átomos e radioatividade.

## 75. Nível 4 — Preparação universitária
- Cálculo diferencial e integral.
- Álgebra linear, equações diferenciais e números complexos.
- Mecânica newtoniana com formalismo matemático completo.
- Oscilações, ondas, física experimental e análise de incertezas.
- Termodinâmica formal, eletromagnetismo e óptica física.

## 76. Nível 5 — Física universitária
- Mecânica analítica, métodos matemáticos e física estatística.
- Equações de Maxwell, potenciais e ondas eletromagnéticas.
- Mecânica quântica, átomos e moléculas.
- Relatividade especial e introdução à geral.
- Física nuclear, partículas, estado sólido e métodos computacionais.

## 77. Nível 6 — Áreas avançadas e pesquisa
- Mecânica quântica avançada, teoria de perturbação, espalhamento e muitos corpos.
- Eletrodinâmica clássica avançada e eletrodinâmica quântica.
- Teoria quântica de campos, simetrias, grupos e teorias de calibre.
- Relatividade geral avançada, buracos negros, ondas gravitacionais e cosmologia.
- Física da matéria condensada, supercondutividade, materiais quânticos.
- Plasma, fusão, fluidos não lineares e turbulência.
- Física matemática, caos, sistemas complexos, métodos numéricos e inferência estatística.
- Física de partículas experimental, detectores, aceleradores, astrofísica observacional e instrumentação.

A ordem exata depende da área. Por exemplo, pesquisa em astrofísica exige gravitação, termodinâmica, radiação e estatística; pesquisa em matéria condensada exige quântica, estatística e métodos numéricos; relatividade geral exige cálculo tensorial e geometria diferencial.

---

# PARTE XIII — REFERÊNCIAS ABERTAS E FONTES DE CONSULTA

1. **OpenStax, University Physics, Volume 1** — unidades, vetores, cinemática, leis de Newton, energia, momento, rotação, gravitação, fluidos, oscilações, ondas e som: https://openstax.org/books/university-physics-volume-1/pages/preface
2. **OpenStax, University Physics, Volume 2** — termodinâmica, eletrostática, circuitos, magnetismo, indução e ondas eletromagnéticas: https://openstax.org/books/university-physics-volume-2/pages/preface
3. **OpenStax, University Physics, Volume 3** — óptica, relatividade, fótons, mecânica quântica, estrutura atômica, matéria condensada, nuclear, partículas e cosmologia: https://openstax.org/books/university-physics-volume-3/pages/preface
4. **BIPM — Brochura do SI** — definições e convenções de unidades: https://www.bipm.org/en/si-brochure-9
5. **NIST/CODATA — constantes fundamentais** — valores recomendados, incertezas e referências: https://www.nist.gov/programs-projects/codata-values-fundamental-physical-constants
6. **MIT OpenCourseWare — Física** — cursos introdutórios e avançados, incluindo mecânica, eletromagnetismo, relatividade, quântica e teoria de campos: https://ocw.mit.edu/search/?d=Physics

Essas referências fornecem sequências de aulas, demonstrações, problemas e detalhes além do espaço deste roteiro.

---

## CONCLUSÃO

Física não é uma coleção de fórmulas desconectadas. É uma forma de construir modelos quantitativos, testar previsões e compreender por que certos fenômenos ocorrem. O procedimento fundamental é identificar o sistema, declarar as hipóteses, selecionar a lei física, traduzir a lei em equações, resolver com unidades consistentes e verificar se o resultado faz sentido.

Estudar todos os ramos de maneira aprofundada leva anos e exige matemática progressivamente mais avançada, leitura e prática experimental. Não seria honesto afirmar que um único caderno contém cada detalhe existente em todas as fronteiras da física. Este volume foi preparado para servir como mapa abrangente e base de trabalho, de onde cada ramo pode ser desenvolvido em cursos específicos, com derivações completas, problemas graduados e análises experimentais.