package inkball;

import processing.data.JSONArray;

public class Level {
    
    public String layout;
    public int time;
    public int spawnInterval;
    public float scoreIncreaseModifier;
    public float scoreDecreaseModifier;
    public int[] balls;
    public float startingScore;

    /**
     * Constructor that sets all the characteristics of a level based on a config file.
     * @param layout the layout of the level
     * @param time the time limit for the level
     * @param spawnInterval the time interval between spawning balls
     * @param scoreIncreaseModifier the number to multiply the score by when increasing it
     * @param scoreDecreaseModifier the number to multiply the score by when decreasing it
     * @param balls the list of balls to be spawned
     */
    public Level(String layout, int time, int spawnInterval, float scoreIncreaseModifier, float scoreDecreaseModifier, JSONArray balls) {
        this.startingScore = 0;
        this.layout = layout;
        this.time = time;
        this.spawnInterval = spawnInterval;
        this.scoreIncreaseModifier = scoreIncreaseModifier;
        this.scoreDecreaseModifier = scoreDecreaseModifier;
        this.balls = new int[balls.size()];
        
        //Assign a number to each colour of the ball and store in the balls attribute of this class
        for (int i = 0; i < balls.size(); i++) {
            String colour = balls.getString(i);
            
            switch (colour) {
                case "grey":
                    this.balls[i] = 0;
                    break;
                case "orange":
                    this.balls[i] = 1;
                    break;
                case "blue":
                    this.balls[i] = 2;
                    break;
                case "green":
                    this.balls[i] = 3;
                    break;
                case "yellow":
                    this.balls[i] = 4;
                    break;
            }
        }
    }
}
