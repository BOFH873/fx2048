package game2048;

import giocatoreAutomatico.*;

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
    private final int PERIODO_MOSSE = 1000;
    
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
        
        /**
         * Qualora non ci fosse nessuna partita in corso, le mosse non vengono 
         * ne calcolate ne eseguite
         */
        if (!gm.isGameOver())
        {
            Griglia grid = gm.getGriglia();
            Direction d = Direction.convertiDirezione(ga.prossimaMossa(grid));
            gm.move(d);

            try
            {
                Thread.sleep(PERIODO_MOSSE);
            }
            catch (InterruptedException e)
            {
                // Non essendoci altri thread in grado di interrompere questo, non
                // dovremmo preoccuparci di gestire questa eccezione.
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
}
