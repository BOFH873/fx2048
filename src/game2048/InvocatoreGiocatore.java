package game2048;

import giocatoreAutomatico.*;
import javafx.application.Platform;

/**
 *
 * InvocatoreGiocatore si occupa di creare il thread che chiama ripetutamente il
 * GiocatoreAutomatico.
 * 
 * 
 * @author BOFH873
 */
public class InvocatoreGiocatore implements Runnable {

    /**
     * Ritardo fra una mossa e l'altra (in millisecondi).
     */
    private long periodo;
    
    private final GameManager gm;
    private final Thread t;
    private final GiocatoreAutomatico ga;
    
    /**
     * run() verra' invocata all'avvio del thread ed effettuera' periodicamente
     * delle mosse (invocando GameManager.move()) seguendo le direzioni indicate
     * volta per volta da GiocatoreAutomatico.prossimaMossa().
     */
    public void run()
    {    
        while (!gm.isLayerOn())
        {
            Griglia grid = gm.getGriglia();
            Direction d = Direction.convertiDirezione(ga.prossimaMossa(grid));

//                System.out.println("GRIGLIA: " + grid);
//                System.out.println("DIREZIONE " + d);

            Platform.runLater(() -> { gm.move(d);});

            try
            {
                Thread.sleep(getPeriodo());
            }
            catch (InterruptedException e)
            {
                // Non essendoci altri thread in grado di interrompere questo, non
                // dovremmo preoccuparci di gestire questa eccezione.
            }
        }
    }
    /**
     * Costruttore per InvocatoreGiocatore, inizializza gli attributi principali
     * della classe e tenta di recuperare il GiocatoreAutomatico.
     * 
     * @param gm la classe GameManager che sta istanziando l'oggetto.
     * @param periodo il periodo (in millisecondi) fra una mossa e l'altra.
     * @throws Exception - lanciata nel caso in cui getGiocatoreAutomatico() dovesse
     *                      fallire nella ricerca della classe.
     */
    public InvocatoreGiocatore(GameManager gm, long periodo) throws Exception
    {
        this.gm = gm;
        this.periodo = periodo;
        this.t = new Thread(this);
        this.ga = GiocatoreAutomatico.getGiocatoreAutomatico();
    }
    
    /**
     * Avvia il Thread che invoca il GiocatoreAutomatico.
     */
    public void start()
    {
        getThread().start();
    }

    /**
     * Getter per il thread.
     * 
     * @return il Thread attuale del GA
     */
    public Thread getThread() {
        return t;
    }

    /**
     * Getter per il periodo.
     * 
     * @return il periodo attuale di questo invocatore.
     */
    public long getPeriodo() {
        return periodo;
    }

    /**
     * Setter per il periodo.
     * 
     * @param periodo il nuovo valore del periodo.
     */
    public void setPeriodo(long periodo) {
        this.periodo = periodo;
    }
    
}
