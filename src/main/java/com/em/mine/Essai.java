package com.em.mine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Essai {

    public static void main(String[] args) {

        try {

            Pattern linePattern = Pattern.compile("- ([0-9-]*), ([0-9-]*), ([0-9-]*), ([0-9-]*) -> ([0-9-]*).*");

            final HashMap<Chunck, Chunck> chuncks = new HashMap<>();
            Set<Point> points = new HashSet<>();

            // read the input file
            FileInputStream inputStream = null;
            Scanner sc = null;
            try {
                inputStream = new FileInputStream("./myfile.txt");
                sc = new Scanner(inputStream, "UTF-8");
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();

                    Matcher matcher = linePattern.matcher(line);
                    if (matcher.matches()) {

                        int x = Integer.parseInt(matcher.group(1))+Integer.parseInt(matcher.group(3));
                        int y = Integer.parseInt(matcher.group(2))+Integer.parseInt(matcher.group(4));
                        int biome = Integer.parseInt(matcher.group(5));

                        Point p = new Point(x, y, biome);

//                        if (y == 511) {
//                            System.out.println("- "+points.size());
//                        }
                        points.add(p);
//                        if (y == 511) {
//                            System.out.println("+ "+points.size());
//                        }

                    } else {
                        //System.err.println("'"+line+"'");
                    }
                }
                // note that Scanner suppresses exceptions
                if (sc.ioException() != null) {
                    throw sc.ioException();
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (sc != null) {
                    sc.close();
                }
            }

            System.out.println(points.size());

            points.forEach((p) -> {
                Chunck c = p.getChunck();
//                if (p.y == 511) {
//                    System.out.println("= "+c+" ("+chuncks.get(c)+")");
//                    System.out.println(chuncks.size());
//                }
                if (chuncks.get(c) != null) {
                    chuncks.get(c).addBiome(p.biome);
                } else {
                    chuncks.put(c, c);
                }
//                if (p.y == 511) {
//                    System.out.println("= "+c);
//                    System.out.println(chuncks.size());
//                }

            });

            System.out.println(chuncks.size());

            chuncks.keySet().forEach((c1) -> {
                System.out.println(c1.x+"\t"+c1.y+"\t"+c1.getMaxBiome());
            });


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
