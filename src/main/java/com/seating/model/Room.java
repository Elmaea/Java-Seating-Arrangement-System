package com.seating.model;

public class Room {
    private String name;
    private int capacity;
    private int rows;
    private int cols;

    public Room(String name, int capacity, int rows, int cols) {
        this.name = name;
        this.capacity = capacity;
        this.rows = rows;
        this.cols = cols;
    }

    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
