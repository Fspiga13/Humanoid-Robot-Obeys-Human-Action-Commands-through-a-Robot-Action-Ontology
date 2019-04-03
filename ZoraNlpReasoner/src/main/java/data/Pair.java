/**
 * This class allows the creation of objects pairs.
 */

package main.java.data;

public class Pair<L,R> {

    private final L left;
    private final R right;

    /***
     * Builder of a pair of objects
     * @param left left object
     * @param right right object
     */
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /***
     *
     * @return left object
     */
    public L getLeft() {
        return left;
    }

    /***
     *
     * @return right object
     */
    public R getRight() {
        return right;
    }
}