package com.em.mine;

import java.util.Hashtable;

public class Chunck {

    int x;
    int y;
    Hashtable<Integer, Integer> biomes = new Hashtable<>();

    public Chunck(int x, int y, int biome) {
        this.x = x;
        this.y = y;
        addBiome(biome);
   }

   public void addBiome(int biome) {
        if (biomes.get(biome) != null) {
            biomes.put(biome, biomes.get(biome)+1);
        } else {
            biomes.put(biome, 1);
        }
       //System.out.println(this);
   }

   public int getMaxBiome() {
        int cpt = 0;

        int maxBiome = 0;
        int maxCount = -1;

       for (Integer biome: biomes.keySet()) {
           if (biomes.get(biome) > maxCount) {
               maxCount = biomes.get(biome);
               maxBiome = biome;
           }
           cpt += biomes.get(biome);

       }

       if (cpt > 16) {
           System.err.println(this);
       }

       return   maxBiome;
   }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return (x+"x"+y).hashCode();
    }

    @Override
    public String toString() {
        return x+"\t"+y+"\t"+biomes;
    }
}
