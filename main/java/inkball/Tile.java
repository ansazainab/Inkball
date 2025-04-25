package inkball;

import processing.core.PImage;
import processing.core.PVector;

public class Tile {
    
    protected int x;
    protected int y;

    /**
     * Constructor that sets the position of the tile.
     * @param x the x coordinate of the tile
     * @param y the y coordinate of the tile
     */
    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Draws the tile image for a general Tile object.
     * @param app an object of the App class
     */
    public void draw(App app) {
        PImage tile = app.getSprite("tile");
        app.image(tile, x*App.CELLSIZE, y*App.CELLSIZE+App.TOPBAR);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}


class Wall extends Tile {
    
    private int colour;
    private static final double size = 32;
    public int hits = 0;

    /**
     * Constructor that sets the position of the wall in pixels. It also sets the colour of the wall.
     * @param x the x position of the wall on the board
     * @param y the y position of the wall on the board
     * @param colour the colour of the wall
     */
    public Wall(int x, int y, int colour) {
        super(x*App.CELLSIZE, y*App.CELLSIZE+App.TOPBAR);
        this.colour = colour;
    }

    /**
     * Draws the relevant wall image based on the colour of the wall and how damaged it is.
     * @param app an object of the App class
     */
    @Override
    public void draw(App app) {
        PImage tile;
        if (hits==2) {
            tile = app.getSprite("broken"+String.valueOf(colour));
        }
        else {
            tile = app.getSprite("wall"+String.valueOf(colour));
        }
        app.image(tile, x, y);
    }

    /**
     * Checks if the wall is coloured, meaning non-grey.
     * @return true if it is coloured, false otherwise
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

    public double getLeft() {
        return x; //Returns the left x coordinate of the wall
    }

    public double getRight() {
        return x + size; //Returns the right x coordinate of the wall
    }

    public double getTop() {
        return y; //Returns the top y coordinate of the wall
    }

    public double getBottom() {
        return y + size; //Returns the bottom y coordinate of the wall
    }
}


class Spawner extends Tile {

    /**
     * Constructor that sets the position of the spawner.
     * @param x the x position of the spawner
     * @param y the y position of the spawner
     */
    public Spawner(int x, int y) {
        super(x, y);
    }

    /**
     * Draws the spawner image.
     * @param app an object of the App class
     */
    @Override
    public void draw(App app) {
        PImage tile = app.getSprite("entrypoint");
        app.image(tile, x*App.CELLSIZE, y*App.CELLSIZE+App.TOPBAR);
    }
}


class Hole extends Tile {
    
    private int colour;
    public PVector centrePosition = new PVector();

    /**
     * Constructor that sets the position and colour of a hole. It also sets its centre position.
     * @param x the x position of the hole
     * @param y the y position of the hole
     * @param colour the colour of the hole
     */
    public Hole(int x, int y, int colour) {
        super(x, y);
        this.colour = colour;
        centrePosition.set(x*App.CELLSIZE+32, y*App.CELLSIZE+App.TOPBAR+32);
    }

    /**
     * Draws the relevant hole image based on the colour of the hole.
     * @param app an object of the App class
     */
    @Override
    public void draw(App app) {
        PImage tile = app.getSprite("hole"+String.valueOf(colour));
        app.image(tile, x*App.CELLSIZE, y*App.CELLSIZE+App.TOPBAR);
    }

    /**
     * Checks if the hole is coloured, meaning non-grey.
     * @return true if it is coloured, false otherwise
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