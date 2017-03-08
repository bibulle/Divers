package com.em.mine;

public class Point {

    int x;
    int y;
    int biome;

    static int maxX = 0;
    static int minX = 0;
    static int maxY = 0;
    static int minY = 0;

    public Point(int x, int y, int biome) {
        this.x = x;
        this.y = y;
        this.biome = biome;

        /*if (x > maxX) {
            maxX = x;
            System.out.println("maxX "+maxX);
        }
        if (x < minX) {
            minX = x;
            System.out.println("minX "+minX);
        }
        if (y > maxY) {
            maxY = y;
            System.out.println("maxY "+maxY);
        }
        if (x < minY) {
            minY = y;
            System.out.println("minY "+minY);
        }*/

//        if (y == 511) {
//            System.out.println(x+" "+y+" "+biome+" "+Math.floorDiv(x, 4)+" "+Math.floorDiv(y, 4));
//        }
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return (x+"x"+y).hashCode();
    }


    public Chunck getChunck() {
//        if (y == 511) {
//            System.out.println(new com.em.mine.Chunck(Math.floorDiv(x, 4), Math.floorDiv(y, 4), biome));
//        }
        return new Chunck(Math.floorDiv(x, 4), Math.floorDiv(y, 4), biome);
    }
}
