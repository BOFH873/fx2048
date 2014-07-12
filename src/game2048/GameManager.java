package game2048;

import giocatoreAutomatico.Griglia;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;

import giocatoreAutomatico.Griglia;
import javafx.geometry.Orientation;

/**
 *
 * @author bruno
 */
public class GameManager extends Group {

    private static final int FINAL_VALUE_TO_WIN = 2048;
    public static final int CELL_SIZE = 128;
    private static final int DEFAULT_GRID_SIZE = 4;
    private static final int BORDER_WIDTH = (14 + 2) / 2;
    // grid_width=4*cell_size + 2*cell_stroke/2d (14px css)+2*grid_stroke/2d (2 px css)
    private static final int GRID_WIDTH = CELL_SIZE * DEFAULT_GRID_SIZE + BORDER_WIDTH * 2;
    private static final int TOP_HEIGHT = 92;
    
    /**
     * Numero di simulazioni effettuate quando l'utente avvia le statistiche.
     */
    private static final int PLAYS_NUMBER = 3;
    
    /**
     * Periodo fra le mosse quando si usa il giocatore automatico in mod. partita singola.
     */
    private static final long PERIODO = 150;
    
    /**
     * Periodo fra le mosse quando si effettuano statistiche sul GA.
     */
    private static final long PERIODO_STATS = 50;

    private volatile boolean movingTiles = false;
    private final int gridSize;
    private final List<Integer> traversalX;
    private final List<Integer> traversalY;
    private final List<Location> locations = new ArrayList<>();
    private final Map<Location, Tile> gameGrid;
    private final BooleanProperty automaticPlayerProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty gameWonProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty gameOverProperty = new SimpleBooleanProperty(false);
    private final IntegerProperty gameScoreProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty gameMovePoints = new SimpleIntegerProperty(0);
    private final Set<Tile> mergedToBeRemoved = new HashSet<>();
    private final ParallelTransition parallelTransition = new ParallelTransition();
    private final BooleanProperty layerOnProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty statsOnProperty = new SimpleBooleanProperty(false);
    private final IntegerProperty statsNProperty = new SimpleIntegerProperty(0);
    
    private static ObservableList<Tripla> statistics = FXCollections.observableArrayList();
    
    // User Interface controls
    private final VBox vGame = new VBox(50);
    private final HBox hOvrLabel = new HBox();
    private final HBox hOvrButton = new HBox();
    private final VBox vButton = new VBox();

    private Group gridGroup = new Group();

    private HBox hBottom = new HBox();
    private HBox hTop = new HBox(0);
    private Label lblScore = new Label("0");
    private Label lblPoints = new Label();
    
    
    // Statistic's variables
    private int maxScore;
    private int maxValue;
    private int maxMoves;
    
    // Event Handlers
    private EventHandler keyH;
    private EventHandler swipeHUp;
    private EventHandler swipeHRight;
    private EventHandler swipeHDown;
    private EventHandler swipeHLeft;

    // Invocatore
    private InvocatoreGiocatore invocatore;
    
    public GameManager() {
        this(DEFAULT_GRID_SIZE);
    }

    public GameManager(int gridSize) {
        this.gameGrid = new HashMap<>();
        this.gridSize = gridSize;
        this.traversalX = IntStream.range(0, gridSize).boxed().collect(Collectors.toList());
        this.traversalY = IntStream.range(0, gridSize).boxed().collect(Collectors.toList());

        createScore();
        createGrid();
        scegliGiocatore();
        initGameProperties();

        initializeGrid();

        this.setManaged(false);
        
        // Stat.var inizialization
        this.maxScore = 0;
        this.maxValue = 0;
        this.maxMoves = 0;
    }
    
    public void move(Direction direction) {
        if (layerOnProperty.get()) {
            return;
        }

        synchronized (gameGrid) {
            if (movingTiles) {
                return;
            }
        }

        gameMovePoints.set(0);

        Collections.sort(traversalX, direction.getX() == 1 ? Collections.reverseOrder() : Integer::compareTo);
        Collections.sort(traversalY, direction.getY() == 1 ? Collections.reverseOrder() : Integer::compareTo);
        final int tilesWereMoved = traverseGrid((int x, int y) -> {
            Location thisloc = new Location(x, y);
            Tile tile = gameGrid.get(thisloc);
            if (tile == null) {
                return 0;
            }

            Location farthestLocation = findFarthestLocation(thisloc, direction); // farthest available location
            Location nextLocation = farthestLocation.offset(direction); // calculates to a possible merge
            Tile tileToBeMerged = nextLocation.isValidFor(gridSize) ? gameGrid.get(nextLocation) : null;

            if (tileToBeMerged != null && tileToBeMerged.getValue().equals(tile.getValue()) && !tileToBeMerged.isMerged()) {
                tileToBeMerged.merge(tile);

                this.maxMoves++;
                
                gameGrid.put(nextLocation, tileToBeMerged);
                gameGrid.replace(tile.getLocation(), null);

                parallelTransition.getChildren().add(animateExistingTile(tile, tileToBeMerged.getLocation()));
                parallelTransition.getChildren().add(hideTileToBeMerged(tile));
                mergedToBeRemoved.add(tile);

                gameMovePoints.set(gameMovePoints.get() + tileToBeMerged.getValue());
                gameScoreProperty.set(gameScoreProperty.get() + tileToBeMerged.getValue());

                if (tileToBeMerged.getValue() == FINAL_VALUE_TO_WIN && !statsOnProperty.get()) {
                    gameWonProperty.set(true);
                }
                return 1;
            } else if (farthestLocation.equals(tile.getLocation()) == false) {
                parallelTransition.getChildren().add(animateExistingTile(tile, farthestLocation));
           
                gameGrid.put(farthestLocation, tile);
                gameGrid.replace(tile.getLocation(), null);

                tile.setLocation(farthestLocation);

                return 1;
            }

            return 0;
        });

        if (gameMovePoints.get() > 0) {
            animateScore(gameMovePoints.getValue().toString()).play();
        }

        parallelTransition.setOnFinished(e -> {
            synchronized (gameGrid) {
                movingTiles = false;
            }

            gridGroup.getChildren().removeAll(mergedToBeRemoved);

            // game is over if there is no more moves
            Location randomAvailableLocation = findRandomAvailableLocation();
            if (randomAvailableLocation == null && !mergeMovementsAvailable()) {
                this.maxValue = maxValue();
                this.maxScore = gameScoreProperty.get();
                gameOverProperty.set(true);
            } else if (randomAvailableLocation != null && tilesWereMoved > 0) {
                addAndAnimateRandomTile(randomAvailableLocation);
            }

            mergedToBeRemoved.clear();

            // reset merged after each movement
            gameGrid.values().stream().filter(Objects::nonNull).forEach(Tile::clearMerge);
        });

        synchronized (gameGrid) {
            movingTiles = true;
        }

        parallelTransition.play();
        parallelTransition.getChildren().clear();
    }

    private Location findFarthestLocation(Location location, Direction direction) {
        Location farthest;

        do {
            farthest = location;
            location = farthest.offset(direction);
        } while (location.isValidFor(gridSize) && gameGrid.get(location) == null);

        return farthest;
    }

    private int traverseGrid(IntBinaryOperator func) {
        AtomicInteger at = new AtomicInteger();
        traversalX.forEach(t_x -> {
            traversalY.forEach(t_y -> {
                at.addAndGet(func.applyAsInt(t_x, t_y));
            });
        });

        return at.get();
    }

    private boolean mergeMovementsAvailable() {
        final SimpleBooleanProperty foundMergeableTile = new SimpleBooleanProperty(false);

        Stream.of(Direction.UP, Direction.LEFT).parallel().forEach(direction -> {
            int mergeableFound = traverseGrid((x, y) -> {
                Location thisloc = new Location(x, y);
                Tile tile = gameGrid.get(thisloc);

                if (tile != null) {
                    Location nextLocation = thisloc.offset(direction); // calculates to a possible merge
                    if (nextLocation.isValidFor(gridSize)) {
                        Tile tileToBeMerged = gameGrid.get(nextLocation);
                        if (tile.isMergeable(tileToBeMerged)) {
                            return 1;
                        }
                    }
                }

                return 0;
            });

            if (mergeableFound > 0) {
                foundMergeableTile.set(true);
            }
        });

        return foundMergeableTile.getValue();
    }

    private void createScore() {
        Label lblTitle = new Label("2048");
        lblTitle.getStyleClass().add("title");
        Label lblSubtitle = new Label("FX");
        lblSubtitle.getStyleClass().add("subtitle");
        HBox hFill = new HBox();
        HBox.setHgrow(hFill, Priority.ALWAYS);
        hFill.setAlignment(Pos.CENTER);
        VBox vScore = new VBox();
        vScore.setAlignment(Pos.CENTER);
        vScore.getStyleClass().add("vbox");
        Label lblTit = new Label("SCORE");
        lblTit.getStyleClass().add("titScore");
        lblScore.getStyleClass().add("score");
        lblScore.textProperty().bind(gameScoreProperty.asString());
        vScore.getChildren().addAll(lblTit, lblScore);

        hTop.getChildren().addAll(lblTitle, lblSubtitle, hFill, vScore);
        hTop.setMinSize(GRID_WIDTH, TOP_HEIGHT);
        hTop.setPrefSize(GRID_WIDTH, TOP_HEIGHT);
        hTop.setMaxSize(GRID_WIDTH, TOP_HEIGHT);

        vGame.getChildren().add(hTop);
        getChildren().add(vGame);

        lblPoints.getStyleClass().add("points");

        getChildren().add(lblPoints);
    }

    private void createGrid() {
        final double arcSize = CELL_SIZE / 6d;

        IntStream.range(0, gridSize)
                .mapToObj(i -> IntStream.range(0, gridSize).mapToObj(j -> {
                    Location loc = new Location(i, j);
                    locations.add(loc);

                    Rectangle rect2 = new Rectangle(i * CELL_SIZE, j * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    rect2.setArcHeight(arcSize);
                    rect2.setArcWidth(arcSize);
                    rect2.getStyleClass().add("grid-cell");
                    return rect2;
                }))
                .flatMap(s -> s)
                .forEach(gridGroup.getChildren()::add);

        gridGroup.getStyleClass().add("grid");
        gridGroup.setManaged(false);
        gridGroup.setLayoutX(BORDER_WIDTH);
        gridGroup.setLayoutY(BORDER_WIDTH);

        hBottom.getStyleClass().add("backGrid");
        hBottom.setMinSize(GRID_WIDTH, GRID_WIDTH);
        hBottom.setPrefSize(GRID_WIDTH, GRID_WIDTH);
        hBottom.setMaxSize(GRID_WIDTH, GRID_WIDTH);

        hBottom.getChildren().add(gridGroup);

        vGame.getChildren().add(hBottom);
    }

    private void initGameProperties() {
        gameOverProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue && !statsOnProperty.get()) {
                layerOnProperty.set(true);
                hOvrLabel.getStyleClass().setAll("over");
                hOvrLabel.setMinSize(GRID_WIDTH, GRID_WIDTH);
                Label lblOver = new Label("Game over!");
                lblOver.getStyleClass().add("lblOver");
                hOvrLabel.setAlignment(Pos.CENTER);
                hOvrLabel.getChildren().setAll(lblOver);
                hOvrLabel.setTranslateY(TOP_HEIGHT + vGame.getSpacing());
                this.getChildren().add(hOvrLabel);

                hOvrButton.setMinSize(GRID_WIDTH, GRID_WIDTH / 2);
                Button bTry = new Button("Try again");
                bTry.getStyleClass().setAll("try");

                bTry.setOnTouchPressed(e -> {
			resetGame();
                        scegliGiocatore();
		});
                bTry.setOnAction(e -> {
			resetGame();
                        scegliGiocatore();
		});

                hOvrButton.setAlignment(Pos.CENTER);
                hOvrButton.getChildren().setAll(bTry);
                hOvrButton.setTranslateY(TOP_HEIGHT + vGame.getSpacing() + GRID_WIDTH / 2);
                this.getChildren().add(hOvrButton);
            }
            else if (newValue && statsOnProperty.get())
            {
                if (statsNProperty.greaterThan(0).get())
                {
                    this.maxValue = maxValue();
                    this.maxScore = gameScoreProperty.get();

                    statistics.add(new Tripla(maxMoves, maxScore, maxValue));
                    this.maxMoves = 0;
                    statsNProperty.set(statsNProperty.get() -1);
                    
                    resetGame();
                }
                if (statsNProperty.isEqualTo(0).get())
                {
                    showStat(statistics);
                }
            }
        });

        gameWonProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue && !statsOnProperty.get()) {
                layerOnProperty.set(true);
                hOvrLabel.getStyleClass().setAll("won");
                hOvrLabel.setMinSize(GRID_WIDTH, GRID_WIDTH);
                Label lblWin = new Label("You win!");
                lblWin.getStyleClass().add("lblWon");
                hOvrLabel.setAlignment(Pos.CENTER);
                hOvrLabel.getChildren().setAll(lblWin);
                hOvrLabel.setTranslateY(TOP_HEIGHT + vGame.getSpacing());
                this.getChildren().add(hOvrLabel);

                hOvrButton.setMinSize(GRID_WIDTH, GRID_WIDTH / 2);
                hOvrButton.setSpacing(10);
                Button bContinue = new Button("Keep going");
                bContinue.getStyleClass().add("try");
                bContinue.setOnAction(e -> {
                    layerOnProperty.set(false);
                    getChildren().removeAll(hOvrLabel, hOvrButton);
                });
                Button bTry = new Button("Try again");
                bTry.getStyleClass().add("try");
                bTry.setOnTouchPressed(e -> {
                        scegliGiocatore();
		});
                bTry.setOnAction(e -> {
                        scegliGiocatore();
		});
                hOvrButton.setAlignment(Pos.CENTER);
                hOvrButton.getChildren().setAll(bContinue, bTry);
                hOvrButton.setTranslateY(TOP_HEIGHT + vGame.getSpacing() + GRID_WIDTH / 2);
                this.getChildren().add(hOvrButton);
            }
        });

        automaticPlayerProperty.addListener((observable, oldValue, newValue) -> {
            Scene scene = this.getScene();
            if (newValue)
            {
                this.keyH = scene.getOnKeyPressed();
                this.swipeHUp = scene.getOnSwipeUp();
                this.swipeHRight = scene.getOnSwipeRight();
                this.swipeHDown = scene.getOnSwipeDown();
                this.swipeHLeft = scene.getOnSwipeLeft();
                scene.setOnKeyPressed(null);
                scene.setOnSwipeUp(null);
                scene.setOnSwipeRight(null);
                scene.setOnSwipeDown(null);
                scene.setOnSwipeLeft(null);
            }
            else
            {
                scene.setOnKeyPressed(this.keyH);
                scene.setOnSwipeUp(this.swipeHUp);
                scene.setOnSwipeRight(this.swipeHRight);
                scene.setOnSwipeDown(this.swipeHDown);
                scene.setOnSwipeLeft(this.swipeHLeft);
            }
        });
        layerOnProperty.addListener((observable, oldValue, newValue) -> {
            if (!newValue)
            {
                if ((isAutomaticPlayerSet() && !statsOnProperty.get())
                        || (statsOnProperty.get() && statsNProperty.greaterThan(0).get()))
                {
                    long periodo = PERIODO;
                    if (statsOnProperty.get()) periodo = PERIODO_STATS;
                    try
                    {
                        this.invocatore = new InvocatoreGiocatore(this, periodo);
                        this.invocatore.start();
                    }
                    catch (Exception e)
                    {
                        automaticPlayerProperty.set(false);
                        resetGame();
                        scegliGiocatore();
                    }
                }
            }
            else
            {
            }
        });
    }

    private void clearGame() {
        List<Node> collect = gridGroup.getChildren().filtered(c -> c instanceof Tile).stream().collect(Collectors.toList());
        gridGroup.getChildren().removeAll(collect);
        gameGrid.clear();
        getChildren().removeAll(hOvrLabel, hOvrButton, vButton);

        layerOnProperty.set(false);
        gameScoreProperty.set(0);
        gameWonProperty.set(false);
        gameOverProperty.set(false);

        initializeLocationsInGameGrid();
    }

    private void resetGame() {
        clearGame();
        initializeGrid();
    }

    /**
     * Clears the grid and redraws all tiles in the <code>gameGrid</code> object
     */
    private void redrawTilesInGameGrid() {
        gameGrid.values().stream().filter(Objects::nonNull).forEach(t -> {
            double layoutX = t.getLocation().getLayoutX(CELL_SIZE) - (t.getMinWidth() / 2);
            double layoutY = t.getLocation().getLayoutY(CELL_SIZE) - (t.getMinHeight() / 2);

            t.setLayoutX(layoutX);
            t.setLayoutY(layoutY);
            gridGroup.getChildren().add(t);
        });
    }

    private Timeline animateScore(String v1) {
        final Timeline timeline = new Timeline();
        lblPoints.setText("+" + v1);
        lblPoints.setOpacity(1);
        lblPoints.setLayoutX(400);
        lblPoints.setLayoutY(20);
        final KeyValue kvO = new KeyValue(lblPoints.opacityProperty(), 0);
        final KeyValue kvY = new KeyValue(lblPoints.layoutYProperty(), 100);

        Duration animationDuration = Duration.millis(600);
        final KeyFrame kfO = new KeyFrame(animationDuration, kvO);
        final KeyFrame kfY = new KeyFrame(animationDuration, kvY);

        timeline.getKeyFrames().add(kfO);
        timeline.getKeyFrames().add(kfY);

        return timeline;
    }

    interface AddTile {
        void add(int value, int x, int y);
    }

    /**
     * Initializes all cells in gameGrid map to null
     */
    private void initializeLocationsInGameGrid() {
        traverseGrid((x, y) -> {
            Location thisloc = new Location(x, y);
            gameGrid.put(thisloc, null);
            return 0;
        });
    }

    private void initializeGrid() {
        initializeLocationsInGameGrid();

        Tile tile0 = Tile.newRandomTile();
        List<Location> randomLocs = new ArrayList<>(locations);
        Collections.shuffle(randomLocs);
        Iterator<Location> locs = randomLocs.stream().limit(2).iterator();
        tile0.setLocation(locs.next());

        Tile tile1 = null;
        if (new Random().nextFloat() <= 0.8) { // gives 80% chance to add a second tile
            tile1 = Tile.newRandomTile();
            if (tile1.getValue() == 4 && tile0.getValue() == 4) {
                tile1 = Tile.newTile(2);
            }
            tile1.setLocation(locs.next());
        }

        Arrays.asList(tile0, tile1).forEach(t -> {
            if (t == null) {
                return;
            }
            gameGrid.put(t.getLocation(), t);
        });

        redrawTilesInGameGrid();
    }

    /**
     * Finds a random location or returns null if none exist
     *
     * @return a random location or <code>null</code> if there are no more
     * locations available
     */
    private Location findRandomAvailableLocation() {
        List<Location> availableLocations = locations.stream().filter(l -> gameGrid.get(l) == null).collect(Collectors.toList());

        if (availableLocations.isEmpty()) {
            return null;
        }

        Collections.shuffle(availableLocations);
        Location randomLocation = availableLocations.get(new Random().nextInt(availableLocations.size()));
        return randomLocation;
    }

    private void addAndAnimateRandomTile(Location randomLocation) {
        Tile tile = Tile.newRandomTile();
        tile.setLocation(randomLocation);

        double layoutX = tile.getLocation().getLayoutX(CELL_SIZE) - (tile.getMinWidth() / 2);
        double layoutY = tile.getLocation().getLayoutY(CELL_SIZE) - (tile.getMinHeight() / 2);

        tile.setLayoutX(layoutX);
        tile.setLayoutY(layoutY);
        tile.setScaleX(0);
        tile.setScaleY(0);

        gameGrid.put(tile.getLocation(), tile);
        gridGroup.getChildren().add(tile);

        animateNewlyAddedTile(tile).play();
    }

    private static final Duration ANIMATION_EXISTING_TILE = Duration.millis(125);

    private Timeline animateExistingTile(Tile tile, Location newLocation) {
        Timeline timeline = new Timeline();
        KeyValue kvX = new KeyValue(tile.layoutXProperty(), newLocation.getLayoutX(CELL_SIZE) - (tile.getMinHeight() / 2));
        KeyValue kvY = new KeyValue(tile.layoutYProperty(), newLocation.getLayoutY(CELL_SIZE) - (tile.getMinHeight() / 2));

        KeyFrame kfX = new KeyFrame(ANIMATION_EXISTING_TILE, kvX);
        KeyFrame kfY = new KeyFrame(ANIMATION_EXISTING_TILE, kvY);

        timeline.getKeyFrames().add(kfX);
        timeline.getKeyFrames().add(kfY);

        return timeline;
    }

    // after last movement on full grid, check if there are movements available
    private EventHandler<ActionEvent> onFinishNewlyAddedTile = e -> {
        if (this.gameGrid.values().parallelStream().noneMatch(Objects::isNull) && !mergeMovementsAvailable()) {
            this.gameOverProperty.set(true);
        }
    };

    private static final Duration ANIMATION_NEWLY_ADDED_TILE = Duration.millis(125);

    private Timeline animateNewlyAddedTile(Tile tile) {
        Timeline timeline = new Timeline();
        KeyValue kvX = new KeyValue(tile.scaleXProperty(), 1);
        KeyValue kvY = new KeyValue(tile.scaleYProperty(), 1);

        KeyFrame kfX = new KeyFrame(ANIMATION_NEWLY_ADDED_TILE, kvX);
        KeyFrame kfY = new KeyFrame(ANIMATION_NEWLY_ADDED_TILE, kvY);

        timeline.getKeyFrames().add(kfX);
        timeline.getKeyFrames().add(kfY);
        timeline.setOnFinished(onFinishNewlyAddedTile);
        return timeline;
    }

    private static final Duration ANIMATION_TILE_TO_BE_MERGED = Duration.millis(150);

    private Timeline hideTileToBeMerged(Tile tile) {
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(tile.opacityProperty(), 0);
        KeyFrame kf = new KeyFrame(ANIMATION_TILE_TO_BE_MERGED, kv);
        timeline.getKeyFrames().add(kf);
        return timeline;
    }

    public void saveSession() {
        SessionManager sessionManager = new SessionManager(DEFAULT_GRID_SIZE);
        sessionManager.saveSession(gameGrid, gameScoreProperty.getValue());
    }

    public void restoreSession() {
        SessionManager sessionManager = new SessionManager(DEFAULT_GRID_SIZE);

        clearGame();
        int score = sessionManager.restoreSession(gameGrid);
        if (score >= 0) {
            gameScoreProperty.set(score);
            redrawTilesInGameGrid();
        } else {
            // not session found, restart again
            resetGame();
        }
    }
    
    /*** Metodo che analizza la griglia di gioco corrente per trovare il valore massimo 
     * @author Claudia
     * @return Valore intero corrispondente al massimo valore. 
     */
    public int maxValue(){
        for (int x=0; x<gridSize; x++)
            for (int y=0; y<gridSize; y++){
                Tile tile = gameGrid.get(new Location(x,y));
                if (tile.getValue() > this.maxValue )
                    this.maxValue = tile.getValue();
            }
        return this.maxValue;
    }
                    
    /**
     * Converte la gameGrid di GameManager in una Griglia, in modo da renderla
     * utilizzabile dal GiocatoreAutomatico.
     * 
     * @author BOFH873
     * 
     * @return la Griglia corrispondente al gameGrid attuale.
     */
    public Griglia getGriglia ()
    {
        Griglia grid = new MyGriglia();

        synchronized (gameGrid)
        {
            Iterator<Map.Entry<Location, Tile>> iter = this.gameGrid.entrySet().iterator();
            Map.Entry<Location, Tile> entry;
            while (iter.hasNext())
            {                
                entry = iter.next();
                grid.put(
                        entry.getKey(),
                        (entry.getValue() != null) ? entry.getValue().getValue() : -1
                );
            }
        }
        return grid;
    }

    /**
     * Wrapper per layerOnProperty.
     * 
     * @return valore attuale di layerOnProperty
     */
    public boolean isLayerOn() {
        return layerOnProperty.get();
    }

    /**
     * Restituisce true se si è deciso di lasciar giocare il giocatore automatico.
     * 
     * @author Annalisa
     * 
     * @return true if the user decides to let the authomatic player play; false if the user decides to play.
    **/
    public boolean isAutomaticPlayerSet(){
        return automaticPlayerProperty.get();
    }
    /**
     * Crea il dialogue per scegliere se giocare manualmente o lasciar giocare il giocatore automatico.
     * 
     * @author Annalisa
     **/
    public void scegliGiocatore(){
        layerOnProperty.set(true);
        hOvrLabel.getStyleClass().setAll("over");
        hOvrLabel.setMinSize(GRID_WIDTH, GRID_WIDTH);
        Label lblSceltaGiocatore = new Label("Who plays?");
        lblSceltaGiocatore.getStyleClass().add("lblOver"); 
        hOvrLabel.setAlignment(Pos.TOP_CENTER);
        hOvrLabel.setMargin(lblSceltaGiocatore, new Insets(30, 0, 10, 0));
        hOvrLabel.getChildren().setAll(lblSceltaGiocatore);
        hOvrLabel.setTranslateY(TOP_HEIGHT + vGame.getSpacing());
        this.getChildren().add(hOvrLabel);



        vButton.setMinSize(GRID_WIDTH, GRID_WIDTH / 2);
        vButton.setSpacing(30);
        vButton.setAlignment(Pos.TOP_CENTER);
        vButton.setPadding(new Insets(0, 150, 10, 150));
        vButton.setTranslateY(TOP_HEIGHT + vGame.getSpacing() + (GRID_WIDTH )/ 3);


        Button bHumanPlayer = new Button("Human Player");
        bHumanPlayer.getStyleClass().add("try");

        bHumanPlayer.setOnAction(e -> {
                automaticPlayerProperty.set(false);
                statsOnProperty.set(false);
                resetGame();
        });

        bHumanPlayer.setOnTouchPressed(e -> {
                automaticPlayerProperty.set(false);
                statsOnProperty.set(false);
                resetGame();
        });

        Button bAutomaticPlayer = new Button("Automatic Player");
        bAutomaticPlayer.getStyleClass().add("try");

        bAutomaticPlayer.setOnTouchPressed(e -> {
                automaticPlayerProperty.set(true);
                statsOnProperty.set(false);
                resetGame();
        });

        bAutomaticPlayer.setOnAction(e -> {
                automaticPlayerProperty.set(true);
                statsOnProperty.set(false);
                resetGame();
        });

        Button statisticsButton = new Button("A.P. Statistics");
        statisticsButton.getStyleClass().add("try");

        statisticsButton.setOnAction(e -> {
                automaticPlayerProperty.set(true);
                statsOnProperty.set(true);
                statistics.clear();
                statsNProperty.set(PLAYS_NUMBER);
                resetGame();
        });
        statisticsButton.setOnTouchPressed(e -> {
                automaticPlayerProperty.set(true);
                statsOnProperty.set(true);
                statistics.clear();
                statsNProperty.set(PLAYS_NUMBER);
                resetGame();
        });

        vButton.getChildren().setAll(bHumanPlayer, bAutomaticPlayer, statisticsButton);

        this.getChildren().addAll(vButton);
    }
    /**
     * Ripulisce l'interfaccia dopo che sono state visualizzate le statistiche
     * tramite showStat(). Viene richiamata da showStat() appena l'utente clicca il
     * bottone "Back".
     */
    private void clearBox() {
    
            vGame.getChildren().clear();
            lblScore = new Label("0");
            lblPoints = new Label();
            hTop = new HBox(0);
            gridGroup = new Group();
            hBottom = new HBox();
            getChildren().clear();
            createScore();
            createGrid();
            scegliGiocatore();
    }
    
    /**
     * Avvia l'interfaccia che mostra le statistiche sulle partite effettuate
     * dall'IA.
     * 
     * @param data Contiene tutte le partite effettute.
     */    
    private void showStat(ObservableList<Tripla> data) {
        
        layerOnProperty.set(true);
        
        VBox vTitle = new VBox();
        VBox vPrinc = new VBox();
        VBox vSecond = new VBox();
        HBox hOvrLabelStat = new HBox();
        HBox hOvrMaxScore = new HBox();
        HBox hOvrAvg = new HBox();
        HBox hOvrMaxTile = new HBox();
        VBox vOvrScrl = new VBox();
        ScrollPane sp = new ScrollPane();
        TableView<Tripla> table = new TableView<>();
        
        Label lbl = new Label("Statistiche");
        lbl.getStyleClass().add("subtitle");
        hOvrLabelStat.getChildren().add(lbl);
        
        int maxScore = 0;
        int avg = 0;
        int maxTile = 0;
        for (Tripla t: data)
        {
            avg += t.getMaxScoreAsInt();
            maxScore = (t.getMaxScoreAsInt() > maxScore) ? t.getMaxScoreAsInt() : maxScore;
            maxTile = (t.getMaxValueAsInt() > maxTile) ? t.getMaxValueAsInt() : maxTile;
        }
        avg /= data.size();
        
        Label lblMaxScore = new Label("Punteggio max: ");
        Label valMaxScore = new Label(Integer.toString(maxScore));
        hOvrMaxScore.setSpacing(vGame.getSpacing());
        lblMaxScore.getStyleClass().add("labelStat");
        valMaxScore.getStyleClass().add("labelStat");        
        hOvrMaxScore.getChildren().addAll(lblMaxScore, valMaxScore);
        
        Label lblAvg = new Label("Media punti: ");
        Label valAvg = new Label(Integer.toString(avg));
        hOvrAvg.setSpacing(vGame.getSpacing());
        lblAvg.getStyleClass().add("labelStat");
        valAvg.getStyleClass().add("labelStat");
        hOvrAvg.getChildren().addAll(lblAvg, valAvg);
        
        Label lblMaxTile = new Label("Tile max: ");
        Label valMaxTile = new Label(Integer.toString(maxTile));
        hOvrMaxTile.setSpacing(vGame.getSpacing());
        lblMaxTile.getStyleClass().add("labelStat");
        valMaxTile.getStyleClass().add("labelStat");
        hOvrMaxTile.getChildren().addAll(lblMaxTile, valMaxTile);
        
        Label scrollTitle = new Label("Statistiche complete: ");
        scrollTitle.getStyleClass().add("labelStat");
                
        TableColumn matchCol = new TableColumn("Partita n.");
        TableColumn param1Col = new TableColumn("V/S");
        TableColumn scoreCol = new TableColumn<>("Punteggio");
        TableColumn movesCol = new TableColumn<>("Mosse");
        TableColumn valueCol = new TableColumn<>("Valore raggiunto");
        
        matchCol.setMinWidth(GRID_WIDTH / 5);
        param1Col.setMinWidth(GRID_WIDTH / 5);
        scoreCol.setMinWidth(GRID_WIDTH / 3);
        movesCol.setMinWidth(GRID_WIDTH / 3);
        valueCol.setMinWidth(GRID_WIDTH / 3);
        
        
        scoreCol.setCellValueFactory(new PropertyValueFactory<Tripla, String>("maxScore"));
        movesCol.setCellValueFactory(new PropertyValueFactory<Tripla, String>("maxMoves"));
        valueCol.setCellValueFactory(new PropertyValueFactory<Tripla, String>("maxValue"));
        
        table.getColumns().addAll(scoreCol, movesCol, valueCol);        
        table.getStyleClass().add("table");

        table.setItems(data);

        
        sp.setContent(table);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefSize((Integer)GRID_WIDTH, 150);
        
        vOvrScrl.setPadding(new Insets(5));
        vOvrScrl.setSpacing(3);
        vOvrScrl.getChildren().addAll(scrollTitle, sp);

        vTitle.getChildren().add(hOvrLabelStat);
        
        vPrinc.setSpacing(15);
        vPrinc.setPadding(new Insets(10));
        vPrinc.getChildren().addAll(hOvrMaxScore, hOvrAvg, hOvrMaxTile);
        
        Button bBack = new Button("Back");
        bBack.getStyleClass().add("try");
        bBack.setOnTouchPressed(e -> {
            clearBox();
        });
        bBack.setOnAction(e -> {
            clearBox();
        });

        
        vSecond.setSpacing(7);
        vSecond.setPadding(new Insets(10));
        vSecond.getChildren().addAll(vOvrScrl, bBack);
        
        vGame.setSpacing(20);
        vGame.getChildren().setAll(vTitle, vPrinc, vSecond);
    }
    
    /**
     * Classe che implementa l'interfaccia Griglia per poter interagire col
     * GiocatoreAutomatico.
     */
    private class MyGriglia extends HashMap<Location, Integer> implements Griglia {}
    
    /** Classe interna necessaria per gestire in un unico oggetto i tre dati.  
     * @author Claudia
     * 
     */
    public class  Tripla{
        private int maxScore;
        private int maxValue;
        private int maxMoves;

        public Tripla(int maxMoves, int maxScore, int maxValue){
            this.maxScore = maxScore;
            this.maxValue = maxValue;
            this.maxMoves = maxMoves;
        }
        
        /** Metodo getter della variabile maxScore
         * @Author Claudia
         * @return Stringa rappresentante il punteggio massimo.
         */
        public String getMaxScore(){ return Integer.toString(this.maxScore); }
        /** Metodo getter della variabile maxMoves
         * @author Claudia
         * @return Stringa rappresentante il numero di mosse.
         */
        public String getMaxMoves(){ return Integer.toString(this.maxMoves); }
        /** Metodo getter della variabile maxValue.
         * @author Claudia
         * @return Stringa rappresentante il valore massimo raggiunto.
         */
        public String getMaxValue(){ return Integer.toString(this.maxValue); }

        public int getMaxScoreAsInt(){ return this.maxScore; }
        public int getMaxMovesAsInt(){ return this.maxMoves; }
        public int getMaxValueAsInt(){ return this.maxValue; }
    }
    
}



