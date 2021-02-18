package io.moco.engine;

/**
 * Mutation Generator to create mutants.
 *
 * @author Tran Phan
 * @since 1.0
 */
public class DummyForTesting {
    public void dummy() {
        int a = 1;
        int b = 5 + 4;
        int c = 10/2;
        int d = 2*4;
        int e = a + b;
        if (e == 10) {
            int f = 4 - 9;
        }
    }
}
