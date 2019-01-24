package net.namekdev.entity_tracker.utils.sample;

public class DeepArray {
    public int[][][][] arr = new int[][][][] {
        // left
        new int[][][] {
            new int[][] {
                new int[] {
                    123,
                    124
                }
            }
        },

        // right
        new int[][][] {
            new int[][] {
                new int[] {
                    124
                }
            },
            null
        }
    };
}
