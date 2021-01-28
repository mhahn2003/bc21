package finals.utils;

public class Constants {

    public static int staleCooldown = 200;
    public static int[] bestSlanderers = {21, 41, 63, 85, 107, 130, 204, 310, 400, 500, 606, 724, 810, 902, 949};

    public static int getBestSlanderer(int inf) {
        for (int i = 0; i < 14; i++) {
            if (bestSlanderers[i] <= inf && bestSlanderers[i+1] > inf) {
                return bestSlanderers[i];
            }
        }
        if (inf < bestSlanderers[0]) return 0;
        else return bestSlanderers[14];
    }
}
