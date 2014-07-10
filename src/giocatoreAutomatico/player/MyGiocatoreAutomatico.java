package giocatoreAutomatico.player;

import giocatoreAutomatico.GiocatoreAutomatico;
import giocatoreAutomatico.Griglia;
import java.util.Random;

/**
 * @author Andrea
 * @date 06/07/2014
 * Questa classe si occupa di far scegliere la prossima mossa al giocatore automatico.
 */

public class MyGiocatoreAutomatico implements GiocatoreAutomatico {
        
        private static Random numeroCasuale = null;
        
        public MyGiocatoreAutomatico()
        {
            if (this.numeroCasuale == null)
            {
                this.numeroCasuale = new Random(System.currentTimeMillis());
            }
        }
        
        
        /***
         * @parame g Griglia passata per consentire la prossima mossa;
         * @parame numeroCasuale numero casuale per consentire la scelta;
         * @parame scelta numero che rappresenta la scelta del giocatore automatico;
         * @return scelta numero che rappresenta la scelta del giocatore automatico
         */
        @Override
        public int prossimaMossa(Griglia g){
            
            int scelta = this.numeroCasuale.nextInt(4);

            return scelta;
        }
}
