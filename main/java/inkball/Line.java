package inkball;

import processing.core.PVector;
import java.util.*;

public class Line {
    
    public ArrayList<PVector> points;
    private boolean isComplete;

    /**
     * Constructor that creates a new list to store all the points on a line and
     * initialises it as not complete.
     */
    public Line() {
        points = new ArrayList<>();
        isComplete = false;
    }

    /**
     * Adds points of a player-drawn line to a points array as long as the line is
     * not complete.
     * @param x the x coordinate of the point to add
     * @param y the y coordinate of the point to add
     */
    public void addPoint(float x, float y) {
        if (!isComplete) {
            points.add(new PVector(x, y));
        }
    }

    /**
     * Sets the line as completed.
     */
    public void completed() {
        isComplete = true;
    }

    /**
     * Draws the player-drawn line based on the points of the line.
     * @param app an object of the App class
     */
    public void draw(App app) {
        //Set the colour and thickness of the line
        app.stroke(0);
        app.strokeWeight(10);
        
        //Draw a line between each consecutive pair of points in the list of points
        for (int i = 0; i < points.size()-1; i++) {
            PVector point1 = points.get(i);
            PVector point2 = points.get(i+1);
            
            //Only draw the line if it is inside the bounds of the game board
            if (point1.y>App.TOPBAR+3 && point2.y>App.TOPBAR+3) {
                app.line(point1.x, point1.y, point2.x, point2.y);
            }
        }
    }

    public boolean getCompleted() {
        return isComplete;
    }
}