package inkball;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.*;

public class App extends PApplet {

    public static final int CELLSIZE = 32;
    public static final int TOPBAR = 64;
    public static int WIDTH = 576;
    public static int HEIGHT = 640;

    public static final int FPS = 30;

    public int currentLevel = 0;
    public boolean paused = false;
    public boolean levelEnded = false;
    public boolean gameEnded = false;

    public static Random random = new Random();

    /**
     * Constructor that sets the path to the configuration file.
     */
    public App() {
        this.configPath = "config.json";
    }

	/**
     * Sets the size of the window
     */
    @Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    private Tile[][] board;
    private HashMap<String, PImage> sprites = new HashMap<>();

    /**
     * Given the name of an image, it gets its sprite from an array of sprites
     * if it is already loaded. Otherwise, it loads the sprite and returns it.
     * @param s name of the sprite
     * @return sprite
     */
    public PImage getSprite(String s) {
        PImage result = sprites.get(s);
        
        if (result == null) {
            result = loadImage(this.getClass().getResource(s+".png").getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
            sprites.put(s, result);
        }
        return result;
    }

    public String configPath;
    public ArrayList<Level> levels = new ArrayList<>();
    private JSONObject scoreIncrease;
    private JSONObject scoreDecrease;
    private Map<Integer, Float> scoreIncreaseMap = new HashMap<>();
    private Map<Integer, Float> scoreDecreaseMap = new HashMap<>();

	/**
     * Loads and stores all the information about each level from a configuration file,
     * loads all the sprites required for the game and calls the resetGame() function.
     */
    @Override
    public void setup() {
        frameRate(FPS);
		
        //Load the config file and the array in it
        JSONObject jsonObject = loadJSONObject(configPath);
        JSONArray levelsArray = jsonObject.getJSONArray("levels");
        
        //Set attributes of a level
        for (int i = 0; i < levelsArray.size(); i++) {
            JSONObject levelData = levelsArray.getJSONObject(i);
            String layout = levelData.getString("layout");
            int time = levelData.getInt("time", -1);
            int spawnInterval = levelData.getInt("spawn_interval");
            float scoreIncreaseModifier = levelData.getFloat("score_increase_from_hole_capture_modifier");
            float scoreDecreaseModifier = levelData.getFloat("score_decrease_from_wrong_hole_modifier");
            JSONArray balls = levelData.getJSONArray("balls");
            levels.add(new Level(layout, time, spawnInterval, scoreIncreaseModifier, scoreDecreaseModifier, balls));
        }

        //Set the score increase values for each ball
        scoreIncrease = jsonObject.getJSONObject("score_increase_from_hole_capture");
        scoreIncreaseMap.put(0, scoreIncrease.getFloat("grey"));
        scoreIncreaseMap.put(1, scoreIncrease.getFloat("orange"));
        scoreIncreaseMap.put(2, scoreIncrease.getFloat("blue"));
        scoreIncreaseMap.put(3, scoreIncrease.getFloat("green"));
        scoreIncreaseMap.put(4, scoreIncrease.getFloat("yellow"));
        
        //Set the score decrease values for each ball
        scoreDecrease = jsonObject.getJSONObject("score_decrease_from_wrong_hole");
        scoreDecreaseMap.put(0, scoreDecrease.getFloat("grey"));
        scoreDecreaseMap.put(1, scoreDecrease.getFloat("orange"));
        scoreDecreaseMap.put(2, scoreDecrease.getFloat("blue"));
        scoreDecreaseMap.put(3, scoreDecrease.getFloat("green"));
        scoreDecreaseMap.put(4, scoreDecrease.getFloat("yellow"));

        //Load all sprites
        String[] sprites = new String[] {"entrypoint", "tile", "bar"};
        
        for (int i = 0; i < sprites.length; i++) {
            getSprite(sprites[i]);
        }
        
        for (int i = 0; i < 5; i++) {
            getSprite("ball"+String.valueOf(i));
            getSprite("hole"+String.valueOf(i));
            getSprite("wall"+String.valueOf(i));
            getSprite("broken"+String.valueOf(i));
        }

        //Set the level
        resetGame();
    }

    private ArrayList<Spawner> spawners = new ArrayList<>();
    public ArrayList<Wall> walls =  new ArrayList<>();
    public ArrayList<Hole> holes = new ArrayList<>();
    public ArrayList<Ball> balls = new ArrayList<>();
    public int spawnTimer;
    public ArrayList<Integer> remainingBalls;
    public int currentBall;
    private float score;
    public int time;
    public int startTime;
    public int elapsedTime;
    public float sinceLastFrame = 0;
    private float endInterval = 67;

    /**
     * Sets up the board, loads the level layout, sets up the spawners, holes, walls and balls and
     * resets all the relevant attributes each time the game needs to be reset.
     */
    public void resetGame() {
        this.board = new Tile[(HEIGHT-TOPBAR)/CELLSIZE][WIDTH/CELLSIZE];

        //Initialise game board
        for (int i = 0; i < this.board.length; i++) {
            for (int i2 = 0; i2 < this.board[i].length; i2++) {
                this.board[i][i2] = new Tile(i2, i);
            }
        }

        //Clear all previous elements
        walls.clear();
        holes.clear();
        spawners.clear();
        balls.clear();
        lines.clear();

        //Load level layout
        Level level = levels.get(currentLevel);
        String[] layoutLines = loadStrings(level.layout);
        
        //Set elements on the board based on each character in level layout file
        for (int i = 0; i < layoutLines.length; i++) {
            String line = layoutLines[i];
            
            for (int i2 = 0; i2 < line.length(); i2++) {
                char c = line.charAt(i2);
                
                switch (c) {
                    case 'X':
                        Wall wall = new Wall(i2, i, 0);
                        this.walls.add(wall);
                        this.board[i][i2] = wall;
                        break;
                    case '1': case '2': case '3': case '4':
                        if (this.board[i][i2] != null) {
                            Wall cWall = new Wall(i2, i, Character.getNumericValue(c));
                            this.walls.add(cWall);
                            this.board[i][i2] = cWall;
                        }
                        break;
                    case 'S':
                        Spawner spawner = new Spawner(i2, i);
                        this.spawners.add(spawner);
                        this.board[i][i2] = spawner;
                        break;
                    case 'H':
                        int colour = Character.getNumericValue(line.charAt(i2 + 1));
                        Hole hole = new Hole(i2, i, colour);
                        this.holes.add(hole);
                        this.board[i][i2] = hole;
                        this.board[i][i2+1] = null;
                        this.board[i+1][i2] = null;
                        this.board[i+1][i2+1] = null;
                        i2++;
                        break;
                    case 'B':
                        int bcolour = Character.getNumericValue(line.charAt(i2 + 1));
                        Ball ball = new Ball(i2, i, bcolour);
                        ball.setVelocity(randomVelocity(), randomVelocity());
                        this.balls.add(ball);
                        i2++;
                        break;
                    default:
                        break;
                }
            }
        }

        //Set a list of balls for the level as in the config file
        this.remainingBalls = new ArrayList<>();
        
        for (int ball : level.balls) {
            this.remainingBalls.add(ball);
        }

        //Reset all attributes and variables
        this.spawnTimer = level.spawnInterval * App.FPS;
        this.currentBall = 0;
        this.score = level.startingScore;
        this.time = level.time;
        this.startTime = millis();
        this.paused = false;
        this.levelEnded = false;
        this.gameEnded = false;
        yellowTile1[0] = 0;
        yellowTile1[1] = 0;
        yellowTile2[0] = (WIDTH/CELLSIZE)-1;
        yellowTile2[1] = ((HEIGHT-TOPBAR)/CELLSIZE)-1;
        initialised = false;
    }

    /**
     * Detects when a keyboard key is pressed and resets the level if 'r' is pressed and pauses or
     * unpauses the game is spacebar is pressed, if the level or game has not ended.
     * @param event key press as an event
     */
    @Override
    public void keyPressed(KeyEvent event){
        if (event.getKey() == 'R' || event.getKey() == 'r') {
            if (gameEnded) {
                currentLevel = 0;
            }
            resetGame();
        }
        
        if (event.getKeyCode() == 32) {
            if (!paused && !gameEnded) {
                for (Ball ball : this.balls) {
                    ball.tempVelocity = ball.getVelocity();
                    ball.setVelocity(0, 0);
                }
                paused = true;
            }
            else if (!gameEnded) {
                for (Ball ball : this.balls) {
                    ball.setVelocity(ball.tempVelocity.x, ball.tempVelocity.y);
                }
                paused = false;
            }
        }
    }

    public ArrayList<Line> lines = new ArrayList<>();
    public Line currentLine;
    private PVector lastPoint;

    /**
     * Detects when a mouse button is clicked and if it is a left click, initialises a line object.
     * @param e mouse click as an event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        if (e.getButton() == PConstants.LEFT) {
            currentLine = new Line();
            lines.add(currentLine);
            currentLine.addPoint(mouseX, mouseY);
        }
    }
	
	/**
     * Detects if left mouse button is held and dragged and keeps adding the mouse points to
     * line object. If points are too far away, it creates points in the middle to fill the gaps.
     * @param e mouse held as an event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        if (e.getButton() == PConstants.LEFT) {
            PVector currentPoint = new PVector(mouseX, mouseY);

            if (lastPoint != null) {
                float dist = PVector.dist(lastPoint, currentPoint);

                //Fill in gaps between points if points too far away
                if (dist >= 10) {
                    for (int i = 0; i < dist; i++) {
                        float lerpX = lerp(lastPoint.x, currentPoint.x, i / dist);
                        float lerpY = lerp(lastPoint.y, currentPoint.y, i / dist);
                        currentLine.addPoint(lerpX, lerpY);
                    }
                }
            }
            currentLine.addPoint(mouseX, mouseY);
            lastPoint = currentPoint;
        }
    }

    /**
     * Detects if mouse click is released. If it is a left click, it completes the line object
     * and stores it. If it a left click and the ctrl button is held down or if it is a right click,
     * it removes the line it is clicked over.
     * @param e mouse release as an event
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        if (e.getButton() == PConstants.LEFT && !e.isControlDown()) {
            currentLine.addPoint(mouseX, mouseY);
            currentLine.completed();
            lastPoint = null;
        }
        else if (e.getButton() == PConstants.RIGHT || (e.getButton() == PConstants.LEFT && e.isControlDown())) {
            for (Line line : lines) {
                boolean removed = false;
                
                for (PVector point : line.points) {
                    float distance = PVector.dist(point, new PVector(mouseX, mouseY));
                    
                    if (distance <= 5) {
                        lines.remove(line);
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    break;
                }
            }
        }
    }

	/**
     * Draws all the elements on the window each frame. If collisions occur, it calls other functions to
     * handle the collisions and update score. It also handles the timers and the level end animation.
     */
    @Override
    public void draw() {
        background(200,200,200);
        
        //Draw all elements of board
        for (int i = 0; i < this.board.length; i++) {
            for (int i2 = 0; i2 < this.board[i].length; i2++) {
                if (this.board[i][i2] != null) {
                    this.board[i][i2].draw(this);
                }
            }
        }

        //Draw lines
        if (!levelEnded && !gameEnded) {
            for (Line line : this.lines) {
                line.draw(this);
            }
        }

        //Stop ball movement when level ends in loss
        if (levelEnded && (currentBall<remainingBalls.size() || this.balls.size()!=0)) {
            for (Ball ball : this.balls) {
                ball.setVelocity(0, 0);
            }
        }

        //Draw balls and check collisions or hole captures
        for (int i = this.balls.size()-1; i >= 0; i--) {
            Ball ball = this.balls.get(i);
            ball.draw(this);
            
            //Check if near hole or captured
            if (checkHole(ball) && !paused && !levelEnded) {
                int colour = ball.checkHoleCapture(this.holes);
                
                //Increase or decrease score if colour matches or doesn't match, respectively
                if (colour!=-1) {
                    if (colourCheck(ball, colour)) {
                        increaseScore(ball);
                    }
                    else {
                        decreaseScore(ball);
                        remainingBalls.add(ball.getColour()); //Add ball back to queue if wrong capture
                    }
                    this.balls.remove(ball); //Remove ball from board
                }
            }
            //Check for collisions if not near hole
            else if (!paused && !levelEnded) {
                ball.restoreSize();
                checkAllCollisions(ball);
                ball.update();
            }
        }

        //If all balls captured correctly, end level
        if (remainingBalls.size() - currentBall == 0 && this.balls.size() == 0) {
            levelEnded = true;
            drawYellowTile(); //Draw end animation
            
            if (this.time>0) {
                sinceLastFrame += millis() - elapsedTime;
                
                //Add remaining time to score
                if (sinceLastFrame >= endInterval && !paused) {
                    this.time--;
                    this.score++;
                    sinceLastFrame = 0;
                }
            }
            //Load next level once all time has been added to score
            if (this.time <= 0) {
                if (currentLevel+1<this.levels.size()) {
                    currentLevel++;
                    levels.get(currentLevel).startingScore = this.score; //Store score to be consistent across levels
                    resetGame();
                }
                else {
                    gameEnded = true; //If no levels remaining, end game
                }
            }
        }

        //Display score
        textSize(21);
        fill(0);
        text("Score: " + (int) score, WIDTH-135, App.TOPBAR-34);
        
        elapsedTime = millis();
        
        //Decrement timer
        if (elapsedTime-startTime>=1000 && this.time>0 && !paused && !gameEnded) {
            this.time--;
            startTime = elapsedTime;
        }
        
        //Display timer
        textSize(21);
        fill(0);
        text("Time: " + this.time, WIDTH-135, App.TOPBAR-8);
        
        //Display "time's up" if timer finished and level lost
        if (this.time == 0 && !gameEnded) {
            textSize(21);
            fill(0);
            text("=== TIME'S UP ===", 200, App.TOPBAR-20);
            levelEnded = true;
        }

        //Display "paused" if game is paused
        if (paused) {
            textSize(21);
            fill(0);
            text("*** PAUSED ***", 210, App.TOPBAR-20);
        }

        //Display "game ended" if all levels finished
        if (gameEnded) {
            textSize(21);
            fill(0);
            text("=== ENDED ===", 200, App.TOPBAR-20);
        }

        //Display black bar in top left
        PImage bar = this.getSprite("bar");
        this.image(bar, 10, 17);

        //Display spawn timer if balls still left to be spawned
        if (currentBall<remainingBalls.size()) {
            float spawnLeft = spawnTimer/(float) App.FPS;
            textSize(15);
            fill(0);
            text(String.format("%.1f", spawnLeft), 145, 37);
        }

        if (spawnTimer > 0 && !paused && !levelEnded) {
            spawnTimer--; //Decrement spawn timer
        }
        
        //Spawn next ball when spawn timer finished and balls remaining
        if (spawnTimer == 0 && currentBall < remainingBalls.size() && !levelEnded) {
            spawnBall();
            isQueueShifting = true;
            
            if (currentBall <= remainingBalls.size()-1) {
                spawnTimer = levels.get(currentLevel).spawnInterval*App.FPS; //Reset spawn timer
            }
        }

        drawBallQueue(); //Draw ball queue in the black bar
    }

    /**
     * Checks if the colour of the ball and hole match or if it is a grey hole or a grey ball.
     * @param ball the ball object to compare colour with
     * @param colour the colour of the hole the ball is absorbed by
     * @return true if it matches, false otherwise
     */
    public boolean colourCheck(Ball ball, int colour) {
        if (ball.isColoured()) {
            if (colour == 0 || ball.getColour() == colour) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Increases the score by the values specified in the config file using the colour of the ball.
     * @param ball ball object to get the colour of
     */
    private void increaseScore(Ball ball) {
        float addScore = scoreIncreaseMap.get(ball.getColour()) * levels.get(currentLevel).scoreIncreaseModifier;
        this.score += addScore;
    }

    /**
     * Decreases the score by the values specified in the config file using the colour of the ball.
     * @param ball ball object to get the colour of
     */
    private void decreaseScore(Ball ball) {
        float subScore = scoreDecreaseMap.get(ball.getColour()) * levels.get(currentLevel).scoreDecreaseModifier;
        this.score -= subScore;
    }

    /**
     * Checks if ball is within the bounds of a hole.
     * @param ball ball object to check
     * @return true if it is within bounds, false otherwise
     */
    private boolean checkHole(Ball ball) {
        for (Hole hole : this.holes) {
            float distance = PVector.dist(ball.centrePosition, hole.centrePosition);
            if (distance <= 32) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calls other functions to check all the collisions of the ball with lines, walls and screen edges.
     * @param ball ball object to check
     */
    public void checkAllCollisions(Ball ball) {
        //Check collision with lines
        for (Line line : this.lines) {
            if (ball.checkLineCollision(line.points)) {
                this.lines.remove(line); //Remove line if collided
                return;
            }
        }
        
        //Check collision with walls
        for (Wall wall : this.walls) {
            if (ball.checkWallCollision(wall)) {
                if (wall.isColoured()) {
                    //Damage the coloured wall if hit with ball of same colour
                    if (ball.getColour() == wall.getColour() && wall.hits < 3) {
                        wall.hits++;
                    }
                    ball.setColour(wall.getColour()); //Change colour of ball if collided with coloured wall
                }
                else if (wall.hits < 3) { //Damage grey walls with any ball
                    wall.hits++;
                }
                
                //Remove wall if hit three times
                if (wall.hits == 3) {
                    int x = wall.getX()/CELLSIZE;
                    int y = (wall.getY()-TOPBAR)/CELLSIZE;
                    this.board[y][x] = new Tile(x, y);
                    this.walls.remove(wall);
                }
                return;
            }
        }
        
        //Check collision with screen edges
        if (ball.checkBoundaryCollision()) {
            return;
        }
    }

    /**
     * Spawns a new ball from a randomly chosen spawner on the board.
     */
    private void spawnBall() {
        if (!spawners.isEmpty()) {
            int randSpawn = random.nextInt(spawners.size());
            Spawner spawner = spawners.get(randSpawn);

            int colour = remainingBalls.get(currentBall);
            currentBall++;
            Ball ball = new Ball(spawner.getX(), spawner.getY(), colour);
            ball.setVelocity(randomVelocity(), randomVelocity());
            balls.add(ball);
        }
    }

    /**
     * Gets a random velocity to set for a ball. It uses a random boolean and returns either 2 or -2 accordingly.
     * @return 2 or -2 as the velocity
     */
    private float randomVelocity() {
        boolean vel = random.nextBoolean();
        if (vel) {
            return (float) 2;
        }
        return (float) -2;
    }

    private int startX = 11;
    private int startY = 21;
    private int ballGap = 26;
    private float ballShift = 0;
    public boolean isQueueShifting = false;

    /**
     * Draws the ball queue at the top left of the window and moves the balls 1 px/frame each time
     * a ball is spawned.
     */
    private void drawBallQueue() {
        //Set starting point of first ball based on if queue shifting or not
        if (isQueueShifting) {
            startX = 37;
        }
        else {
            startX = 11;
        }
        
        int maxBalls = Math.min(5, remainingBalls.size()-currentBall); //Get maximum number of balls to display
        
        for (int i = 0; i < maxBalls; i++) {
            int colour = remainingBalls.get(currentBall+i);
            float xPosition = startX + (i*ballGap) - ballShift; //Set x position based on how it is shifting
            PImage ball = getSprite("ball"+String.valueOf(colour));
            
            if (xPosition+ball.width<=140) {
                this.image(ball, xPosition, startY); //Display ball
            }
        }
        
        //Shift next position to left
        if (isQueueShifting && !paused && !levelEnded) {
            if (ballShift < ballGap) {
                ballShift+=1;
            }
            else {
                ballShift=0;
                isQueueShifting = false;
            }
        }
    }

    private int[] yellowTile1 = {0, 0};
    private int[] yellowTile2 = {(WIDTH/CELLSIZE)-1, ((HEIGHT-TOPBAR)/CELLSIZE)-1};
    private float sinceLastFrame2 = 0;
    private Tile originalTile1;
    private Tile originalTile2;
    private boolean initialised = false;

    /**
     * Draws the clockwise yellow tile animation when a level ends.
     */
    private void drawYellowTile() {
        sinceLastFrame2 += millis() - elapsedTime;
        
        //Draw yellow tiles at starting position first
        if (!initialised) {
            //Store original tiles
            originalTile1 = this.board[yellowTile1[1]][yellowTile1[0]];
            originalTile2 = this.board[yellowTile2[1]][yellowTile2[0]];
            //Set yellow tiles in place of original tiles
            this.board[yellowTile1[1]][yellowTile1[0]] = new Wall(yellowTile1[0], yellowTile1[1], 4);
            this.board[yellowTile2[1]][yellowTile2[0]] = new Wall(yellowTile2[0], yellowTile2[1], 4);
            initialised = true;
        }

        if (sinceLastFrame2 >= endInterval && !paused && !gameEnded) {
            //Restore original tile
            this.board[yellowTile1[1]][yellowTile1[0]] = originalTile1;
            this.board[yellowTile2[1]][yellowTile2[0]] = originalTile2;
            //Move yellow tiles' positions clockwise
            moveYellowTile(yellowTile1);
            moveYellowTile(yellowTile2);
            //Store original tiles
            originalTile1 = this.board[yellowTile1[1]][yellowTile1[0]];
            originalTile2 = this.board[yellowTile2[1]][yellowTile2[0]];
            //Set yellow tiles in place of original tiles
            this.board[yellowTile1[1]][yellowTile1[0]] = new Wall(yellowTile1[0], yellowTile1[1], 4);
            this.board[yellowTile2[1]][yellowTile2[0]] = new Wall(yellowTile2[0], yellowTile2[1], 4);
            sinceLastFrame2 = 0;
        }
    }

    /**
     * Moves the position of the yellow tile for the level end animation clockwise around the
     * edges.
     * @param yellowTile the x and y position of the yellow tile in an array
     */
    public void moveYellowTile(int[] yellowTile) {
        int x = yellowTile[0];
        int y = yellowTile[1];
        
        if (y == 0 && x < this.board[0].length - 1) {
            yellowTile[0]++;
        }
        else if (x == this.board[0].length - 1 && y < this.board.length - 1) {
            yellowTile[1]++;
        }
        else if (y == this.board.length - 1 && x > 0) {
            yellowTile[0]--;
        }
        else if (x == 0 && y > 0) {
            yellowTile[1]--;
        }
    }

    public static void main(String[] args) {
        PApplet.main("inkball.App");
    }

}
