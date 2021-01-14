package ducks.utils;

public class Constants {

    public static int[] bestSlanderers = {21, 41, 63, 85, 107, 204, 310, 400, 500, 606, 724, 810, 902, 949};

    public static int getBestSlanderer(int inf) {
        for (int i = 0; i < 13; i++) {
            if (bestSlanderers[i] <= inf && bestSlanderers[i+1] > inf) {
                return bestSlanderers[i];
            }
        }
        if (inf < bestSlanderers[0]) return 0;
        else return bestSlanderers[13];
    }
}
