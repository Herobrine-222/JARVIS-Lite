# CADERNO UNIFICADO DE CIÊNCIA QUÂNTICA
## Volume I — Fundamentos, átomo e formalismo da mecânica quântica

### O que é ciência quântica

A ciência quântica estuda os fenômenos físicos que exigem a descrição quântica da matéria, da luz e de outros sistemas.

A teoria fundamental usada para descrever átomos e elétrons em velocidades muito menores que a da luz é a mecânica quântica não relativística.

Quando a relatividade especial também precisa ser incorporada, usam-se teorias relativísticas, como a equação de Dirac ou a teoria quântica de campos.

A física clássica continua sendo extremamente precisa em muitos contextos cotidianos, mas não explica sozinha os espectros atômicos, a estabilidade dos átomos e vários efeitos microscópicos.

Em física, “quântico” não significa mágico, ilimitado ou livre de regras; significa que o sistema obedece a princípios matemáticos específicos cuja previsão é probabilística para certos resultados.

A energia de muitos sistemas ligados só pode assumir determinados valores permitidos, fenômeno chamado quantização.

A quantização não significa que toda grandeza, em todo sistema, tenha sempre valores discretos; partículas livres, por exemplo, podem ter energia contínua.

Luz e outras radiações eletromagnéticas podem ser descritas em termos de fótons, cada um com energia relacionada à frequência.

Elétrons e outras partículas materiais também apresentam comportamento ondulatório, demonstrado por fenômenos de interferência e difração.

A função de onda representa o estado quântico e permite calcular probabilidades de resultados de medidas.

O módulo ao quadrado da função de onda fornece uma densidade de probabilidade, conforme a regra de Born.

Superposição significa que um estado pode ser uma combinação linear de estados possíveis, com amplitudes que interferem entre si.

Emaranhamento é uma propriedade de estados conjuntos de sistemas quânticos que não pode ser descrita como simples combinação de estados independentes.

O princípio da incerteza limita a dispersão simultânea de pares de observáveis incompatíveis, como posição e momento.

Uma medida é uma interação física que produz um resultado registrado; ela não exige que uma pessoa consciente observe o sistema.

A mecânica quântica explica a estrutura atômica, ligações químicas, propriedades de sólidos, lasers, LEDs, transistores e tecnologias de precisão.

A teoria quântica de campos descreve campos quânticos e processos nos quais partículas podem ser criadas ou aniquiladas, dentro das regras de conservação pertinentes.

A relatividade geral descreve a gravidade como geometria do espaço-tempo; uma teoria completa e experimentalmente confirmada da gravidade quântica ainda não existe.

A ciência quântica reúne experimentos, modelos matemáticos, previsões testáveis e revisão contínua de resultados.

A teoria descreve amplitudes e probabilidades, mas também prevê valores médios, espectros, taxas de transição e correlações mensuráveis.

A interferência quântica ocorre quando amplitudes de alternativas coerentes se combinam, reforçando ou reduzindo probabilidades.

O tunelamento quântico permite amplitudes em regiões classicamente proibidas, com probabilidade dependente da forma do potencial.

O spin é uma propriedade intrínseca que influencia os níveis de energia e as interações com campos magnéticos.

A decoerência descreve a perda prática de interferência observável quando um sistema interage com muitos graus de liberdade ambientais.

A física quântica também é estatística: previsões para sistemas individuais podem ser probabilísticas, embora as distribuições sejam calculadas por regras precisas.

Experimentos podem testar essas previsões por meio de espectros, interferência, medidas de correlação, efeitos de campos e dispositivos tecnológicos.

As aproximações científicas têm domínios de validade; indicar as hipóteses de uma equação é parte da explicação, não um detalhe opcional.

A matemática avançada inclui álgebra linear, cálculo diferencial e integral, números complexos e, em níveis posteriores, teoria de grupos e métodos computacionais.

Este capítulo estuda detalhadamente o átomo: sua estrutura, seus estados eletrônicos, a matemática que os descreve, suas interações com a luz e os limites dos modelos usados.

## Sumário deste volume

**Fundamentos iniciais:** linguagem matemática, unidades, números complexos, vetores, matrizes, funções, derivadas, integrais e probabilidade.

**Estrutura atômica:** modelos históricos, núcleo, elétrons, orbitais, números quânticos, energia, espectros, spin, equação de Schrödinger e aplicações atômicas.

**Formalismo geral:** estados, superposição, matrizes de Pauli, operadores densidade, evolução unitária e conservação da probabilidade.

**Sistemas quânticos fundamentais:** dupla fenda, poço infinito e finito, oscilador harmônico e tunelamento.

**Tópicos avançados:** adição de spin, emaranhamento, desigualdades de Bell, métodos de aproximação, sistemas multieletrônicos, materiais e teoria quântica de campos.

**Prática:** exemplos resolvidos, limites de validade, exercícios conceituais e referências para aprofundamento.


## Como usar este volume

Este volume reúne, em uma única obra, a introdução geral, o estudo detalhado do átomo e os fundamentos matemáticos necessários para compreender outros sistemas quânticos. Os capítulos se complementam: uma fórmula apresentada no estudo do átomo volta a aparecer em exemplos mais gerais, e as hipóteses de cada modelo são indicadas onde importam.

A sequência lógica é: linguagem matemática; estrutura atômica; estados e medidas; dupla fenda; sistemas-modelo (poço, oscilador e barreira); spin e adição de momento angular; emaranhamento e desigualdades de Bell; aproximações e átomos multieletrônicos; aplicações e teoria quântica de campos.

Nenhum volume finito pode conter literalmente todas as teorias, cálculos e aplicações existentes na ciência quântica. Este material procura cobrir fundamentos centrais com rigor e mostrar explicitamente quando um tópico exige desenvolvimento adicional, em vez de fingir que uma menção curta esgota o assunto.

## Fundamentos matemáticos necessários

### Números, unidades e notação científica

A física usa números com unidades. O valor numérico de uma grandeza física só fica completo quando a unidade é indicada, como 3 metros, 2 segundos ou 5 elétron-volts.

A notação científica escreve números muito grandes ou muito pequenos na forma a × 10ⁿ, em que a costuma estar entre 1 e 10 e n é um inteiro. Por exemplo, 0,000000001 m = 1 × 10⁻⁹ m = 1 nanômetro.

O prefixo nano corresponde a 10⁻⁹, micro a 10⁻⁶, mili a 10⁻³, quilo a 10³ e mega a 10⁶. Ao substituir unidades numa equação, todos os fatores de conversão precisam ser aplicados de modo consistente.

A energia pode ser medida em joules ou elétron-volts. Um elétron-volt é a energia transferida a uma carga elementar quando ela atravessa uma diferença de potencial de um volt: 1 eV = 1,602176634 × 10⁻¹⁹ J.

A constante de Planck h tem unidade J·s; a constante reduzida ℏ = h/(2π) também tem unidade J·s. O fato de ℏ possuir unidade de ação explica por que aparece em relações entre grandezas angulares, momento e escala quântica.

A análise dimensional verifica se os dois lados de uma equação têm as mesmas unidades. Ela não prova que uma fórmula está correta, mas detecta muitos erros.

### Álgebra, funções e gráficos

Uma função associa uma entrada a uma saída. Na física, a função pode representar posição em função do tempo, energia potencial em função da posição, ou uma função de onda em função da coordenada.

Uma equação linear pode ser resolvida isolando a incógnita; equações quadráticas podem ter duas raízes, uma raiz dupla ou raízes complexas. Em mecânica quântica, números complexos são indispensáveis porque amplitudes podem ter fase.

O número complexo z = a + ib possui parte real a e parte imaginária b, com i² = −1. Seu conjugado é z* = a − ib, e seu módulo ao quadrado é |z|² = z*z = a² + b², uma quantidade real não negativa.

A amplitude quântica pode ser complexa, mas a probabilidade calculada pelo módulo ao quadrado é real e não negativa. A fase relativa entre amplitudes pode alterar interferências; uma fase global comum não altera probabilidades.

### Derivadas e integrais

A derivada mede a taxa local de variação de uma função. Se x(t) é posição em função do tempo, dx/dt é a velocidade; se a função de onda depende da posição, sua derivada descreve como a amplitude varia espacialmente.

A segunda derivada mede como a inclinação muda e aparece na equação de Schrödinger porque está ligada ao operador de energia cinética.

A integral soma contribuições infinitesimais. Em probabilidade quântica, integrar |ψ(x)|² numa região fornece a probabilidade de encontrar a partícula ali. Em três dimensões, o elemento de volume é d³r = dx dy dz, ou uma forma equivalente apropriada às coordenadas usadas.

A integral de uma densidade sobre todo o espaço deve ser igual a 1 para uma função de onda normalizada de uma partícula. Se a integral diverge ou não pode ser normalizada, a função pode não representar um estado ligado normalizável; estados de espalhamento requerem tratamento de normalização diferente.

### Vetores, matrizes e espaços de estados

Um vetor é um objeto matemático que pode representar direção e magnitude, ou uma lista ordenada de componentes. Em mecânica quântica, o estado pode ser representado abstratamente por um vetor ket |ψ⟩ num espaço de Hilbert.

Uma base ortonormal é um conjunto de vetores de referência mutuamente ortogonais e de norma unitária. Se {|n⟩} é uma base, um estado pode ser escrito |ψ⟩ = Σₙ cₙ|n⟩, com amplitudes complexas cₙ.

A normalização de um estado puro exige Σₙ |cₙ|² = 1. A probabilidade de encontrar o resultado n numa medida na base correspondente é |cₙ|².

O produto interno ⟨φ|ψ⟩ compara dois estados e pode ser complexo. A probabilidade de projeção de um estado puro normalizado |ψ⟩ no estado normalizado |φ⟩ é |⟨φ|ψ⟩|².

Uma matriz representa uma transformação linear ou um operador numa base escolhida. Os autovetores de um operador representam estados com resultado definido para o observável associado; os autovalores são os resultados possíveis.

Operadores que representam observáveis são hermitianos ou autoadjuntos nos domínios matemáticos apropriados, de modo que seus valores de medida sejam reais. Operadores unitários descrevem evoluções reversíveis que preservam as normas dos estados.

O comutador [A,B] = AB − BA revela se dois operadores comutam. Se o comutador não é zero, em geral não existe uma base completa de estados próprios comuns aos dois observáveis.

### Probabilidade e estatística

A teoria quântica prevê probabilidades de resultados e, com elas, médias e dispersões. Um resultado individual não precisa coincidir com o valor esperado.

Para resultados aᵢ com probabilidades pᵢ, o valor esperado é ⟨A⟩ = Σᵢ pᵢaᵢ, e a variância é ΔA² = ⟨A²⟩ − ⟨A⟩².

A média de muitas medidas independentes tende a se aproximar do valor esperado conforme aumenta o número de ensaios, sob as condições estatísticas usuais. Uma discrepância em uma única medida não refuta uma previsão probabilística; a comparação deve considerar a distribuição completa e as incertezas experimentais.

### O que é um átomo e como é formado

Um átomo é um sistema formado por um núcleo e por elétrons ligados a esse núcleo pela interação eletromagnética.

O número de prótons do núcleo determina o número atômico Z e, portanto, a identidade química do elemento.

Um átomo eletricamente neutro tem o mesmo número de elétrons e de prótons; se perde ou ganha elétrons, torna-se um íon.

Um cátion é um íon com carga líquida positiva, normalmente formado quando um átomo perde elétrons.

Um ânion é um íon com carga líquida negativa, normalmente formado quando um átomo ganha elétrons.

Prótons têm carga elétrica positiva e nêutrons têm carga elétrica total nula; juntos, prótons e nêutrons são chamados núcleons.

Prótons e nêutrons não são partículas fundamentais: são hádrons compostos por quarks ligados por glúons pela interação forte.

O elétron é classificado como partícula elementar no Modelo Padrão; até hoje, experimentos não demonstraram uma estrutura interna do elétron.

A carga elementar tem magnitude aproximadamente e = 1,602176634 × 10⁻¹⁹ coulomb.

A massa do elétron é aproximadamente 9,109 × 10⁻³¹ quilograma, muito menor que a massa do próton, cerca de 1,673 × 10⁻²⁷ quilograma.

A massa do elétron é aproximadamente 1/1836 da massa do próton, embora a razão precisa dependa dos valores usados e a massa do nêutron seja ligeiramente diferente.

Quase toda a massa de um átomo comum está no núcleo, mas a maior parte do volume atômico está associada à distribuição eletrônica.

Um raio atômico típico está na escala de 10⁻¹⁰ metro, enquanto escalas nucleares estão perto de 10⁻¹⁵ metro; o núcleo é muito menor que o átomo.

Os desenhos de átomos costumam exagerar o tamanho do núcleo para que ele possa ser visto, por isso não estão em escala literal.

A região eletrônica não é uma nuvem de gás comum e nem uma casca sólida; é uma representação da distribuição espacial associada ao estado quântico.

O limite desenhado de um orbital é uma superfície escolhida para visualizar uma fração da probabilidade, não uma parede física.

O núcleo contém carga positiva devido aos prótons, e a atração elétrica entre o núcleo e os elétrons participa da ligação do átomo.

A estabilidade do átomo, contudo, não pode ser explicada apenas pela lei clássica de Coulomb; é necessário usar a descrição quântica dos elétrons.

### Número atômico, número de massa e isótopos

O número atômico Z é a quantidade de prótons no núcleo; para um átomo neutro, também é a quantidade de elétrons.

O número de nêutrons é representado por N, e o número de massa A é A = Z + N.

Isótopos são núcleos do mesmo elemento, com o mesmo Z e diferentes quantidades de nêutrons.

Isótopos de um elemento têm química muito semelhante porque compartilham a mesma carga nuclear, mas diferenças de massa podem alterar propriedades espectroscópicas e taxas de processos físicos.

Um exemplo é o hidrogênio comum, cujo núcleo tem um próton; o deutério tem um próton e um nêutron; o trítio tem um próton e dois nêutrons.

A massa real de um átomo não é simplesmente a soma das massas de seus prótons, nêutrons e elétrons isolados.

A diferença de massa está relacionada à energia de ligação e à equivalência entre massa e energia, E = mc².

A interação forte residual liga núcleons no núcleo, enquanto a repulsão elétrica entre prótons tende a separá-los.

A estabilidade nuclear depende da combinação entre interação forte, repulsão elétrica, números de prótons e nêutrons e efeitos quânticos nucleares.

Decaimentos radioativos e reações nucleares podem alterar o núcleo e transformar um elemento em outro.

Uma reação química comum reorganiza principalmente os elétrons e não transforma um núcleo em outro elemento.

Física atômica, física nuclear e química quântica se relacionam, mas não são nomes diferentes para a mesma área.

### Como os modelos atômicos mudaram

Na Antiguidade, filósofos propuseram que a matéria seria composta por unidades muito pequenas, mas essas propostas não eram teorias experimentais modernas.

No século XIX, John Dalton usou uma teoria atômica para organizar regularidades de proporções nas reações químicas.

A descoberta do elétron por J. J. Thomson mostrou que o átomo tinha partes internas e não era indivisível.

O modelo de Thomson imaginava elétrons em uma distribuição positiva difusa; ele ficou conhecido como modelo do “pudim de passas”.

O espalhamento de partículas alfa em experimentos associados a Ernest Rutherford mostrou que a carga positiva e a maior parte da massa ocupam um núcleo pequeno.

O modelo nuclear de Rutherford substituiu a ideia de carga positiva espalhada por todo o átomo, mas não explicava sozinho a estabilidade e os espectros de linhas.

Niels Bohr introduziu estados de energia quantizados e explicou muitas linhas do espectro do hidrogênio com órbitas permitidas.

O modelo de Bohr é útil historicamente e como aproximação para sistemas de um elétron, mas suas órbitas não são a descrição moderna dos elétrons.

Louis de Broglie propôs que partículas materiais possuem comprimento de onda associado ao momento.

Experimentos de difração de elétrons, incluindo os de Davisson e Germer, confirmaram que elétrons podem manifestar comportamento ondulatório.

Werner Heisenberg desenvolveu uma formulação da mecânica quântica baseada em observáveis e matrizes; Erwin Schrödinger desenvolveu a formulação baseada em funções de onda.

Max Born interpretou o módulo ao quadrado da função de onda como densidade de probabilidade.

A formulação moderna substituiu trajetórias eletrônicas clássicas definidas por estados quânticos e distribuições calculáveis.

Modelos científicos são avaliados pela capacidade de explicar resultados, fazer previsões e resistir a testes experimentais.

### Interação elétrica e Hamiltoniano atômico

A lei de Coulomb descreve, no regime clássico apropriado, a força entre cargas elétricas; a energia potencial de um elétron e um núcleo de carga +Ze é atrativa.

Para um núcleo considerado fixo, o potencial coulombiano do elétron é V(r) = −Ze²/(4πε₀r), onde r é a distância ao núcleo.

O símbolo e representa a magnitude da carga elementar, ε₀ é a permissividade elétrica do vácuo e Z é o número de prótons.

No modelo de um elétron ligado a um núcleo, o Hamiltoniano não relativístico inclui energia cinética e potencial coulombiano.

A equação estacionária pode ser escrita como [−ℏ²∇²/(2μ) − Ze²/(4πε₀r)]ψ = Eψ.

Aqui ℏ = h/(2π), h é a constante de Planck, μ é a massa reduzida do sistema elétron–núcleo, ψ é a função de onda e E é a energia.

A massa reduzida é μ = mₑM/(mₑ + M), onde mₑ é a massa do elétron e M é a massa do núcleo.

Usar a massa reduzida em vez de tratar o núcleo como infinitamente pesado corrige pequenas diferenças nos níveis e nos comprimentos de onda.

O operador laplaciano ∇² mede, em três dimensões, uma combinação de derivadas espaciais da função de onda.

O Hamiltoniano é o operador associado à energia total; na equação de autovalores, Ĥψ = Eψ, os estados próprios têm energia E.

Os resultados dependem das hipóteses: o Hamiltoniano acima descreve um átomo ou íon hidrogenoide não relativístico, sem campos externos e sem correções radiativas.

Para maior precisão, entram efeitos de massa nuclear finita, relatividade, spin, interação spin–órbita, estrutura nuclear e eletrodinâmica quântica.

### Função de onda, probabilidade e normalização

A função de onda é indicada por ψ e representa o estado quântico em uma formulação de uma partícula.

A função de onda pode ser complexa; em geral, seu valor não é uma probabilidade diretamente.

A regra de Born diz que |ψ(r,t)|² é a densidade de probabilidade de encontrar a partícula perto da posição r no instante t.

Para uma região espacial R, a probabilidade de encontrar a partícula nessa região é a integral de |ψ|² sobre R.

Para uma função de onda normalizada em todo o espaço, ∫ |ψ(r,t)|² d³r = 1.

A normalização expressa que a partícula será encontrada em algum resultado do conjunto completo de posições com probabilidade total igual a um.

Uma maior densidade de probabilidade em uma região significa maior frequência relativa de resultados nessa região em muitas preparações equivalentes.

Não significa que um único evento de medição precise ocorrer no ponto de máxima densidade; resultados individuais podem ocorrer em qualquer região com probabilidade não nula.

Uma superfície orbital normalmente é desenhada como uma região que contém uma porcentagem escolhida da probabilidade, como 90% ou 95%, dependendo da convenção didática.

A probabilidade não é necessariamente zero fora dessa superfície; em muitos orbitais, a distribuição diminui gradualmente com a distância.

A fase da função de onda importa para a interferência, embora a fase global multiplicativa não altere as probabilidades de medição.

Se ψ₁ e ψ₂ forem amplitudes coerentes, a amplitude combinada é ψ = aψ₁ + bψ₂; a probabilidade depende de |aψ₁ + bψ₂|², que contém termos de interferência.

É por isso que somar amplitudes quânticas não equivale, em geral, a somar probabilidades clássicas independentes.

### Equação de Schrödinger e evolução temporal

A equação de Schrödinger dependente do tempo é iℏ ∂ψ/∂t = Ĥψ, para um sistema descrito pela mecânica quântica não relativística.

Ela determina a evolução temporal do estado quando o Hamiltoniano e o estado inicial são conhecidos.

Se o Hamiltoniano não depender explicitamente do tempo, podem existir estados estacionários que resolvem Ĥψ = Eψ.

Uma solução estacionária possui fator temporal e^(−iEt/ℏ); para um estado de energia única, a densidade |ψ|² permanece constante no tempo.

Isso não significa que o elétron seja uma bolinha parada; significa que a distribuição de resultados de posição do estado estacionário não muda com o tempo.

Um estado que seja superposição de diferentes energias pode apresentar densidades ou observáveis que variam no tempo por interferência entre os componentes.

A equação de Schrödinger é não relativística e não constitui, sozinha, uma teoria universal para partículas a velocidades próximas à da luz ou para criação e aniquilação de partículas.

A equação de Dirac fornece uma descrição relativística do elétron e incorpora naturalmente spin 1/2.

A eletrodinâmica quântica, uma teoria quântica de campos, descreve a interação entre elétrons, pósitrons e o campo eletromagnético, incluindo correções radiativas.

### Valores esperados, operadores e observáveis

Na formulação de operadores, cada grandeza física observável é representada por um operador adequado ao sistema.

O resultado de uma medida ideal de um observável corresponde a um de seus autovalores permitidos.

Se Â representa um observável, um estado próprio satisfaz Âψ = aψ, sendo a o valor próprio associado.

O valor esperado de uma grandeza para muitas preparações idênticas é calculado por ⟨A⟩ = ∫ ψ*Âψ d³r, quando essa representação e integral se aplicam.

O valor esperado é uma média estatística prevista pela teoria, não necessariamente um resultado que aparecerá em uma única medição.

Para posições, o operador é multiplicação pela coordenada; para o momento em representação espacial, o operador é −iℏ∇.

Operadores associados a observáveis são representados matematicamente como operadores autoadjuntos, condição ligada à obtenção de valores de medida reais.

Dois observáveis podem não possuir estados próprios simultâneos completos quando seus operadores não comutam.

O comutador é [Â,B̂] = ÂB̂ − B̂Â; para posição x e momento pₓ, [x̂,p̂ₓ] = iℏ.

Essa relação não comutativa está ligada à relação de incerteza entre as distribuições de posição e momento.

### Solução do átomo de hidrogênio e energias permitidas

O átomo de hidrogênio neutro possui um próton no núcleo e um elétron ligado a ele.

Íons como He⁺ e Li²⁺ também têm apenas um elétron; são chamados sistemas hidrogenoides e podem ser tratados com a mesma estrutura matemática, ajustando Z e a massa reduzida.

No modelo não relativístico ideal, a energia dos níveis ligados de um sistema hidrogenoide é aproximadamente Eₙ = −13,6 eV × Z²/n².

O valor 13,6 eV é uma aproximação para a energia de ionização do hidrogênio no estado fundamental, com pequenas correções em cálculos de alta precisão.

O número quântico principal n assume valores inteiros positivos: 1, 2, 3, e assim por diante.

Quanto maior n, em geral maior é o tamanho característico e menos negativa é a energia de um estado ligado hidrogenoide.

A energia negativa indica um estado ligado tomando como zero de energia o elétron livre mais o núcleo separados no repouso.

O limite E = 0 corresponde ao limiar de ionização no modelo; para E ≥ 0 há estados de espalhamento não ligados com energias em um contínuo.

Para uma emissão do nível inicial nᵢ para um nível final menor n_f no hidrogênio ideal, a fórmula de Rydberg é 1/λ = R_H(1/n_f² − 1/nᵢ²), onde R_H é a constante de Rydberg apropriada ao hidrogênio.

A fórmula de Rydberg é uma forma espectral da diferença entre níveis; para alta precisão, usa-se a massa reduzida e corrigem-se os efeitos finos, hiperfinos e radiativos.

No hidrogênio ideal, muitos estados com diferentes l e mℓ possuem a mesma energia se têm o mesmo n; isso é chamado degenerescência.

A degenerescência do hidrogênio ideal é parcialmente alterada por estrutura fina, deslocamento de Lamb, efeitos hiperfinos, campos externos e massa nuclear finita.

Em átomos com vários elétrons, repulsões entre elétrons alteram as energias; orbitais com o mesmo n mas diferentes subcamadas geralmente deixam de ser degenerados.

O estado fundamental é o estado de menor energia permitido; estados acima dele são excitados.

Um estado excitado pode decair para um estado de energia menor por emissão de radiação ou por outros processos que transfiram energia ao ambiente.

### Cálculo resolvido: energia do hidrogênio

Para o hidrogênio, Z = 1, então Eₙ ≈ −13,6 eV/n².

No estado fundamental, n = 1, logo E₁ ≈ −13,6 eV.

Para n = 2, E₂ ≈ −13,6/4 = −3,40 eV.

A energia necessária para ionizar o hidrogênio no estado n = 2 é a diferença entre zero e E₂, isto é, aproximadamente 3,40 eV.

Para n = 3, E₃ ≈ −13,6/9 = −1,51 eV.

Para He⁺, Z = 2 e n = 1, de modo que E₁ ≈ −13,6 × 4 = −54,4 eV.

O resultado de −54,4 eV é o nível fundamental aproximado de He⁺; ele não é a energia de ionização do hélio neutro, que tem dois elétrons.

Essas contas usam o modelo hidrogenoide simples; a precisão real exige considerar massa reduzida, relatividade e outras correções conforme o nível de detalhe desejado.

### Orbitais e separação das variáveis

Para um potencial coulombiano central, a função de onda do hidrogênio pode ser separada em uma parte radial e uma angular.

A forma usual é ψₙℓm(r,θ,φ) = Rₙℓ(r)Yℓᵐ(θ,φ), onde Rₙℓ é a função radial e Yℓᵐ são harmônicos esféricos.

A parte radial descreve como a amplitude varia com a distância ao núcleo; a parte angular descreve sua dependência das direções.

Os orbitais s têm ℓ = 0 e densidade angularmente esférica.

Os orbitais p têm ℓ = 1 e podem ser representados por lóbulos opostos; a forma visual depende da combinação real ou complexa de harmônicos esféricos.

Os orbitais d têm ℓ = 2 e incluem formas com quatro lóbulos e uma forma com dois lóbulos e uma região anular.

Os orbitais f têm ℓ = 3 e formas angulares mais complexas; para valores ainda maiores, usam-se g, h e outras letras em contextos especializados.

As letras s, p, d e f são nomes históricos para classes de subcamadas, não abreviações de propriedades físicas fundamentais.

Os orbitais não são caminhos percorridos; são funções de estado ou aproximações de estados de um elétron dentro do modelo.

Em átomos reais com muitos elétrons, orbitais de um elétron são ferramentas aproximadas, pois o estado verdadeiro envolve correlações entre todos os elétrons.

### Nós radiais e angulares

Um nó é uma região em que uma função de onda específica se anula, levando a densidade de probabilidade nula nesse ponto ou superfície.

Para orbitais hidrogenoides não relativísticos, o número de nós radiais é n − ℓ − 1.

O número de nós angulares é ℓ; portanto, o total de nós radiais e angulares é n − 1.

O orbital 1s possui n = 1 e ℓ = 0, portanto não tem nós radiais nem angulares.

O orbital 2s possui n = 2 e ℓ = 0, portanto tem um nó radial.

O orbital 2p tem n = 2 e ℓ = 1, portanto não tem nós radiais e tem um nó angular.

O enunciado de nós depende do modelo do orbital e não deve ser transferido sem cuidado para qualquer estado aproximado de um átomo complexo.

A densidade pontual |ψ|² do orbital 1s do hidrogênio é máxima no núcleo, mas a distribuição radial de probabilidade 4πr²|R₁₀(r)|² tem máximo em r = a₀.

Essa distinção importa: “densidade por unidade de volume” e “probabilidade em uma casca esférica” são quantidades diferentes.

### Os quatro números quânticos

O número quântico principal n assume inteiros positivos e está ligado à energia e ao tamanho característico dos orbitais.

O número quântico orbital ℓ pode assumir valores inteiros de 0 até n − 1.

O valor ℓ = 0 identifica subcamadas s, ℓ = 1 identifica p, ℓ = 2 identifica d e ℓ = 3 identifica f.

O número quântico magnético orbital mℓ assume valores inteiros de −ℓ até +ℓ.

A quantidade de orbitais espaciais em uma subcamada de número ℓ é 2ℓ + 1.

Assim, uma subcamada s tem um orbital, uma p tem três, uma d tem cinco e uma f tem sete.

O quarto número, mₛ, descreve a componente de spin do elétron e pode assumir +1/2 ou −1/2 para um eixo de quantização escolhido.

Esses valores não significam que o elétron seja uma esfera clássica girando literalmente em duas direções.

Para n = 1, só existe ℓ = 0 e mℓ = 0; há um orbital espacial e duas possibilidades de spin.

Para n = 2, ℓ pode ser 0 ou 1, totalizando 1 + 3 = 4 orbitais espaciais e até 8 estados incluindo spin.

Para n = 3, ℓ pode ser 0, 1 ou 2, totalizando 1 + 3 + 5 = 9 orbitais espaciais e até 18 estados incluindo spin.

No modelo ideal hidrogenoide, há n² orbitais espaciais para uma camada n e 2n² estados eletrônicos incluindo spin, antes de considerar outras divisões de energia.

A contagem 2n² expressa possibilidades de estado para a camada; não significa que todo átomo real tenha necessariamente todos esses estados ocupados.

### Momento angular orbital, spin e momento angular total

O número quântico ℓ determina o módulo do momento angular orbital, cujo quadrado tem autovalor ℓ(ℓ + 1)ℏ².

A componente z do momento angular orbital tem autovalor mℓℏ.

Para ℓ = 1, o módulo do momento angular é √2ℏ, mas a componente z só pode assumir −ℏ, 0 ou +ℏ.

Spin é um momento angular intrínseco; para o elétron, s = 1/2 e S² tem autovalor s(s + 1)ℏ².

Uma componente de spin medida ao longo de um eixo escolhido apresenta valores mₛℏ, com mₛ = ±1/2.

O momento magnético associado ao spin faz com que o elétron interaja com campos magnéticos.

Em tratamentos mais precisos, o momento angular orbital L e o spin S se combinam para formar o momento angular total J = L + S.

Para um elétron com ℓ > 0, o número quântico j pode ser ℓ + 1/2 ou ℓ − 1/2; para ℓ = 0, j = 1/2.

O número quântico magnético total mⱼ assume valores de −j a +j em passos inteiros.

A combinação de momento orbital e spin é essencial para explicar a estrutura fina de muitos espectros atômicos.

### Princípio de Pauli e natureza dos elétrons

Elétrons são férmions idênticos e o estado conjunto de vários elétrons deve ser antissimétrico sob troca de dois elétrons.

O princípio de exclusão de Pauli diz que dois elétrons do mesmo átomo não podem ter os mesmos quatro números quânticos n, ℓ, mℓ e mₛ.

Por isso, um orbital espacial pode conter no máximo dois elétrons, com valores opostos da componente de spin no eixo escolhido.

Essa regra não é uma força mecânica comum de repulsão; é consequência da simetria quântica dos estados de férmions idênticos.

Fótons são bósons e seguem estatística diferente; muitos fótons podem ocupar o mesmo modo do campo eletromagnético.

Pauli contribui para a organização eletrônica dos átomos e para propriedades da matéria condensada, incluindo a estrutura da tabela periódica.

A explicação de por que objetos sólidos resistem à sobreposição envolve Pauli, interações eletromagnéticas e a estrutura quântica da matéria, não apenas um único princípio isolado.

### Configurações eletrônicas e tabela periódica

Em átomos com vários elétrons, a configuração eletrônica resume a ocupação aproximada dos orbitais.

A escrita 1s² significa que o subnível 1s está ocupado por dois elétrons; o expoente representa a quantidade de elétrons.

No estado fundamental, o hidrogênio tem configuração 1s¹ e o hélio tem 1s².

O lítio neutro tem configuração fundamental aproximada 1s² 2s¹, e o carbono tem 1s² 2s² 2p².

O princípio de Aufbau é uma regra prática para colocar elétrons nos estados disponíveis de menor energia, mas existem exceções porque as energias dependem das interações reais.

A regra de Hund diz que orbitais degenerados tendem a ser ocupados individualmente com spins paralelos antes de ocorrer o pareamento, no regime comum de preenchimento do estado fundamental.

Uma subcamada s suporta até 2 elétrons, uma p até 6, uma d até 10 e uma f até 14.

A tabela periódica ordena os elementos pelo número atômico, e a repetição dos padrões de valência está por trás da periodicidade química.

Elétrons de valência são especialmente relevantes para ligações e reatividade, embora defini-los não seja simplesmente contar os elétrons mais externos em todos os casos.

Ao formar um cátion, o átomo perde elétrons; a remoção real ocorre nos estados energeticamente apropriados do átomo ou íon, não por uma regra simplista universal.

Ao formar um ânion, elétrons adicionais podem ocupar estados permitidos se o processo for energeticamente favorável.

A blindagem eletrônica reduz parcialmente a atração nuclear efetiva sentida pelos elétrons externos, porque outros elétrons também contribuem para o campo elétrico.

A carga nuclear efetiva, a repulsão entre elétrons e as correlações eletrônicas fazem as energias de subníveis diferentes em átomos multieletrônicos.

O raio atômico não tem uma borda rígida; valores tabelados variam conforme a definição química ou experimental usada.

Energia de ionização é a energia necessária para remover um elétron de uma espécie isolada sob condições especificadas.

Afinidade eletrônica descreve a mudança de energia quando uma espécie gasosa recebe um elétron; é necessário verificar a convenção de sinal usada na tabela consultada.

Eletronegatividade descreve a tendência de um átomo atrair densidade eletrônica numa ligação e não possui uma única definição experimental universal.

### Átomos multieletrônicos e métodos aproximados

Em um átomo com muitos elétrons, o Hamiltoniano contém a energia cinética de cada elétron, a atração de cada elétron pelo núcleo e a repulsão entre pares de elétrons.

A repulsão eletrônica pode ser escrita como uma soma de termos e²/(4πε₀rᵢⱼ), em que rᵢⱼ é a distância entre os elétrons i e j.

A função de onda completa depende das coordenadas de todos os elétrons e, por isso, o problema cresce rapidamente em complexidade.

O modelo de partículas independentes aproxima o efeito dos outros elétrons por um campo efetivo, sendo útil mas não exato.

O método Hartree–Fock descreve cada elétron por orbitais e incorpora a troca exigida pela antissimetria, mas não inclui toda a correlação dinâmica eletrônica.

A teoria do funcional da densidade, ou DFT, trabalha com a densidade eletrônica; sua precisão depende do funcional aproximado escolhido.

Interação de configurações (CI), teoria de perturbações, coupled cluster e outros métodos tratam correlação eletrônica de maneiras diferentes e têm custos e limites próprios.

Nenhum método computacional é universalmente perfeito: sua adequação depende do tamanho do sistema, da propriedade procurada, dos efeitos relativísticos e da precisão necessária.

Para átomos pesados, efeitos relativísticos e acoplamento spin–órbita podem alterar consideravelmente as configurações eletrônicas e os espectros.

### Energia, absorção e emissão de fótons

Um fóton é um quantum do campo eletromagnético, e sua energia está relacionada à frequência por Eγ = hf.

A constante de Planck é exatamente h = 6,62607015 × 10⁻³⁴ joule-segundo no Sistema Internacional.

A frequência f relaciona-se ao comprimento de onda λ pela expressão c = λf no vácuo, onde c ≈ 2,99792458 × 10⁸ metros por segundo.

Com a energia expressa em elétron-volts e o comprimento de onda em nanômetros, uma aproximação prática é Eγ(eV) ≈ 1239,84/λ(nm).

Um átomo absorve um fóton quando há um processo de transição compatível com a energia, as regras de seleção e o ambiente físico.

Durante a absorção, a energia total do sistema aumenta; numa emissão, o átomo passa a um estado de energia menor e a energia liberada pode surgir como um fóton.

Para uma transição simples sem outras trocas relevantes, a energia do fóton é |ΔE| = |E_f − E_i|.

A conservação de energia e de momento vale para o processo completo; o átomo pode sofrer um pequeno recuo, e colisões ou o ambiente podem compartilhar energia.

Um elétron em um estado estacionário ideal não emite continuamente radiação; a emissão ocorre durante uma transição ou processo específico.

A emissão espontânea ocorre sem um fóton incidente controlando a transição; a emissão estimulada ocorre quando o campo incidente induz emissão coerente em um modo apropriado.

A absorção, a emissão espontânea e a emissão estimulada são processos centrais em espectroscopia e no funcionamento de lasers.

A intensidade de um feixe não equivale à energia de um único fóton; depende também do número de fótons que atravessam uma área por unidade de tempo.

Frequência mais alta significa maior energia por fóton; comprimento de onda menor corresponde a frequência maior no vácuo.

A luz visível é apenas uma parte do espectro eletromagnético; infravermelho tem comprimento de onda maior que a luz vermelha, enquanto ultravioleta tem comprimento de onda menor que a luz violeta.

Raios X e raios gama são radiações de fótons de alta energia; a distinção entre eles também considera sua origem, e suas faixas podem se sobrepor dependendo da convenção.

### Cálculo resolvido: transição do hidrogênio de n = 3 para n = 2

A energia inicial é E₃ ≈ −13,6/9 = −1,511 eV; a energia final é E₂ ≈ −13,6/4 = −3,400 eV.

A variação da energia do átomo é ΔE = E_f − E_i ≈ −3,400 − (−1,511) = −1,889 eV.

O sinal negativo mostra que o átomo perdeu energia; o fóton emitido carrega aproximadamente 1,889 eV.

Usando λ(nm) ≈ 1239,84/Eγ(eV), obtemos λ ≈ 1239,84/1,889 ≈ 656,3 nanômetros.

Esse comprimento de onda fica na região vermelha do espectro visível e corresponde à linha H-alfa da série de Balmer, no modelo simplificado.

A linha observada real apresenta pequenas correções por estrutura fina, massa nuclear finita, deslocamento de Lamb e outros efeitos de precisão.

### Cálculo resolvido: excitação e ionização do hidrogênio

Para promover o elétron do estado fundamental n = 1 ao nível n = 2, a energia necessária é ΔE = E₂ − E₁.

Com E₁ ≈ −13,6 eV e E₂ ≈ −3,40 eV, ΔE ≈ 10,2 eV, que deve ser fornecida ao sistema.

O comprimento de onda do fóton com essa energia é aproximadamente λ = 1239,84/10,2 ≈ 121,6 nanômetros, na região ultravioleta.

A energia mínima para ionizar o hidrogênio no estado fundamental é aproximadamente 13,6 eV, equivalente a um comprimento de onda limite de cerca de 91,2 nm para um fóton único.

Se o átomo já estiver no estado n = 2, a energia de ionização restante é de aproximadamente 3,40 eV.

A ionização também pode ocorrer por colisões, campos intensos e outros mecanismos; não exige exclusivamente a absorção de um único fóton.

### Espectros de emissão, absorção e identificação de elementos

Um espectro de emissão registra a radiação emitida, enquanto um espectro de absorção mostra frequências retiradas ou enfraquecidas de uma radiação incidente.

Cada elemento tem energias permitidas próprias e, por isso, apresenta padrões característicos de linhas espectrais.

A espectroscopia permite identificar elementos em lâmpadas, plasmas, estrelas e nebulosas; a interpretação também depende da temperatura, densidade, ionização e condições do material.

No hidrogênio, a série de Lyman envolve transições para n = 1 e fica principalmente no ultravioleta.

A série de Balmer envolve transições para n = 2 e inclui linhas visíveis, além de outras linhas da série em regiões adjacentes do espectro.

A série de Paschen termina em n = 3 e fica no infravermelho; séries com n final 4 e 5 são chamadas Brackett e Pfund, respectivamente.

A posição das linhas permite inferir diferenças de energia; deslocamentos sistemáticos podem indicar movimento relativo pelo efeito Doppler ou condições ambientais.

A intensidade das linhas depende das probabilidades de transição, da quantidade de átomos em cada estado e de fatores como temperatura e densidade.

O formato e a largura das linhas podem ser alterados pelo movimento térmico, colisões, campos externos, tempo de vida finito dos estados e resolução do instrumento.

A largura natural está ligada ao tempo de vida dos estados envolvidos; o alargamento Doppler decorre da distribuição de velocidades, e o alargamento colisional decorre das interações com partículas vizinhas.

### Regras de seleção e probabilidades de transição

Nem toda diferença de energia produz uma linha com a mesma intensidade: a transição precisa ter uma amplitude de probabilidade não nula para o tipo de interação considerado.

Para transições de dipolo elétrico em um sistema hidrogenoide, uma regra central é Δℓ = ±1.

Para um tratamento angular mais completo de dipolo elétrico, ΔJ = 0 ou ±1, mas a transição J = 0 para J = 0 é proibida para dipolo elétrico.

A componente magnética pode mudar conforme ΔM = 0 ou ±1, dependendo da polarização e da geometria da radiação.

Em uma descrição de acoplamento LS com interação de dipolo elétrico, a mudança de paridade é necessária e ΔS = 0 é uma regra aproximada útil.

As regras dependem do tipo de multipolo; transições de dipolo magnético e quadrupolo elétrico seguem regras diferentes.

“Proibida” costuma significar muito menos provável no modelo dominante, não absolutamente impossível em qualquer circunstância.

Transições fracas podem ocorrer por outros multipolos, mistura de estados, interações hiperfinas ou outros mecanismos que não são dominantes.

A amplitude de uma transição de dipolo elétrico envolve elementos de matriz como ⟨ψ_f|r̂|ψ_i⟩, incluindo a integral entre os estados inicial e final.

As regras de seleção surgem de simetrias e propriedades angulares, não de uma escolha arbitrária dos cientistas.

### Estrutura fina: correções relativísticas e spin–órbita

A estrutura fina é a divisão de níveis que o modelo não relativístico mais simples não explica completamente.

Entre suas contribuições estão correções relativísticas à energia cinética, a interação spin–órbita e, para estados s no tratamento apropriado, o termo de Darwin.

A interação spin–órbita relaciona o momento angular orbital do elétron com seu spin e contribui para a divisão de níveis de mesmo n e ℓ em diferentes j.

A equação de Dirac descreve o elétron relativístico e prevê parte importante da estrutura fina sem introduzir o spin como um apêndice clássico.

A estrutura fina pode ser observada como separação de componentes de linhas espectrais quando a resolução do experimento é suficiente.

A descrição de alta precisão inclui correções adicionais que vão além da estrutura fina relativística básica.

### Deslocamento de Lamb e eletrodinâmica quântica

A equação de Dirac sozinha prevê degenerescências que não correspondem exatamente a todos os níveis medidos no hidrogênio.

O deslocamento de Lamb é uma pequena diferença de energia associada a efeitos da eletrodinâmica quântica, incluindo a interação com flutuações do campo eletromagnético e a autoenergia do elétron.

O deslocamento de Lamb foi medido experimentalmente e tornou-se uma confirmação importante da eletrodinâmica quântica.

Cálculos de alta precisão também precisam considerar polarização do vácuo, recuo do núcleo, tamanho finito do núcleo e outras correções conforme o nível de precisão.

O termo “vácuo quântico” refere-se ao estado de menor energia dos campos na teoria; não significa que seja possível obter energia ilimitada do nada.

### Estrutura hiperfina e deslocamento isotópico

A estrutura hiperfina é uma divisão ainda mais fina de níveis atômicos, ligada principalmente à interação entre momentos magnéticos e outras propriedades nucleares e eletrônicas.

O spin nuclear I combina-se com o momento angular eletrônico J para formar o momento angular total atômico F, em condições em que esse acoplamento é adequado.

Diferentes valores de F podem apresentar energias ligeiramente diferentes, produzindo transições hiperfinas.

A famosa transição de 21 centímetros do hidrogênio neutro está associada a uma transição hiperfina entre estados do spin combinado do elétron e do próton.

O deslocamento isotópico ocorre porque isótopos do mesmo elemento têm massas nucleares diferentes e distribuições de carga nuclear que podem ser diferentes.

As diferenças isotópicas alteram levemente níveis e comprimentos de onda; em alta precisão, elas ajudam a estudar massas e tamanhos nucleares.

### Efeitos Zeeman e Stark: campos magnéticos e elétricos

O efeito Zeeman é a alteração e, muitas vezes, a divisão de níveis e linhas espectrais na presença de um campo magnético.

O campo interage com momentos magnéticos associados ao momento angular orbital, ao spin e, quando relevante, ao núcleo.

Em um regime simples, a ordem de grandeza do deslocamento magnético é ligada ao magneton de Bohr μB = eℏ/(2mₑ), ao campo B e aos fatores quânticos do estado.

O efeito Stark é a alteração de níveis atômicos causada por um campo elétrico externo.

Campos fracos e fortes podem produzir padrões diferentes; a abordagem correta depende da intensidade do campo e da estrutura do átomo.

Esses efeitos são usados em espectroscopia, diagnóstico de plasmas, estudo de campos magnéticos estelares e controle de estados atômicos.

### Medição, colapso, superposição e decoerência

Em mecânica quântica, uma medida tem uma distribuição de resultados determinada pelo estado e pelo observável medido.

Se um estado é superposição de estados próprios de energia, uma medição ideal de energia pode obter um dos autovalores, com probabilidades dadas pelas amplitudes correspondentes.

O formalismo convencional atualiza o estado condicionado ao resultado da medida; isso costuma ser chamado postulado de redução ou colapso do estado.

Diferentes interpretações discutem o significado físico desse processo, mas em geral concordam nas previsões experimentais usuais da teoria.

A palavra “observador” não exige consciência humana; um detector ou ambiente pode registrar informação por interação física.

Decoerência descreve como o emaranhamento com o ambiente reduz a observabilidade de interferências entre certas alternativas do sistema.

A decoerência ajuda a explicar por que superposições macroscópicas são difíceis de observar, mas não resolve sozinha todas as questões interpretativas do problema da medição.

Probabilidades quânticas não significam que todos os resultados sejam igualmente prováveis; as probabilidades dependem do estado e das amplitudes.

### Princípio da incerteza

O princípio da incerteza de Heisenberg limita a dispersão conjunta de observáveis incompatíveis, como a posição x e o momento pₓ.

A relação usual é Δx Δpₓ ≥ ℏ/2, onde Δ representa o desvio-padrão estatístico definido no estado quântico.

Essa limitação não é simplesmente resultado de aparelhos imprecisos; ela decorre da estrutura dos estados quânticos e da não comutatividade dos observáveis.

Ela não diz que é impossível medir qualquer grandeza com precisão nem que “tudo é incerto”; diz que certos pares não podem ter dispersões arbitrariamente pequenas ao mesmo tempo.

Para uma onda bem localizada no espaço, a distribuição de momentos precisa ser mais espalhada; uma onda com momento muito definido estende-se espacialmente.

A relação energia–tempo precisa de cuidado: em sua formulação comum, não é simplesmente o mesmo tipo de relação entre dois observáveis como posição e momento.

Um estado com vida média curta costuma ter uma largura energética maior; a interpretação depende da situação e da definição de tempo usada.

### Dualidade onda-partícula e comprimento de onda de de Broglie

A relação de de Broglie associa a uma partícula com momento p um comprimento de onda λ = h/p.

Para uma partícula não relativística, p = mv, mas em regime relativístico o momento deve ser calculado com a expressão relativística apropriada.

O comportamento ondulatório é observado por difração e interferência de elétrons e outras partículas em condições controladas.

A onda de matéria não é uma onda clássica de água nem prova de que uma partícula seja uma pequena esfera dividida ao meio.

Um exemplo prático, no regime não relativístico, é um elétron acelerado por uma diferença de potencial V: aproximadamente λ(nm) ≈ 1,227/√V, com V em volts, se efeitos relativísticos forem pequenos.

Para V = 150 volts, λ ≈ 1,227/√150 nm ≈ 0,100 nm, escala comparável a distâncias entre átomos em cristais.

Essa aproximação é útil para estimativas; quando a tensão fica alta, é necessário corrigir o momento relativístico.

### Fotoionização, colisões e outros processos atômicos

Fotoionização ocorre quando o átomo absorve energia suficiente para liberar um elétron e passar a um estado ionizado.

A energia do fóton precisa exceder o limiar de ionização do estado inicial, respeitando a conservação de energia e as condições do processo.

Excitação por colisão ocorre quando uma partícula transfere energia ao átomo durante uma colisão; o átomo pode depois emitir luz ao decair.

Recombinação é a captura de um elétron por um íon, frequentemente acompanhada de emissão de energia em forma de fóton ou partilha energética com outros participantes.

Em certas espécies, a emissão de um elétron Auger ocorre quando uma transição eletrônica transfere energia para outro elétron, que é ejetado, em vez de toda a energia sair como fóton.

Fluorescência de raios X pode ocorrer quando uma vacância em camada interna é preenchida e a diferença de energia é emitida como fóton de raio X.

A competição entre canais radiativos e não radiativos determina a vida média e os produtos de uma transição.

### Tunelamento e limites de estados ligados

Tunelamento quântico é a possibilidade de uma amplitude atravessar uma região que seria proibida para uma partícula clássica com a mesma energia.

Ele pode influenciar ionização em campos fortes, emissão de elétrons, reações e dispositivos, dependendo do potencial e da escala do sistema.

Tunelamento não significa que qualquer barreira possa ser atravessada com probabilidade grande; a probabilidade depende fortemente da altura e largura da barreira e da energia do estado.

Em átomos, estados ligados possuem energia abaixo do limiar de ionização, enquanto estados acima do limiar podem corresponder a elétrons livres ou de espalhamento.

Campos muito intensos podem modificar o potencial de ligação e permitir ionização por mecanismos que exigem modelos mais avançados.

### Limites clássicos e correspondência

A mecânica quântica não invalida a física clássica; esta continua sendo uma aproximação excelente quando as escalas e condições tornam efeitos quânticos pouco perceptíveis.

Em sistemas com muitas partículas ou em estados apropriados, médias quânticas podem aproximar previsões clássicas, embora nem todo estado quântico tenha um limite clássico simples.

O princípio de correspondência historicamente relaciona previsões quânticas e clássicas em regimes pertinentes, mas não deve ser confundido com uma afirmação de que toda trajetória clássica está literalmente escondida dentro da teoria.

A estabilidade atômica resulta da estrutura quântica dos estados ligados, do comportamento ondulatório, da interação eletromagnética e, para muitos elétrons, do princípio de Pauli.

A imagem de elétrons como planetas em órbita é uma analogia histórica e não deve ser tomada como descrição exata do átomo real.

### Aplicações científicas e tecnológicas

Lasers dependem de transições entre níveis, emissão estimulada e condições que favorecem a amplificação de um modo óptico.

LEDs emitem luz em materiais semicondutores por processos eletrônicos de recombinação e transições entre estados de energia.

Transistores dependem da estrutura quântica dos materiais e do controle de portadores de carga; são componentes fundamentais dos circuitos digitais.

Painéis fotovoltaicos convertem energia luminosa em energia elétrica por processos quânticos e de transporte de carga nos materiais.

Relógios atômicos usam frequências de transições muito estáveis para definir e realizar medidas precisas do tempo.

A espectroscopia atômica identifica elementos e mede propriedades de gases, plasmas, estrelas e materiais.

Microscópios eletrônicos exploram o comprimento de onda de de Broglie e a interação de elétrons com amostras para formar imagens de alta resolução.

Sensores quânticos usam estados sensíveis a campos, frequências ou acelerações para medições de grande precisão.

A computação quântica usa qubits e operações quânticas; ela pode oferecer vantagens em tarefas específicas, mas não é automaticamente mais rápida para todos os problemas.

A criptografia quântica usa princípios quânticos em certos protocolos para detectar interferência ou interceptação sob hipóteses bem definidas; isso não torna qualquer sistema digital invulnerável.

A palavra “quântico” em publicidade não garante que um produto use efeitos quânticos especiais nem que suas alegações tenham evidência científica.

### O que não deve ser confundido

Um orbital não é uma órbita planetária e a nuvem eletrônica não é uma névoa material comum.

A probabilidade quântica não significa que qualquer resultado tenha a mesma probabilidade.

A superposição não permite escolher resultados à vontade por pensamento ou desejo.

O princípio da incerteza não é apenas erro instrumental, nem implica que a ciência nada possa conhecer com precisão.

A medida quântica não exige consciência humana; interações físicas podem produzir registros e decoerência.

A energia de um fóton depende da frequência, não simplesmente do brilho observado de uma fonte.

A quantização dos estados ligados não significa que toda energia possível no universo seja discreta.

O modelo de Bohr é útil em certos contextos, mas orbitais e funções de onda são a descrição moderna apropriada para os elétrons atômicos.

“Estado estacionário” não significa elétron parado, mas estado de energia definida cuja distribuição de probabilidade não varia no tempo no modelo ideal.

Regras de seleção indicam processos dominantes e simetrias, mas transições chamadas proibidas podem ocorrer por mecanismos mais fracos.

A descrição do átomo com orbitais independentes é uma aproximação para muitos elétrons; o estado real pode conter correlações entre eles.

### Resumo conceitual e referências

O número de prótons define o elemento, os nêutrons distinguem isótopos e os elétrons determinam grande parte da química do átomo.

A função de onda e a regra de Born permitem calcular probabilidades; a equação de Schrödinger determina os estados e energias no domínio não relativístico.

Os números quânticos descrevem energia, momento angular orbital, componente angular e spin; Pauli limita a ocupação dos estados eletrônicos.

As transições entre estados explicam espectros, enquanto regras de seleção, estrutura fina, Lamb, hiperfina e efeitos de campos explicam detalhes adicionais.

Modelos de um elétron são exatos apenas dentro de suas hipóteses; átomos multieletrônicos exigem aproximações e cálculos mais complexos.

A mecânica quântica é confirmada por muitos experimentos e fundamenta tecnologias, mas interpretações filosóficas devem ser separadas dos resultados experimentais.

Nenhum capítulo único pode esgotar literalmente toda a ciência quântica, todos os modelos, todas as aplicações e todos os cálculos possíveis; cada sistema avançado exige hipóteses, métodos e dados específicos.

Fontes didáticas principais: OpenStax, Chemistry 2e, seção “Development of Quantum Theory”: https://openstax.org/books/chemistry-2e/pages/6-3-development-of-quantum-theory

Fonte didática para hidrogênio e espectros: OpenStax, University Physics Volume 3, “The Hydrogen Atom”: https://openstax.org/books/university-physics-volume-3/pages/8-1-the-hydrogen-atom

Fonte didática para espectros e regras de seleção: OpenStax, University Physics Volume 3, “Atomic Spectra and X-rays”: https://openstax.org/books/university-physics-volume-3/pages/8-5-atomic-spectra-and-x-rays

Dados de referência para níveis, linhas, probabilidades de transição e energias de ionização: NIST Atomic Spectra Database: https://www.nist.gov/pml/atomic-spectra-database

Compêndio do NIST sobre regras de seleção e linhas espectrais: https://www.nist.gov/pml/atomic-spectroscopy-compendium-basic-ideas-notation-data-and-formulas/atomic-spectroscopy

## Formalismo quântico além do átomo

### Estado quântico e princípio de superposição

A evolução de estados na mecânica quântica é linear. Se |ψ₁⟩ e |ψ₂⟩ são soluções possíveis para a mesma dinâmica, uma combinação c₁|ψ₁⟩ + c₂|ψ₂⟩ também é uma solução, depois de considerada a normalização necessária.

Uma superposição não é simplesmente ignorância clássica sobre qual estado o sistema já ocupa. Ela pode produzir interferência entre amplitudes, o que permite distingui-la de certas misturas estatísticas.

Por exemplo, num sistema com dois resultados de medida |0⟩ e |1⟩, o estado |ψ⟩ = α|0⟩ + β|1⟩ exige |α|² + |β|² = 1. Uma medida na base {|0⟩, |1⟩} retorna 0 com probabilidade |α|² e 1 com probabilidade |β|².

Uma medida diferente, numa base formada por combinações de |0⟩ e |1⟩, pode revelar a fase relativa entre α e β. É por isso que duas preparações com os mesmos valores de |α|² e |β|² podem diferir em um experimento de interferência.

### Matrizes de Pauli e um sistema de dois níveis

Um qubit ideal é um sistema quântico de dois níveis. Sua forma geral é |ψ⟩ = α|0⟩ + β|1⟩, com normalização |α|² + |β|² = 1.

As matrizes de Pauli são σₓ = [[0,1],[1,0]], σᵧ = [[0,−i],[i,0]] e σ_z = [[1,0],[0,−1]]. Elas são fundamentais para representar observáveis de spin 1/2 em direções x, y e z.

O operador de spin numa direção cartesiana é Sᵢ = (ℏ/2)σᵢ. A medida de uma componente de spin de um elétron retorna +ℏ/2 ou −ℏ/2, nos casos ideais correspondentes.

As matrizes de Pauli não comutam entre si; por exemplo, [σₓ,σᵧ] = 2iσ_z. Essa álgebra matemática está por trás das relações de incerteza para componentes de spin.

### Operador densidade: estados puros e mistos

Um estado puro |ψ⟩ pode ser representado pelo operador densidade ρ = |ψ⟩⟨ψ|. Um conjunto estatístico de estados |ψᵢ⟩, preparado com probabilidades pᵢ, é representado por ρ = Σᵢ pᵢ|ψᵢ⟩⟨ψᵢ|.

O operador densidade satisfaz tr(ρ)=1 e é positivo. Para um estado puro, ρ²=ρ; para uma mistura genuína, em geral tr(ρ²)<1.

A probabilidade de um resultado associado ao projetor P é p = tr(ρP). Essa formulação permite tratar sistemas abertos, incerteza sobre a preparação e partes de sistemas emaranhados.

Um sistema composto pode estar num estado puro global, mas uma de suas partes pode ser descrita por um estado misto se ignorarmos a outra parte. Isso acontece, por exemplo, quando duas partículas estão emaranhadas.

### Evolução unitária e conservação da probabilidade

Quando um sistema isolado evolui sob um Hamiltoniano independente do tempo, o estado evolui por um operador unitário U(t)=exp(−iĤt/ℏ). A unitariedade conserva a norma do estado e, portanto, a probabilidade total.

A equação de Schrödinger dependente do tempo é iℏ∂|ψ⟩/∂t = Ĥ|ψ⟩. A conservação da probabilidade pode ser expressa por uma equação de continuidade entre densidade e corrente de probabilidade.

A conservação de energia ocorre quando o Hamiltoniano não depende explicitamente do tempo, sob as condições do sistema. Uma interação externa variável no tempo pode transferir energia para dentro ou para fora do sistema definido.

## A dupla fenda: interferência de partículas individuais

No experimento da dupla fenda, partículas como fótons ou elétrons são enviadas para uma barreira com duas aberturas e depois detectadas numa tela.

Se as duas aberturas estão acessíveis e não há informação de caminho que destrua a coerência, as amplitudes associadas aos dois caminhos se combinam. A previsão é P(x)=|ψ₁(x)+ψ₂(x)|².

Expandindo o módulo ao quadrado, P(x)=|ψ₁|²+|ψ₂|²+2 Re[ψ₁*ψ₂]. O último termo é o termo de interferência, que pode aumentar ou diminuir a probabilidade em diferentes posições.

A tela registra impactos localizados. Com muitos eventos, o padrão acumulado pode revelar franjas de interferência, embora cada evento individual apareça como um ponto detectado.

Se uma medida física permite distinguir por qual fenda a partícula passou, a interação pode emaranhar o caminho com o detector e reduzir ou eliminar a interferência observável. Não é necessário que uma pessoa olhe para o detector; a correlação física com o ambiente é o que importa.

A expressão “a partícula passa pelas duas fendas” deve ser entendida com cuidado: o formalismo atribui amplitudes coerentes aos caminhos possíveis, mas a interpretação ontológica depende da interpretação da teoria. O resultado experimental robusto é a interferência prevista pelas amplitudes.

Para ondas clássicas coerentes, a diferença de caminho determina franjas construtivas e destrutivas. Para partículas quânticas, o cálculo correto usa amplitudes e a regra de Born, reproduzindo o padrão sob as condições apropriadas.

A dupla fenda mostra que a teoria não pode ser reduzida a uma partícula clássica com um caminho sempre definido e ignorado pelo observador, sem perder as previsões de interferência.

### Exemplo de interferência de duas amplitudes

Suponha, num ponto específico da tela, duas amplitudes iguais e em fase, ψ₁=a e ψ₂=a, com a real. A amplitude total é 2a e a densidade de probabilidade proporcional é 4a².

Se forem iguais em oposição de fase, ψ₁=a e ψ₂=−a, a soma é zero e a probabilidade nesse ponto é zero para o modelo ideal coerente.

Se o sistema for uma mistura incoerente de alternativas, o termo cruzado médio desaparece e a probabilidade é a soma das probabilidades individuais. A diferença entre esses casos é testável experimentalmente.

A interferência não viola a conservação de energia; ela redistribui a probabilidade espacial dos resultados possíveis.

## Sistemas-modelo: poço de potencial infinito

O poço infinito unidimensional é um modelo em que uma partícula está confinada ao intervalo 0<x<L e o potencial é infinito fora dele. Esse é um ideal matemático; paredes reais têm altura finita e os elétrons podem penetrar uma distância não nula no material de confinamento.

Dentro do poço, toma-se U(x)=0. A equação estacionária de Schrödinger é −(ℏ²/2m)d²ψ/dx²=Eψ, com condições ψ(0)=ψ(L)=0.

As soluções normalizadas são ψₙ(x)=√(2/L) sin(nπx/L), para n=1,2,3,..., e as energias permitidas são Eₙ=n²π²ℏ²/(2mL²).

A energia não pode ser zero nesse modelo porque a função de onda não nula precisa obedecer às condições de contorno. O estado n=1 é o estado fundamental e possui energia positiva.

O comprimento L menor produz níveis mais afastados, porque Eₙ é proporcional a 1/L². Isso ajuda a explicar por que o confinamento em dimensões nanométricas pode alterar significativamente as energias eletrônicas.

### Exemplo resolvido: elétron num poço de 1,0 nm

Para um elétron de massa mₑ num poço infinito com L=1,0 nm, a energia fundamental é E₁=π²ℏ²/(2mₑL²). Substituindo os valores físicos, obtém-se aproximadamente E₁≈0,376 eV.

O segundo nível vale E₂=4E₁≈1,50 eV. A diferença entre os níveis é E₂−E₁=3E₁≈1,13 eV.

Esse resultado é apenas o exemplo do poço infinito ideal; não deve ser tomado como previsão exata de um nanomaterial real sem modelar a barreira, a geometria, as interações eletrônicas e os materiais.

## Poços finitos, estados ligados e espalhamento

Num poço finito, a energia potencial fora da região de confinamento tem valor elevado, mas não infinito. Para energias abaixo da barreira externa, a função de onda pode oscilar dentro do poço e decair exponencialmente fora dele.

A função de onda e sua derivada primeira precisam satisfazer as condições de continuidade nos pontos onde o potencial tem degraus finitos. Essas condições selecionam um conjunto discreto de estados ligados.

O número de estados ligados depende da largura e da profundidade do poço e da massa da partícula. Um poço suficientemente raso pode ter poucos estados ligados; em certos casos unidimensionais atrativos, até uma atração muito fraca pode sustentar um estado ligado, conforme a forma exata do potencial.

Quando a energia fica acima dos limiares de ligação, podem existir estados de espalhamento com energia contínua. A onda incidente pode ser refletida e transmitida, mesmo que a energia esteja acima de uma barreira.

Uma ressonância ocorre quando a probabilidade de transmissão ou a resposta do sistema aumenta fortemente numa faixa de energia por causa da estrutura do potencial e das fases da função de onda.

O estudo dos poços e do espalhamento prepara o caminho para compreender elétrons em materiais, estados nucleares, pontos quânticos e ressonâncias em colisões.

## Oscilador harmônico quântico

O oscilador harmônico é um dos modelos mais importantes da física, porque muitos potenciais perto de um mínimo estável podem ser aproximados por uma parábola.

O potencial é U(x)=½mω²x², em que m é a massa, ω é a frequência angular e x é a posição medida a partir do mínimo do potencial.

O Hamiltoniano é Ĥ=p̂²/(2m)+½mω²x̂². A solução quântica fornece energias Eₙ=(n+½)ℏω, com n=0,1,2,...

Ao contrário do oscilador clássico, o oscilador quântico possui energia mínima não nula, E₀=½ℏω, chamada energia de ponto zero.

A energia de ponto zero não significa que o sistema esteja fornecendo energia gratuita; é a energia do estado fundamental dentro do modelo e das condições físicas especificadas.

A frequência de transição entre níveis adjacentes no modelo ideal é ω, e a diferença de energia é ℏω. Em moléculas reais, a aproximação harmônica falha para grandes deslocamentos, e a anarmonicidade produz correções.

O oscilador harmônico aparece no estudo de vibrações moleculares, modos normais de sólidos, campos quânticos e estados de luz, embora cada aplicação tenha suas próprias variáveis e condições.

### Operadores de criação e aniquilação

Definem-se operadores a = √(mω/(2ℏ)) x̂ + i p̂/√(2mℏω) e a† = √(mω/(2ℏ)) x̂ − i p̂/√(2mℏω).

Eles satisfazem [a,a†]=1. O Hamiltoniano pode ser escrito Ĥ=ℏω(a†a+½), o que permite encontrar os níveis de energia sem resolver a equação diferencial diretamente.

O operador a reduz o número quântico por uma unidade e a† o aumenta, com fatores de normalização apropriados. Esses operadores são chamados de aniquilação e criação no problema do oscilador; a nomenclatura também surge na teoria quântica de campos, onde o contexto físico é diferente.

O estado fundamental satisfaz a|0⟩=0. A escada de estados é construída aplicando a† repetidamente ao estado fundamental e normalizando cada resultado.

## Tunelamento quântico e barreiras

Para uma partícula clássica com energia E abaixo da altura de uma barreira U₀, a partícula não atravessaria a região da barreira se essa fosse uma proibição energética absoluta do modelo clássico.

Na mecânica quântica, a função de onda numa região com U(x)>E pode decair exponencialmente, mas não precisa se anular imediatamente na fronteira. Para uma barreira finita, pode haver amplitude transmitida do outro lado.

Numa barreira retangular de altura U₀ e largura L, com E<U₀, define-se κ=√[2m(U₀−E)]/ℏ. A transmissão diminui aproximadamente como exp(−2κL) numa barreira suficientemente opaca, com fatores de prefator que dependem do problema.

A exponencial mostra por que aumentar a largura ou a altura da barreira pode reduzir enormemente a probabilidade de transmissão. Uma fórmula aproximada não substitui a solução exata quando a barreira não é opaca ou quando o prefator importa.

O tunelamento participa do decaimento alfa, de alguns mecanismos de reações nucleares e do funcionamento de dispositivos como o microscópio de tunelamento por varredura.

No microscópio de tunelamento, a corrente depende fortemente da distância entre uma ponta condutora e a superfície, permitindo mapear a estrutura eletrônica e topográfica com resolução atômica sob condições apropriadas.

Tunelamento não significa que a partícula tenha energia acima da barreira enquanto a atravessa, nem que toda barreira seja atravessada com grande probabilidade; a previsão depende da solução de onda completa e da conservação de energia do sistema total.

## Momento angular e adição de spins

O momento angular orbital e o spin são observáveis vetoriais cujas componentes não comutam todas entre si. Por isso, podem-se especificar simultaneamente o quadrado do momento angular e uma componente escolhida, como z, mas não todas as componentes com valores exatos.

Para dois spins de 1/2, a adição quântica permite um estado total com S=1, chamado triplete, e um estado com S=0, chamado singlete.

Os estados tripletes podem ser escritos |1,1⟩=|↑↑⟩, |1,0⟩=(|↑↓⟩+|↓↑⟩)/√2 e |1,−1⟩=|↓↓⟩.

O estado singlete é |0,0⟩=(|↑↓⟩−|↓↑⟩)/√2. Ele é emaranhado: não pode ser escrito como produto de um estado puro de cada partícula separadamente.

A diferença entre o sinal positivo do triplete de projeção zero e o sinal negativo do singlete é física e afeta as simetrias e as correlações de spin.

Os coeficientes usados para transformar entre bases de momento angular são chamados coeficientes de Clebsch–Gordan. Eles surgem ao decompor o produto de representações de momento angular em estados de momento angular total.

A soma dos momentos angulares aparece em átomos, moléculas, núcleos e colisões; a forma correta de acoplamento depende das escalas de energia e das interações dominantes.

## Emaranhamento quântico e desigualdades de Bell

Um estado emaranhado de dois sistemas não pode ser descrito como um estado puro independente de cada parte. As medições em cada parte podem apresentar correlações que não são reproduzidas por certas teorias de variáveis ocultas locais.

Para dois spins de 1/2 no estado singlete, a teoria prevê correlações entre medidas de spin que dependem do ângulo entre os eixos de medição. Os resultados individuais podem ser imprevisíveis, mas a correlação estatística segue a regra quântica.

John Bell demonstrou que teorias com variáveis ocultas locais obedecem a restrições matemáticas nas correlações. Em testes do tipo CHSH, uma combinação de quatro correlações satisfaz |S|≤2 para modelos locais com as hipóteses adequadas.

A mecânica quântica prevê que, em configurações apropriadas, |S| pode chegar a 2√2, excedendo o limite local clássico. Experimentos com pares de fótons observaram violações de desigualdades de Bell; o Nobel de Física de 2022 reconheceu experimentos fundamentais de Alain Aspect, John Clauser e Anton Zeilinger nesta área.

Violar a desigualdade de Bell não permite enviar mensagens mais rápido que a luz. Os resultados locais, considerados isoladamente, não dão ao experimentador controle arbitrário para codificar uma mensagem; as correlações só se tornam visíveis ao comparar registros por um canal clássico.

Teleportação quântica é a transferência de um estado quântico usando emaranhamento compartilhado e comunicação clássica. Ela não transporta matéria instantaneamente nem permite comunicação superluminal; a comunicação clássica é necessária para reconstruir o estado.

Emaranhamento é um recurso importante em informação quântica, mas sua utilidade depende do estado, do protocolo, do ruído e da tarefa concreta.

## Aproximações em mecânica quântica

Muitos problemas reais não têm solução exata simples. As aproximações precisam ser julgadas comparando suas previsões com experimentos, resultados mais precisos ou limites conhecidos.

Na teoria de perturbações, escreve-se Ĥ=Ĥ₀+λV, em que Ĥ₀ é um problema resolúvel e V é uma perturbação que se supõe suficientemente pequena.

Para um estado não degenerado, a correção de primeira ordem da energia é Eₙ⁽¹⁾=⟨n⁽⁰⁾|V|n⁽⁰⁾⟩. Se existe degenerescência, é necessário diagonalizar a perturbação dentro do subespaço degenerado antes de usar a fórmula de não degenerescência.

O método variacional escolhe uma função de tentativa normalizável. A energia média E[ψ]=⟨ψ|Ĥ|ψ⟩/⟨ψ|ψ⟩ fornece, para o Hamiltoniano apropriado, um limite superior à energia do estado fundamental.

A aproximação de Born–Oppenheimer separa parcialmente o movimento dos núcleos e dos elétrons porque os núcleos são muito mais pesados; ela é amplamente usada em química molecular, mas pode falhar perto de cruzamentos de superfícies de energia e em dinâmica não adiabática.

Hartree–Fock aproxima a função de onda eletrônica por um determinante de Slater de orbitais, respeitando a antissimetria e incluindo troca, mas não descreve toda a correlação eletrônica.

CI mistura configurações eletrônicas, coupled cluster organiza certas excitações de forma sistemática e DFT calcula propriedades a partir da densidade. Cada método tem custo computacional, convergência e erros característicos.

WKB é uma aproximação semiclássica útil quando o potencial varia lentamente na escala da onda, exceto perto de pontos de retorno onde é preciso tratar o comportamento com cuidado.

A escolha do método deve responder à questão científica. Um modelo simples pode ser superior para ensinar a estrutura do fenômeno, enquanto uma pesquisa de alta precisão requer correções e validação.

## Átomos com muitos elétrons e estrutura de materiais

Para N elétrons, o estado total depende das coordenadas espaciais e de spin de todos eles. A antissimetria de férmions idênticos impõe restrições que não aparecem num modelo de partículas distinguíveis.

O princípio de exclusão determina a ocupação dos estados; a repulsão coulombiana e a correlação eletrônica determinam como os elétrons se organizam além do preenchimento elementar de orbitais.

Em moléculas, orbitais moleculares podem estender-se sobre mais de um núcleo. Orbitais ligantes e antiligantes podem resultar da combinação de amplitudes, com alterações de energia que ajudam a explicar a estabilidade de ligações.

Em sólidos, a repetição de átomos e a interação entre muitos elétrons podem produzir bandas de energia. A presença de estados disponíveis próximos da energia relevante, a estrutura de bandas, os defeitos e as interações determinam a condução.

Semicondutores possuem estrutura de bandas que pode ser controlada por dopagem, temperatura, luz e campos elétricos, fundamento de diodos, transistores e células solares.

Fenômenos coletivos como magnetismo, supercondutividade e transições de fase quânticas exigem abordagens de muitos corpos e, frequentemente, conceitos emergentes que não se reduzem à descrição isolada de um átomo.

## Teoria quântica de campos e partículas

A mecânica quântica não relativística descreve muito bem muitos sistemas atômicos, mas a relatividade especial e a possibilidade de criação e aniquilação de partículas exigem um formalismo mais geral.

Na teoria quântica de campos, os campos são objetos fundamentais e as partículas são interpretadas como excitações quantizadas desses campos. O estado do campo pode conter diferentes números de partículas.

Operadores de criação e aniquilação mudam o número de excitações de um modo. Para bósons, as relações de comutação e a estatística são diferentes das relações de anticomutação dos férmions.

A equação de Dirac descreve férmions relativísticos de spin 1/2; a teoria também conduz à previsão de antipartículas, observadas experimentalmente.

A eletrodinâmica quântica (QED) descreve a interação dos elétrons, pósitrons e fótons e foi testada com precisão extraordinária em diversas quantidades.

O Modelo Padrão reúne a interação eletromagnética, a interação fraca e a interação forte numa estrutura de teoria quântica de campos, mas não inclui uma teoria quântica completa da gravidade.

O cálculo de processos em QFT pode envolver diagramas de Feynman e integrais divergentes. A renormalização reorganiza os parâmetros matemáticos para que previsões físicas sejam finitas e comparáveis a observações em uma teoria bem definida.

Diagramas de Feynman são uma forma de organizar termos de uma expansão perturbativa; não devem ser interpretados literalmente como fotografias das trajetórias de partículas minúsculas.

A teoria quântica de campos não afirma que qualquer coisa surge do nada sem regras. Processos precisam respeitar as leis de conservação e os limites da teoria usada.

A gravidade quântica permanece um problema aberto: há propostas teóricas importantes, mas não existe uma única descrição plenamente confirmada por experimentos que una a relatividade geral e a teoria quântica em todas as condições.

## Revisão de conceitos com exercícios resolvidos

### Exercício: probabilidade de medir um estado

Considere |ψ⟩=(√3/2)|0⟩+(1/2)|1⟩. A normalização é |√3/2|²+|1/2|²=3/4+1/4=1.

Se a medida distingue a base {|0⟩,|1⟩}, a probabilidade do resultado 0 é 3/4 e a do resultado 1 é 1/4. As probabilidades somam um, conforme exigido.

### Exercício: nível e comprimento de onda

No hidrogênio, uma transição de n=3 para n=2 libera aproximadamente 1,889 eV, como mostrado na seção do átomo. Usando λ(nm)=1239,84/E(eV), obtém-se cerca de 656,3 nm.

A energia positiva do fóton corresponde à magnitude da energia perdida pelo átomo; o sinal de ΔE do átomo é negativo para emissão.

### Exercício: energia do poço infinito

Para um elétron confinado num poço ideal de 1,0 nm, E₁≈0,376 eV e E₂=4E₁≈1,50 eV. A diferença E₂−E₁≈1,13 eV é a energia de um fóton capaz de induzir a transição no modelo ideal, se o acoplamento e as regras de seleção do sistema permitirem.

### Exercício: distinguir superposição de mistura

Um estado puro (|0⟩+|1⟩)/√2 e uma mistura estatística em que 0 e 1 aparecem cada um com probabilidade 1/2 dão os mesmos resultados na medida direta da base {|0⟩,|1⟩}. Porém, podem dar resultados diferentes numa medida de uma observável numa base de superposições, porque o estado puro possui coerência relativa e a mistura não.

### Exercício: Bell e causalidade

Uma violação de uma desigualdade de Bell exclui uma classe de modelos de variáveis ocultas locais sob as hipóteses do teste; não prova que seja possível transmitir informação mais rápido que a luz. O teorema de não comunicação e as probabilidades locais impedem essa conclusão.

## Limites, correções e como ler afirmações quânticas

Toda equação deve ser acompanhada da identificação do sistema, das condições iniciais, das hipóteses e do domínio de validade. Uma fórmula hidrogenoide não pode ser usada diretamente num átomo com muitos elétrons sem um modelo adicional.

A palavra “medida” tem significado operacional na física: deve-se especificar qual observável o aparelho registra e qual distribuição de resultados a teoria prevê.

A superposição não significa que todas as possibilidades sejam simultaneamente observáveis como resultados clássicos; uma medição individual retorna um resultado e as amplitudes controlam as estatísticas.

O emaranhamento não permite comunicação instantânea controlável. Interpretações da mecânica quântica diferem sobre o significado subjacente, mas devem reproduzir as previsões que já foram testadas.

As previsões quânticas não dão suporte científico a alegações de que pensamentos, desejos ou consciência possam escolher livremente os resultados físicos sem um mecanismo testável.

Quando uma afirmação usa “quântico” para prometer capacidades extraordinárias, é necessário perguntar qual equação, qual previsão quantitativa, qual experimento controlado e qual replicação independente a sustentam.

## Referências para estudo aprofundado

OpenStax, University Physics Volume 3 — equação de Schrödinger, poço infinito, oscilador harmônico e tunelamento: https://openstax.org/books/university-physics-volume-3/pages/7-3-the-schrodinger-equation ; https://openstax.org/books/university-physics-volume-3/pages/7-5-the-quantum-harmonic-oscillator ; https://openstax.org/books/university-physics-volume-3/pages/7-6-the-quantum-tunneling-of-particles-through-potential-barriers

OpenStax, dualidade onda-partícula e experiência da dupla fenda com elétrons: https://openstax.org/books/university-physics-volume-3/pages/6-6-wave-particle-duality

MIT OpenCourseWare, 8.04 Quantum Physics I: notas de aula e problemas sobre superposição, números complexos, função de onda, poços, oscilador, momento angular e átomo de hidrogênio: https://ocw.mit.edu/courses/8-04-quantum-physics-i-spring-2016/

Nobel Prize, Prêmio de Física de 2022 e experimentos com fótons emaranhados e desigualdades de Bell: https://www.nobelprize.org/prizes/physics/2022/press-release/

NIST Atomic Spectra Database, dados de níveis de energia, linhas e transições atômicas: https://www.nist.gov/pml/atomic-spectra-database
