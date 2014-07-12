Fork basato sulla versione Javascript di 2048: https://github.com/gabrielecirulli/2048 

CHI SIAMO
===================
Gruppo 10

* Andrea Atzeni
* Claudia Cauli
* Annalisa Congiu
* Davide Salaris
* Michele Zaccheddu

LICENZA
===================
Il progetto è sotto licenza GPL3, per leggerla:
https://github.com/BOFH873/fx2048/blob/master/LICENSE

SCOPI DEL PROGETTO
===================
Questo progetto ha lo scopo di introdurre la modalità di gioco automatico nel
gioco 2048.

REALIZZAZIONE
===================
Essendo stato realizzato in gruppo, ci sono state diverse riunioni con tutti i
membri del gruppo di lavoro per decidere i punti chiave del progetto e la
suddivisione dei compiti.

IMPLEMENTAZIONE GIOCATORE AUTOMATICO:
===================
Per implementare l’interfaccia GiocatoreAutomatico in un primo momento abbiamo
pensato di basarci su una scelta “easy”, quale può essere la generazione di
numeri casuali (da 0 a 3) che imitassero le 4 direzioni possibili all'interno
della griglia creando la classe MyGiocatoreAutomatico.
Successivamente si è passati alla scelta di un qualche tipo di IA più
intelligente e maggiormente efficiente.
Questa scelta ha portato alla conoscenza di algoritmi di Minimax
(http://it.wikipedia.org/wiki/Minimax), passando per alcune delle sue evoluzioni
come per esempio l'Alfa Beta Pruning
(http://it.wikipedia.org/wiki/Potatura_alfa-beta) e l'Expectimax
(http://en.wikipedia.org/wiki/Expectiminimax_tree).
La scelta è ricaduta sull'Expectimax, basandoci sull'IA di Lee Yiyuan
(https://github.com/LeeYiyuan/2048ai) scritta in C++.
Questi algoritmi sono stati implementati nella classe MyGiocatoreAutomatico3
(attualmente in fase di sviluppo).


INVOCAZIONE GIOCATORE AUTOMATICO
===================
Per quanto riguarda il corretto funzionamento del giocatore automatico, merita
attenzione l'utilizzo di un thread apposito tramite la classe Thread
(java.lang.Thread).
Il Thread invoca periodicamente il metodo prossimaMossa() definito nell'
interfaccia GiocatoreAutomatico, fornendogli in input la disposizione attuale 
della griglia di gioco tramite una Griglia (anch'essa facente parte delle 
specifiche iniziali).
L'intervallo di tempo fra un'invocazione e l'altra (e quindi fra una mossa e l'
altra) è gestito tramite l'utilizzo di Thread.sleep(), e la durata della pausa 
viene stabilita al momento della costruzione di InvocatoreGiocatore. Esistono 
due periodi diversi preimpostati tramite costanti in GameManager: PERIODO e 
PERIODO_STATS, entrambi espressi in millisecondi. PERIODO rappresenta il ritardo
fra una mossa e l'altra quando si sceglie di avviare una partita singola giocata
dal GiocatoreAutomatico, mentre PERIODO_STATS viene utilizzato quando si
effettuano statistiche (per questo motivo è inferiore rispetto a PERIODO).
Tentare di utilizzare un periodo più breve di quello attuale in PERIODO_STATS
sarebbe inutile e controproducente, in quanto l'esecuzione di una mossa richiede
un tempo minimo perché siano completate le transizioni nell'interfaccia grafica.
Il Thread continua la sua esecuzione fino a quando la proprietà layerOnProperty 
è impostata a false. Questa scelta è stata fatta perché tale proprietà viene 
impostata in tutte le occasioni nelle quali il gioco si interrompe (partita 
persa, partita vinta, visualizzazione statistiche...).
L'attivazione del Thread di InvocatoreGiocatore è anch'essa legata all'
impostazione di layerOnProperty.
L'interazione fra il Thread di InvocatoreGiocatore e la UI di fx2048 è stata 
possibile grazie all'utilizzo di Platform.runLater(), metodo che consente di 
inviare del codice al thread di javafx in modo che venga eseguito dall'interno, 
dato che thread esterni a quello di javafx non possono effettuare modifiche alla
UI.


INTEGRAZIONE COL CODICE ESISTENTE
===================
Per la gestione del flusso di esecuzione sono state sfruttate le varie 
Properties già presenti, quali gameOverProperty, gameWonProperty e 
layerOnProperty.

IMPLEMENTAZIONE GUI:
===================
Tramite il metodo scegliGiocatore() all’interno della classe GameManager è stata
introdotta una GUI per la scelta della modalità di gioco e per il lancio di una 
finestra che consenta la visualizzazione delle statistiche del gioco.
Questa GUI compare all’avvio del gioco e al termine di ogni partita.
Si tiene traccia delle scelte fatte dall’utente tramite le proprietà booleane 
automaticPlayerProperty e statisticsVisualizationProperty.

FUNZIONI AGGIUNTIVE: STATISTICHE
===================
Abbiamo pensato di raccogliere alcuni dati sull’andamento delle partite giocate,
sia nella modalità giocatore umano che in modalità giocatore automatico, per 
valutare e quantificare le prestazioni dell’algoritmo usato per modellare 
l’automatismo. 
A tal fine abbiamo dichiarato tre variabili di tipo intero nella classe 
GameManager che contengono rispettivamente il numero delle mosse totali (la 
variabile maxMoves), il valore massimo (maxValue) e il punteggio massimo 
(maxScore) raggiunti. 
La variabile maxMoves è un semplice contatore incrementato ogni qual volta venga
effettuata una mossa valida sulla griglia. La variabile maxScore contiene il 
punteggio di gioco al momento della sua terminazione. La variabile maxValue 
immagazzina il valore intero massimo contenuto tra le tile della griglia alla 
fine del gioco, questo numero viene calcolato tramite una funzione getMaxValue()
che analizza ogni Tile della griglia corrente in sequenza per trovare quella 
contenente il valore più alto.
Queste variabili vengono aggiornate sia in caso di vittoria sia in caso di 
perdita e poi inserite, attraverso la manipolazione di una classe apposita 
Tripla, in una lista di statistiche.

Questi dati saranno poi visualizzabili attraverso una GUI creata utilizzando 
JAVA FX 2, al momento dell'apertura del gioco, attraverso l'utilizzo della 
classe Observable.
