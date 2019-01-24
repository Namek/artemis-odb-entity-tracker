package net.namekdev.entity_tracker.utils.sample;

public class DeepArrayImplicit {
    public Object arr = new int[][][][] {
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