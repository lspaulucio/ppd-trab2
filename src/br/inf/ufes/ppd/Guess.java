package br.inf.ufes.ppd;

/**
 * Guess.java
 */

import java.io.Serializable;

public class Guess implements Serializable {
    private String key;     // chave candidata
    private byte[] message; // mensagem decriptografada com a chave candidata
    private String discoverer;
    private boolean done;   
    private int subAttackID;

    public Guess(String key, byte[] decrypted, String slaveName, int attackID){
        this.key = key;
        this.message = decrypted;
        this.discoverer = slaveName;
        this.subAttackID = attackID;
        this.done = false;
    }
    
    public String getDiscoverer() {
        return discoverer;
    }

    public void setDiscoverer(String discoverer) {
        this.discoverer = discoverer;
    }    
    
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getSubAttackID() {
        return subAttackID;
    }

    public void setSubAttackID(int subAttackID) {
        this.subAttackID = subAttackID;
    }

    public String getKey() {
            return key;
    }
    public void setKey(String key) {
            this.key = key;
    }
    public byte[] getMessage() {
            return message;
    }
    public void setMessage(byte[] message) {
            this.message = message;
    }

}
