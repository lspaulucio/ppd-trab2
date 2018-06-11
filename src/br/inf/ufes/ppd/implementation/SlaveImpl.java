package br.inf.ufes.ppd.implementation;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.Crypto;
import br.inf.ufes.ppd.utils.FileTools;
import java.rmi.RemoteException;
import java.util.*;

/** Slave implementation
 *
 * @author Leonardo Santos Paulucio
 */

public class SlaveImpl implements Slave {

    private static List<String> keys = new ArrayList<String>();
    private UUID uid;

    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }
    
    /**
     * Realiza a leitura do dicionario.
     * @param filename Nome do arquivo de dicionario.
     */ 
    public void readDictionary(String filename) {

        keys = FileTools.readDictionary(filename);
    }

    /**
     * Implementação do startSubAttack
     * @param ciphertext Arquivo criptografado.
     * @param knowntext Trecho conhecido do arquivo criptografado.
     * @param initialwordindex Índice inicial do sub ataque.
     * @param finalwordindex Índice final do sub ataque.
     * @param attackNumber Número do sub ataque
     * @param callbackinterface  Interface do mestre para chamada de
     * checkpoint e foundGuess.
     * @see br.inf.ufes.ppd.services.SubAttackService
     */
    @Override
    public void startSubAttack(byte[] ciphertext, 
                               byte[] knowntext, 
                               long initialwordindex, 
                               long finalwordindex, 
                               int attackNumber, 
                               SlaveManager callbackinterface) 
        throws RemoteException {

        Thread subAttack = new SubAttackService(ciphertext, knowntext, initialwordindex, 
                                                finalwordindex, attackNumber, callbackinterface);
        
        subAttack.start();
    }
    
    public class SubAttackService extends Thread {
    
        private final byte[] encryptedText;
        private final byte[] knownText;
        private long currentIndex;
        private final long finalIndex;
        private final int subAttackID;
        private final SlaveManager smRef;

        /**
         * Construtor do serviço de sub ataque.
         * @param ciphertext Arquivo criptografado.
         * @param knowntext Trecho conhecido do arquivo criptografado.
         * @param initialwordindex Índice inicial do sub ataque.
         * @param finalwordindex Índice final do sub ataque.
         * @param attackNumber Número do sub ataque
         * @param callbackinterface  Interface do mestre para chamada de
         * checkpoint e foundGuess.
         * @see CheckPointTask
         * @see br.inf.ufes.ppd.implementation.RebindService
         */

        public SubAttackService(byte[] ciphertext, 
                                byte[] knowntext, 
                                long initialwordindex,
                                long finalwordindex, 
                                int attackNumber, 
                                SlaveManager callbackinterface){
            
            this.encryptedText = ciphertext;
            this.knownText = knowntext;
            this.currentIndex = initialwordindex;
            this.finalIndex = finalwordindex;
            this.subAttackID = attackNumber;
            this.smRef = callbackinterface;
        }

        @Override
        public void run()
        {

            System.out.println("New SubAttack: " + subAttackID);
            //Making a timer to notify master about currentIndex
            Timer timer = new Timer();

            CheckPointTask checkTask = new CheckPointTask();
            timer.scheduleAtFixedRate(checkTask, 0, Configurations.CHECKPOINT_TIME);  // 0 = delay, CHECKPOINT_TIME = frequence

            //Subattack execution
            try{
                for (; currentIndex <= finalIndex; currentIndex++) {

                    try {
                        String actualKey = keys.get((int) currentIndex); //Get current key

                        byte[] decrypted = Crypto.decrypter(actualKey.getBytes(), encryptedText);

                        //Checking if known text exists in decrypted text
                        if (Crypto.contains(knownText, decrypted)) {

                            Guess currentGuess = new Guess();
                            currentGuess.setKey(actualKey);
                            currentGuess.setMessage(decrypted);

                            smRef.foundGuess(getUid(), subAttackID, currentIndex, currentGuess);

        //                    System.out.println("Key found: " + actualKey);
                        }

                    } catch (javax.crypto.BadPaddingException e) {
                        // essa excecao e jogada quando a senha esta incorreta
                        // porem nao quer dizer que a senha esta correta se nao jogar essa excecao
                        //System.err.println("Senha " + new String(key) + " invalida.");
                    } catch (RemoteException e) {
                        System.err.println("Error subattack service:\n" + e.getMessage());
                    }
                }
            }catch(IndexOutOfBoundsException e){
//              System.out.println("");
                currentIndex++; //Adjusting index
            }

            timer.cancel(); //Closing task checkpoint
            
            currentIndex--; //Just because was incremented one more time
            
            try {
                smRef.checkpoint(getUid(), subAttackID, currentIndex); //End job. Sending last checkpoint            
                System.out.println("Final checkpoint " + currentIndex);
            }
            catch (RemoteException e){
                System.err.println("Subattack callback fail:\n" + e.getMessage());                
            }
            System.out.println("End subattack: " + subAttackID);
        }
        
        /**
         * Classe interna responsável por executar o serviço de checkpoint.
         * @author Leonardo Santos Paulucio
        */
        private class CheckPointTask extends TimerTask{

            @Override
            public void run() {
                try{
                    //Notify master about current index
                    smRef.checkpoint(getUid(), subAttackID, currentIndex);
                    System.out.println("Checkpoint " + currentIndex);
                }
                catch (RemoteException e){
                    System.err.println("Master down:\n" + e.getMessage());
                }
            }
        }
    }
}
