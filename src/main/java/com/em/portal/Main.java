package com.em.portal;

import java.text.MessageFormat;

/**
 * Class to "optimize" portal positioning in the Nether Hub in Minecraft
 */

public class Main {

    static final int ERROR = 1000000000;
    private static final int MAX_COUNTER = 1000;

    public static void main(String[] args) {

        //calculateOptim(MinimizedDistance.SUM_DIST         , DistanceFelt.EUCLIDEAN_VERTICAL_WEIGHT);
        //calculateOptim(MinimizedDistance.MAX_DIST         , DistanceFelt.EUCLIDEAN_VERTICAL_WEIGHT);
        //calculateOptim(MinimizedDistance.MAX_DIST_WITH_SUM, DistanceFelt.EUCLIDEAN_VERTICAL_WEIGHT);

        calculateOptim(MinimizedDistance.MAX_DIST_WITH_SUM, DistanceFelt.EUCLIDEAN_VERTICAL_WEIGHT_ALIGNED);
        //calculateOptim(MinimizedDistance.MAX_DIST_WITH_SUM, DistanceFelt.EUCLIDEAN_VERTICAL_WEIGHT_ALIGNED_PLUS);

    }

    private static void calculateOptim(MinimizedDistance minimizedDistance, DistanceFelt distanceFelt) {
        Portal[] portals = {
                new Portal(248, 67, -7),
                new Portal(795, 81, -218),
                new Portal(335, 34, 144),
                new Portal(227, 53, 145),
                new Portal(199, 42, 132),
                new Portal(173, 6, 178)
        };


        double dist;
        int cpt = 0;
        int lastCpt = 0;

        while (true) {

            dist = calcDistance(portals, minimizedDistance, distanceFelt);
            Block nether = null;
            int i = (int) Math.floor(Math.random() * portals.length);

            int xn = portals[i].realNether.getX();
            int yn = portals[i].realNether.getY();
            int zn = portals[i].realNether.getZ();

            int STEP = 1;

            for (int x = xn - STEP; x <= xn + STEP; x++) {
                for (int y = yn - STEP; y <= yn + STEP; y++) {
                    for (int z = zn - STEP; z <= zn + STEP; z++) {
                        if ((x != xn) || (y != yn) || (z != zn)) {
                            portals[i].realNether.setX(x);
                            portals[i].realNether.setY(y);
                            portals[i].realNether.setZ(z);
                            if (calcDistance(portals, minimizedDistance, distanceFelt) < dist) {
                                dist = calcDistance(portals, minimizedDistance, distanceFelt);
                                nether = new Block(x, y, z);
                            }
                        }

                    }
                }
            }

            if (nether != null) {
                //System.out.println(cpt + "\t " + dist);
                portals[i].realNether = nether;
                lastCpt = cpt;
            } else {
                portals[i].realNether.setX(xn);
                portals[i].realNether.setY(yn);
                portals[i].realNether.setZ(zn);
            }

            if (cpt++ > MAX_COUNTER) {
                if (lastCpt > 0.9 * cpt) {
                    System.err.println("BE CAREFUL, counter max seems to be too low");
                    System.err.println("-------------------------------------------");
                }
                break;
            }
        }

        System.out.println();
        System.out.println(minimizedDistance+" - "+distanceFelt);
        for (Portal portal : portals) {
            System.out.println(portal);
        }
        System.out.println();
        for (Portal portal : portals) {
            System.out.println(
                    String.format("%d\t%d\t%d",
                            portal.getRealNether()
                                  .getX(),
                            portal.getRealNether()
                                  .getY(),
                            portal.getRealNether()
                                  .getZ()));
        }
        //System.out.println();
        //System.out.println(calcDistance(portals, minimizedDistance, distanceFelt, false));
    }

    private static double calcDistance(Portal[] portals, MinimizedDistance minimizedDistance, DistanceFelt distanceFelt) {
        return calcDistance(portals, minimizedDistance, distanceFelt, false);
    }

    private static double calcDistance(Portal[] portals, MinimizedDistance minimizedDistance, DistanceFelt distanceFelt, boolean debug) {
        double distTotal = 0;

        switch (minimizedDistance) {
            case SUM_DIST:
                // Calc sum of inter portal distance
                for (Portal portal1 : portals) {
                    distTotal += portal1.calDistAreOk(debug);
                    distTotal += portal1.calcDistOverOk(portals, debug);
                    distTotal += portal1.calcDistNetherOk(portals, debug);

                    for (Portal portal2 : portals) {
                        distTotal += portal1.getRealNether()
                                            .distFelt(portal2.getRealNether(), distanceFelt);
                    }
                }
                break;
            case MAX_DIST:
                // Calc max of inter portal distance
                double maxDist = 0;
                for (Portal portal1 : portals) {
                    distTotal += portal1.calDistAreOk(debug);
                    distTotal += portal1.calcDistOverOk(portals, debug);
                    distTotal += portal1.calcDistNetherOk(portals, debug);

                    for (Portal portal2 : portals) {
                        if (maxDist < portal1.getRealNether()
                                             .distFelt(portal2.getRealNether(), distanceFelt)) {
                            maxDist = portal1.getRealNether()
                                             .distFelt(portal2.getRealNether(), distanceFelt);
                        }
                    }
                    distTotal += maxDist;
                }
                break;
            case MAX_DIST_WITH_SUM:
                distTotal += 5 * calcDistance(portals, MinimizedDistance.MAX_DIST, distanceFelt, debug);
                distTotal += 1 * calcDistance(portals, MinimizedDistance.SUM_DIST, distanceFelt, debug);
                distTotal = distTotal / 6;
                break;
        }


        return distTotal;
    }


    enum MinimizedDistance {SUM_DIST, MAX_DIST, MAX_DIST_WITH_SUM}

    enum DistanceFelt {EUCLIDEAN, EUCLIDEAN_VERTICAL_WEIGHT, EUCLIDEAN_VERTICAL_WEIGHT_ALIGNED, EUCLIDEAN_VERTICAL_WEIGHT_ALIGNED_PLUS}


    static class Portal {
        Block realOver;
        Block realNether;

        public Portal(int xo, int yo, int zo) {
            realOver = new Block(xo, yo, zo);
            realNether = realOver.toNether();
        }

        @Override
        public String toString() {
            return realOver + "\t-> " + realNether;
        }

        Block getRealOver() {
            return realOver;
        }

        Block getRealNether() {
            return realNether;
        }

        double calDistAreOk(boolean debug) {
            // dist should be < 128
            double dist = 0;

            if (debug) {
                System.out.println("calDistAreOk over   : " + Math.max(0, realOver.distPortal(realNether.toOver()) - 127));
                System.out.println("calDistAreOk nether : " + Math.max(0, realNether.distPortal(realOver.toNether()) - 127));
            }

            dist += ERROR * Math.max(0, realOver.distPortal(realNether.toOver()) - 127);
            dist += ERROR * Math.max(0, realNether.distPortal(realOver.toNether()) - 127);

            return dist;
        }

        double calcDistOverOk(Portal[] portals, boolean debug) {
            double dist = realNether.toOver()
                                    .distEuclidean(realOver) + 2;
            if (debug) {
                System.out.println("calcDistOverOk dist   : " + dist);
            }
            double distRet = 0;

            for (Portal portal : portals) {
                if (portal.equals(this)) {
                    continue;
                }
                if (debug) {
                    System.out.println("calcDistOverOk        : " + realNether.toOver()
                                                                              .distEuclidean(portal.getRealOver()));
                }
                if (realNether.toOver()
                              .distEuclidean(portal.getRealOver()) <= dist) {
                    distRet += ERROR * Math.abs(1 + dist - realNether.toOver()
                                                                     .distEuclidean(portal.getRealOver()));
                }
            }

            return distRet;

        }

        double calcDistNetherOk(Portal[] portals, boolean debug) {
            double dist = realOver.toNether()
                                  .distEuclidean(realNether) + 2;
            if (debug) {
                System.out.println("calcDistNetherOk dist   : " + realOver.toNether()
                                                                          .distEuclidean(realNether) + " -> " + realOver.toNether() + " " + realNether);
                System.out.println("calcDistNetherOk dist   : " + dist);
            }
            double distRet = 0;

            for (Portal portal : portals) {
                if (portal.equals(this)) {
                    continue;
                }
                if (debug) {
                    System.out.println("calcDistNetherOk        : " + getRealOver().toNether().distEuclidean(portal.getRealNether()));
                }
                if (getRealOver().toNether().distEuclidean(portal.getRealNether()) <= dist) {
                    distRet += ERROR * Math.abs(1 + dist - getRealOver().toNether().distEuclidean(portal.getRealNether()));
                }
            }

            return distRet;

        }


    }

    static class Block {
        private int x;
        private int y;
        private int z;

        Block(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            //return MessageFormat.format("{0,number,####}, {1,number,####}, {2,number,####}", x, y, z);
            return String.format("%4d, %4d, %4d", x, y, z);
        }

        void setX(int x) {
            this.x = x;
        }

        void setY(int y) {
            this.y = y;
        }

        void setZ(int z) {
            this.z = z;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        int getZ() {
            return z;
        }

        Block toNether() {
            return new Block((int) Math.floor(x / 8.0), y, (int) Math.floor(z / 8.0));
        }

        Block toOver() {
            return new Block(x * 8, y, z * 8);
        }

        double distEuclidean(Block b2) {
            return Math.sqrt(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getY() - this.getY(), 2) + Math.pow(b2.getZ() - this.getZ(), 2));
        }

        double distPortal(Block b2) {
            return Math.max(Math.abs(b2.getX() - this.getX()), Math.abs(b2.getZ() - this.getZ()));
        }

        double distFelt(Block b2, DistanceFelt distanceFelt) {
            double dist = 0;
            switch (distanceFelt) {
                case EUCLIDEAN:
                    dist = distEuclidean(b2);
                    break;
                case EUCLIDEAN_VERTICAL_WEIGHT:
                    dist = Math.sqrt(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getY() - this.getY(), 8) + Math.pow(b2.getZ() - this.getZ(), 2));
                    break;
                case EUCLIDEAN_VERTICAL_WEIGHT_ALIGNED:
                    dist = Math.sqrt(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getY() - this.getY(), 8) + Math.pow(b2.getZ() - this.getZ(), 2));
                    dist += 800 * Math.min(Math.abs(b2.getX() - this.getX()), Math.abs(b2.getZ() - this.getZ()));
                    break;
                case EUCLIDEAN_VERTICAL_WEIGHT_ALIGNED_PLUS:
                    dist = Math.sqrt(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getY() - this.getY(), 8) + Math.pow(b2.getZ() - this.getZ(), 2));
                    dist += 800000 * Math.min(Math.abs(b2.getX() - this.getX()), Math.abs(b2.getZ() - this.getZ()));
                    break;
            }

            // add dist to be y close to 42
            dist+=1000*Math.pow(42 - this.getY(), 8);
            dist+=1000*Math.pow(42 - b2.getY(), 8);

            // add min distance to DIST_MIN
            int DIST_MIN = 8;
            if (Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getZ() - this.getZ(), 2) != 0) {
                dist+=1000*(DIST_MIN-Math.min(DIST_MIN, Math.sqrt(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getZ() - this.getZ(), 2))));
                //System.out.println(this+" - "+b2+" -> "+(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getZ() - this.getZ(), 2))+" ("+(Math.min(DIST_MIN, Math.sqrt(Math.pow(b2.getX() - this.getX(), 2) + Math.pow(b2.getZ() - this.getZ(), 2)))-DIST_MIN)+")");
            }

            return dist;
        }


    }
}
