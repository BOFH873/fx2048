package giocatoreAutomatico.player;

import game2048.Direction;
import game2048.Location;
import giocatoreAutomatico.Griglia;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Andrea
 */
public class MyGiocatoreAutomatico3 {
    
    private final double[] probability = {0.9, 0.1};
    private final int[] mosse = {2, 4};
    private final int MOVES_COUNT = 2;
    private final int GRIGLIA = 4;
    private int profondita = 3;
      
    int gradienteGriglia[][][] = {
        {
                { 3,  2,  1,  0},
                { 2,  1,  0, -1},
                { 1,  0, -1, -2},
                { 0, -1, -2, -3}
        },
        {
                {  0,  1,  2, 3},
                { -1,  0,  1, 2},
                { -2, -1,  0, 1},
                { -3, -2, -1, 0}
        }
    };      
    
    public int prossimaMossa (Griglia g) {
    
        double best_score = 0;
	Direction best_dir = null;
        int nextMove=0;       

	for(Direction direction: Direction.values()){
            Griglia computer_grid = new NewGrid(g);
            move(computer_grid, direction);

            if (computer_grid == g) // No change due to movement
            {
                continue;
            }

            double computer_score = computer_move(computer_grid, 2*(profondita-1));

            if (computer_score >= best_score){ // Equality : Forces a move even when deadend is expected
                best_score = computer_score;
                best_dir = direction;
            }
	}
        
        switch (best_dir) {
            case UP:    nextMove=0; break;
            case DOWN:  nextMove=2; break;
            case RIGHT: nextMove=1; break;
            case LEFT:  nextMove=3; break;
            default:                break;
        }
	return nextMove;
}
    private double computer_move(Griglia computer_grid, int profondita) {
    	double total_score = 0;
	double total_weight =0;

	// Pruning trackers
	HashMap temp = null;
	for(int x = 0; x < 4; x++)
	{
            for(int y = 0; y < 4; y++)
            {
                if (computer_grid.get(new Location(x, y)) == -1)
                {
                    for(int i = 0; i < MOVES_COUNT; i++)
                    {
                        Griglia player_grid = new NewGrid(computer_grid);
                        player_grid.put(new Location(x, y), mosse[i]);
                        double score = player_move(player_grid, temp, profondita - 1);
                        total_score += probability[i] * score; // Weighted average. This is the essence of expectimax.
                        total_weight += probability[i]; // Weighted average. This is the essence of expectimax.
                    }
                }
            }
	}
	return total_weight == 0 ? 0 : total_score / total_weight;
    }
    
    private double player_move(Griglia g, HashMap<Griglia, Double> map, int profondita){
            if (profondita == 0) // End of branch
        {		
                return has_move(g) ? evaluate_heuristic(g) : 0; // has_move(grid) Penalizes dead end
        }

        double best_score = 0;

        for(Direction direction: Direction.values()){
                Griglia computer_grid = new NewGrid(g);
                move(computer_grid, direction);

                if (computer_grid == g)  // No change due to movement
                {
                        continue; // Skip to next direction
                }

                double computer_score;

                // Pruning                
                double score=map.get(computer_grid);
                if (score != 0)
                {
                    computer_score = score;
                }
                else
                {
                    computer_score = computer_move(computer_grid, profondita - 1);
                    map.put(g, computer_score);
                }

                if (computer_score > best_score){
                    best_score = computer_score;
                }
        }
        return best_score;
    }

    private boolean has_move(Griglia g) {
	for (int x = 0; x < 4; x++)
	{
            for (int y = 0; y < 4; y++)
            {
                if (g.get(new Location (x, y)) == 0)
                        return true;
                if (x < 3 && Objects.equals(g.get(new Location (x, y)), g.get(new Location (x+1, y))))
                        return true;
                if (y < 3 && Objects.equals(g.get(new Location (x, y)), g.get(new Location (x, y+1))))
                        return true;
            }
	}
	return false;
    }

    private int evaluate_heuristic(Griglia g) {
        int best = 0;
	for (int i = 0; i < 2; i++){
            int s = 0;
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    s += gradienteGriglia[i][y][x] * g.get(new Location (x, y));
                }
            }
            s = Math.abs(s); // Hack for symmetry
            if (s > best)
            {
                    best = s;
            }
	}
	return best;
    }

    private void move(Griglia computer_grid, Direction direction) {
        switch (direction){
	case UP:
		shift_up(computer_grid);
		break;
	case RIGHT:
		shift_right(computer_grid);
		break;
	case DOWN:
		shift_down(computer_grid);
		break;
	case LEFT:
		shift_left(computer_grid);
		break;
	}
    }

    private void shift_up(Griglia computer_grid) {
        
        Location[] shiftingUp=null;
        
        for (int i=0; i<4; i++) {
            shiftingUp[i] = new Location(i, 3);
            moving_grid(shiftingUp[i], Direction.UP, computer_grid);
        }
    }
    
    private void moving_grid(Location current, Direction dir, Griglia g) {
        Location next = current.offset(dir);

        for (int i = 0; i < GRIGLIA; i++) 
        {
            if (!next.isValidFor(GRIGLIA)) { break; }
           
            //se la mossa e' possibile per seguente griglia e se la posizione corrente è vuota (=-1)
            if (next.isValidFor(GRIGLIA) && g.get(current).equals(-1)) 
            {
                g.put(next, g.get(current));
                g.put(current, -1); 
                current = next; 
                next = next.offset(dir);
            }   
        }
    }

    private void shift_right(Griglia computer_grid) {
        
        Location[] shiftingUp=null;
                
        for (int i=0; i<4; i++) {
            shiftingUp[i] = new Location(i, 3);
            moving_grid(shiftingUp[i], Direction.RIGHT, computer_grid);
        }
    }

    private void shift_down(Griglia computer_grid) {
        
        Location[] shiftingUp=null;
               
        for (int i=0; i<4; i++) {
            shiftingUp[i] = new Location(i, 3);
            moving_grid(shiftingUp[i], Direction.DOWN, computer_grid);
        }
    }

    private void shift_left(Griglia computer_grid) {
        
        Location[] shiftingUp=null;
                
        for (int i=0; i<4; i++) {
            shiftingUp[i] = new Location(i, 3);
            moving_grid(shiftingUp[i], Direction.LEFT, computer_grid);
        }
    }
    
    private class NewGrid extends HashMap<Location, Integer> implements Griglia 
    {
        public NewGrid(Griglia grid) 
        {
            Set<Location> listLocation = grid.keySet();
            listLocation.stream().forEach((location) -> {
                super.put(location, grid.get(location));
            });
        }
    }
}