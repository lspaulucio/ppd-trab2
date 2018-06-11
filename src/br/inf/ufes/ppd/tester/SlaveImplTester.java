package br.inf.ufes.ppd.tester;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import java.rmi.RemoteException;
import java.util.*;

/** Slave implementation
 *
 * @author Leonardo Santos Paulucio
 */

public class SlaveImplTester implements Slave {

    private UUID uid;

    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }
    
    /**
     * Implementação do startSubAttack para medir overhead.
     * O escravo irá retornar imediatamente o último indice
     * @param ciphertext Arquivo criptografado.
     * @param knowntext Trecho conhecido do arquivo criptografado.
     * @param initialwordindex Índice inicial do sub ataque.
     * @param finalwordindex Índice final do sub ataque.
     * @param attackNumber Número do sub ataque
     * @param callbackinterface  Interface do mestre para chamada de
     * checkpoint e foundGuess.
     * @see br.inf.ufes.ppd.implementation.SubAttackService
     */
    @Override
    public void startSubAttack(byte[] ciphertext, 
                               byte[] knowntext, 
                               long initialwordindex, 
                               long finalwordindex, 
                               int attackNumber, 
                               SlaveManager callbackinterface) 
        throws RemoteException {

        callbackinterface.checkpoint(getUid(), attackNumber, finalwordindex);
    }         
}
