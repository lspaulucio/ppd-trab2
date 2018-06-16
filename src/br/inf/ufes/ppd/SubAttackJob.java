/** Attack Class.
 *
 * @author Leonardo Santos Paulucio
 */

package br.inf.ufes.ppd;

import java.io.Serializable;

public class SubAttackJob implements Serializable{

    private final int attackID;
    private final long initialIndex;
    private final long finalIndex;
    private final byte[] cypherText;
    private final byte[] knowText;

    public SubAttackJob(int attackID, long initialIndex, long finalIndex, byte[] cypherText, byte[] knowText) {
        this.attackID = attackID;
        this.initialIndex = initialIndex;
        this.finalIndex = finalIndex;
        this.cypherText = cypherText;
        this.knowText = knowText;
    }
    
    public int getAttackID() {
        return attackID;
    }

    public long getInitialIndex() {
        return initialIndex;
    }

    public long getFinalIndex() {
        return finalIndex;
    }

    public byte[] getCypherText() {
        return cypherText;
    }

    public byte[] getKnowText() {
        return knowText;
    }
}
