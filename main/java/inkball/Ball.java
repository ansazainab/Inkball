package inkball;

import processing.core.PImage;
import processing.core.PVector;
import java.util.*;

public class Ball {
    
    private PVector position;
    private PVector velocity;
    private PVector acceleration = new PVector(0, 0);
    private int colour;
    private float radius = 12;
    public PVector centrePosition;
    private float size;
    public PVector tempVelocity = new PVector();

    /**
     * Constructor that sets the top left position, colour, size and centre position of a ball.
     * @param x the x position of the all
     * @param y the y position of the ball
     * @param colour the colour of the ball
     */
    public Ball(float x, float y, int colour) {
        this.position = new PVector(x, y);
        this.colour = colour;
        centrePosition = new PVector((this.position.x*App.CELLSIZE)+radius, (this.position.y*App.CELLSIZE+App.TOPBAR)+radius);
        this.size = 24;
    }

    /**
     * Draws the relevant ball image based on its colour and position
     * @param app an object of the App class
     */
    public void draw(App app) {
        PImage ball = app.getSprite("ball"+String.valueOf(colour));
        app.image(ball, this.position.x*App.CELLSIZE, this.position.y*App.CELLSIZE+App.TOPBAR, this.size, this.size);
    }

    public void setVelocity(float vx, float vy) {
        this.velocity = new PVector(vx/(float) App.FPS, vy/(float) App.FPS);
    }

    public PVector getVelocity() {
        PVector vel = PVector.mult(this.velocity, (float) App.FPS);
        return vel;
    }

    /**
     * Checks if the ball has collided with a wall and updates velocity if it has.
     * @param wall wall object to check collision with
     * @return true if collided, false otherwise
     */
    public boolean checkWallCollision(Wall wall) {
        //Get tile boundaries
        double tileLeft = wall.getLeft();
        double tileRight = wall.getRight();
        double tileTop = wall.getTop();
        double tileBottom = wall.getBottom();
    
        //Calculate the closest point on the tile to the center of the ball
        float closestX = clamp(centrePosition.x, (float) tileLeft, (float) tileRight);
        float closestY = clamp(centrePosition.y, (float) tileTop, (float) tileBottom);
    
        //Calculate the distance from the closest point to the ball's center
        float distanceX = centrePosition.x - closestX;
        float distanceY = centrePosition.y - closestY;
        float futureDistanceX = distanceX + velocity.x;
        float futureDistanceY = distanceY + velocity.y;
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        float futureDistanceSquared = futureDistanceX * futureDistanceX + futureDistanceY * futureDistanceY;
    
        //Check if the ball is touching the tile's outer edge (collision detected)
        if (distanceSquared <= radius * radius && futureDistanceSquared <= distanceSquared) {
            //Adjust the ball's position to keep it outside the tile
            if (centrePosition.x <= tileLeft) {
                centrePosition.x = (float) (tileLeft - radius);  //Move the ball to the left of the tile
            }
            else if (centrePosition.x >= tileRight) {
                centrePosition.x = (float) (tileRight + radius);  //Move the ball to the right of the tile
            }
            if (centrePosition.y <= tileTop) {
                centrePosition.y = (float) (tileTop - radius);  //Move the ball above the tile
            }
            else if (centrePosition.y >= tileBottom) {
                centrePosition.y = (float) (tileBottom + radius);  //Move the ball below the tile
            }
    
            //After adjusting the position, reflect the velocity
            float overlapX = Math.abs(centrePosition.x - closestX);
            float overlapY = Math.abs(centrePosition.y - closestY);
            
            if (Math.abs(overlapX-overlapY) <= 1e-9) {
                velocity.mult(-1); //Reflect along both axes for corner
            }
            else if (overlapX > overlapY) {
                velocity.x *= -1;  //Reflect along the x-axis for vertical edge
            }
            else {
                velocity.y *= -1;  //Reflect along the y-axis for horizontal edge
            }
            return true;
        }
        return false;
    }

    /**
     * Helper method to ensure a value stays within a given range. If too big, the maximum allowed value
     * is returned. If too small, the minimum allowed value is returned. Otherwise, the value itself is returned.
     * @param value the value to clamp within a specific range
     * @param min the minimum value of the range
     * @param max the maximum value of the range
     * @return value if within range, min or max otherwise, accordingly
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Given a hitbox of points on a line, it checks if the ball has collided with the line and changes its
     * velocity based on where it collided.
     * @param hitbox a list of all the points on a line
     * @return true if collided, false otherwise
     */
    public boolean checkLineCollision(ArrayList<PVector> hitbox) {
        for (int i = 0; i < hitbox.size()-1; i++) {
            //Get two consecutive points on the line
            PVector p1 = hitbox.get(i);
            PVector p2 = hitbox.get(i+1);
            //Calculate their distance from the centre of the ball
            float dist1 = PVector.dist(this.centrePosition, p1);
            float dist2 = PVector.dist(this.centrePosition, p2);
            
            //Reflect if distance is less than radius of ball + (thickness of line)/2
            if (dist1 <= 17 && dist2 <= 17) {
                PVector normal = getNormal(p1, p2); //Get the normal to reflect across
                reflect(normal);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the ball has collided with the edges of the screen to ensure ball remains within screen and
     * changes velocity accordingly.
     * @return true if collided, false otherwise
     */
    public boolean checkBoundaryCollision() {
        boolean collided = false;

        //Reflect across vertical boundary
        if (centrePosition.x-radius<=0 || centrePosition.x+radius>=App.WIDTH) {
            velocity.x *= -1;
            collided = true;
        }
        
        //Reflect across horizontal boundary
        if (centrePosition.y+radius>=App.HEIGHT || centrePosition.y-radius<=App.TOPBAR) {
            velocity.y *= -1;
            collided = true;
        }

        return collided;
    }

    /**
     * Checks if the ball is within the bounds of a hole and if it is, updates its velocity accordingly. If
     * it gets absorbed, it returns the colour of the hole it got absorbed by. It returns -1 if not captured
     * by hole or near hole.
     * @param holes a list of all the holes to check proximity with
     * @return colour if captured, -1 otherwise
     */
    public int checkHoleCapture(ArrayList<Hole> holes) {
        for (Hole hole : holes) {
            //Calculate distance between ball and hole
            float distance = PVector.dist(this.centrePosition, hole.centrePosition);
            
            if (distance <= 32) {
                updateHole(hole); //Update position if within bounds of hole
                distance = PVector.dist(this.centrePosition, hole.centrePosition);
                
                if (distance <= 12) { //Capture ball if on top of hole
                    return hole.getColour();
                }
            }
        }
        return -1;
    }

    /**
     * Updates the position of the ball based on velocity.
     */
    public void update() {
        this.position.add(this.velocity);
        centrePosition.set((this.position.x*App.CELLSIZE)+(size/2), (this.position.y*App.CELLSIZE+App.TOPBAR)+(size/2));
    }

    /**
     * Updates the position of the ball with a spiral gravity effect if near hole.
     * @param hole the hole object the ball is near
     */
    public void updateHole(Hole hole) {
        //Get the vector between the ball and hole and its magnitude
        PVector ballHoleVector = PVector.sub(hole.centrePosition, this.centrePosition);
        float distance = ballHoleVector.mag();

        //Calculate the force to pull the ball with (maximum of 9%), using the gravity effect
        float forceMagnitude = Math.min(0.05f/distance, 0.09f);
        PVector force = ballHoleVector.mult(forceMagnitude);
        
        //Accelerate the ball and update its position according to the force
        acceleration.add(force);
        velocity.add(acceleration);    
        position.add(velocity);
        acceleration.mult(0);
        centrePosition.set((this.position.x*App.CELLSIZE)+(size/2), (this.position.y*App.CELLSIZE+App.TOPBAR)+(size/2));
        
        //Shrink the size of the ball (with minimum size of 5)
        float shrinkFactor = 0.95f;
        size = Math.max(size*shrinkFactor, 5);
        radius = size/2;
    }

    /**
     * Restores the normal size and velocity of the ball when out of bounds of hole.
     */
    public void restoreSize() {
        //Set the magnitude of the x component of the velocity to 2
        if (velocity.x < 0) {
            velocity.x = -2/(float) App.FPS;
        }
        else {
            velocity.x = 2/(float) App.FPS;
        }
        
        //Set the magnitude of the y component of the velocity to 2
        if (velocity.y < 0) {
            velocity.y = -2/(float) App.FPS;
        }
        else {
            velocity.y = 2/(float) App.FPS;
        }
        
        //Restore size
        size = 24;
        radius = size/2;
        centrePosition.set((this.position.x*App.CELLSIZE)+(size/2), (this.position.y*App.CELLSIZE+App.TOPBAR)+(size/2));
    }

    /**
     * Calculates the closest normal of a line segment to the ball given 2 points of a line segment.
     * @param p1 starting point of a line segment
     * @param p2 ending point of a line segment
     * @return closest normal
     */
    private PVector getNormal(PVector p1, PVector p2) {
        //Calculate the difference between x and y positions of the points
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;

        //Calculate the 2 possible normals
        PVector n1 = new PVector(-dy, dx).normalize();
        PVector n2 = new PVector(dy, -dx).normalize();

        PVector midpoint = PVector.lerp(p1, p2, 0.5f); //Calculate midpoint of line

        //Calculate distance from each normal to the ball
        float dist1 = PVector.dist(PVector.add(midpoint, n1), centrePosition);
        float dist2 = PVector.dist(PVector.add(midpoint, n2), centrePosition);
        
        //Return the closer normal
        if (dist1 < dist2) {
            return n1;
        }
        return n2;
    }

    /**
     * Changes the velocity of the ball by reflecting it at a certain normal to a line segment.
     * @param normal the normal to reflect it from
     */
    private void reflect(PVector normal) {
        //Calculate new velocity using the dot product of the normal and velocity of the ball
        float dotProduct = PVector.dot(velocity, normal);
        PVector newVelocity = PVector.sub(velocity, PVector.mult(normal, 2*dotProduct));
        this.velocity = newVelocity;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    /**
     * Checks if the ball is coloured, meaning non-grey
     * @return true if coloured, false otherwise
     */
    public boolean isColoured() {
        if (this.colour==0) {
            return false;
        }
        return true;
    }

    public int getColour() {
        return this.colour;
    }
}
