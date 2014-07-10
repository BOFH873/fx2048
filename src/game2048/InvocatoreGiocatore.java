package game2048;

import giocatoreAutomatico.*;
import javafx.application.Platform;

/**
 *
 * InvocatoreGiocatore si occupa di creare il thread che chiama ripetutamente il
 * GiocatoreAutomatico.
 * 
 * 
 * @author drgb
 */
public class InvocatoreGiocatore implements Runnable {

    /**
     * Ritardo fra una mossa e l'altra (in millisecondi).
     */
    private long periodo = 1000;
    
    private final GameManager gm;
    private final Thread t;
    private final GiocatoreAutomatico ga;
    
    /**
     * run() verra' invocata all'avvio del thread ed effettuera' periodicamente
     * delle mosse (invocando GameManager.move()) seguendo le direzioni indicate
     * volta per volta da GiocatoreAutomatico.prossimaMossa().
     */
    @Override
    public void run() {
        
        while (true)
        {
            /**
             * Qualora non ci fosse nessuna partita in corso, le mosse non vengono 
             * ne calcolate ne eseguite
             */
            if (!gm.isGameOver())
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
    }
 
    public InvocatoreGiocatore(GameManager gm) throws Exception
    {
        this.gm = gm;
        this.t = new Thread(this);
        this.ga = GiocatoreAutomatico.getGiocatoreAutomatico();
    }
    
    public void start()
    {
        getThread().start();
    }

    /**
     * @return the Thread
     */
    public Thread getThread() {
        return t;
    }

    /**
     * @return the periodo
     */
    public long getPeriodo() {
        return periodo;
    }

    /**
     * @param periodo the periodo to set
     */
    public void setPeriodo(long periodo) {
        this.periodo = periodo;
    }
    
}
