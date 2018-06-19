package br.inf.ufes.ppd.implementation;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.SubAttackJob;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.*;

/** Master Implementation.
 *
 * @author Leonardo Santos Paulucio
 */

public class MasterImpl implements Master {

    private final Map<Integer, List<Guess>> guessList;
    private final Map<Integer, Integer> attackMap;
    private final Map<Integer, AttackControl> attacksList;
    private int attackNumber = 0;
    private int subAttackNumber = 0;
    private Queue subAttacksQueue;
    private JMSContext context;
    private JMSConsumer guessesConsumer;
    private JMSProducer subAttackProducer;
   
    public MasterImpl(){
        guessList = new HashMap<>();
        attackMap = new HashMap<>();
        attacksList = new HashMap<>();
    }
    
    public synchronized int getAttackNumber(){
        return attackNumber++;
    }
    
    public synchronized int getSubAttackNumber(){
        return subAttackNumber++;
    }
    
    public void setConsumer(JMSConsumer consumer) {
        this.guessesConsumer = consumer;
    }

    public void setProducer(JMSProducer producer) {
        this.subAttackProducer = producer;
    }

    public void setSubAttacksQueue(Queue subAttacksQueue) {
        this.subAttacksQueue = subAttacksQueue;
    }
    
    public void setContext(JMSContext context) {
        this.context = context;
    }

    /////////Attacker interfaces
    
    /**
     * Inicia um ataque. Chamado pelo cliente.
     * @param ciphertext Mensagem criptografada.
     * @param knowntext  Trecho conhecido da mensagem.
     * @return Vetor de guess encontrados
     * @throws java.rmi.RemoteException
     */ 
    @Override
    public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
        
        int attackID = getAttackNumber();
        AttackControl newAttack = new AttackControl(ciphertext, knowntext);
        
        System.out.println("New attack request");
        
        //Creating a guess list for this attack
        synchronized(guessList){
            guessList.put(attackID, new ArrayList<>());
        }

        //Creating an attackControl for this attack
        synchronized(attacksList){
            attacksList.put(attackID, newAttack);
        }
        
        Thread attack = new AttackTask(attackID, ciphertext, knowntext);
        attack.start();
        
        try {
            attack.join();
        } catch (InterruptedException ex) {
            System.err.println("Attack error:\n " + ex.getMessage());
        }
        
        //Waiting end job
        while(!newAttack.isDone()){
            try{
                synchronized(newAttack){
                    newAttack.wait();
                }
            }
            catch(InterruptedException e){
                System.err.println("Waiting end job error:\n" + e.getMessage());
            }
        }
        
        System.out.println("\nEnd attack number: " + attackID);
        
        //Return guess vector
        Guess[] guess;
        
        synchronized(guessList)
        {
            guess = getGuessVector(guessList.get(attackID));
            
            guessList.remove(attackID);  //Removing guess from list
        }

        //removing subattack mapping from current attack
        synchronized(attackMap){
            for (Integer subAttackID : attacksList.get(attackID).getSubAttacks().keySet()) {
                attackMap.remove(subAttackID);
            }
        }
        
        //removing current attack control
        synchronized(attacksList){
            attacksList.remove(attackID);
        }
        
        return guess;
    }

    public void startGuessTask(){
        Thread checker = new GuessesChecker();
        checker.start();
    }
    
    /**
     * Gera um vetor de Guess a partir de uma lista de Guess.
     * @param g  Lista de guess.
     * @return Vetor contendo os guess da lista.
     */ 
    public Guess[] getGuessVector(List<Guess> g){
        
        Guess[] guessVector = new Guess[g.size()];
        
        for(int i = 0; i < g.size(); i++){
            guessVector[i] = g.get(i);
        }
        
        return guessVector;
    }
    
    /**
     * Tarefa de Ataque.
     * Responsável executar o ataque.
     */ 
    
    public class AttackTask extends Thread{

        private final int attackID;
        private final byte[] encriptedText;
        private final byte[] knownText;
 
        public AttackTask(int attackNumber, byte[] ciphertext, byte[] knowntext){
            
            this.attackID = attackNumber;
            this.encriptedText = ciphertext;
            this.knownText = knowntext;
            
        }
        
        @Override
        public void run() {
            
            long INDEX_DIVISION = 10000;
            long dictionarySize = Configurations.DICTIONARY_SIZE; 
            long indexDivision = INDEX_DIVISION;
            long initialIndex = 0; 
            long finalIndex = indexDivision + (dictionarySize % INDEX_DIVISION);
            
            System.out.println("AttackNumber: " + attackID);
            
            while(finalIndex != initialIndex){
                
                int subAttackID = getSubAttackNumber();

                synchronized(attackMap){
                    attackMap.put(subAttackID, attackID); //Adding new map subattack -> attack
                }

                try{

                    synchronized(attacksList){
                        attacksList.get(attackID).getSubAttacks().put(subAttackID, false);
                    }

                    SubAttackJob sub = new SubAttackJob(subAttackID, initialIndex, finalIndex, encriptedText, knownText);
                    
                    ObjectMessage objMessage = context.createObjectMessage(sub);
                    subAttackProducer.send((Destination) subAttacksQueue, objMessage);
                    
                    System.out.println("SubAttack " + subAttackID + " created. " + 
                                       "Index range: " + initialIndex + "/" + finalIndex);

                    initialIndex = finalIndex;
                    finalIndex += indexDivision;

                    if(finalIndex > dictionarySize)
                        finalIndex = dictionarySize;            

                }
                catch(Exception e){
                    System.err.println("Attack Task error \n" + e.getMessage());
                }
            }//jobs created
        }
    }
    /**
     * Estrutura de Controle de Ataque. 
     * Responsável por gerenciar um ataque.
     */ 

    public class AttackControl {

        private Map<Integer, Boolean> subAttacks;
        private final byte[] cipherMessage;
        private final byte[] knownText;
        private final double startTime;
        private boolean done;

        public byte[] getCipherMessage() {
            return cipherMessage;
        }

        public byte[] getKnownText() {
            return knownText;
        }

        public double getStartTime() {
            return startTime;
        }

         public Map<Integer, Boolean> getSubAttacks() {
             return subAttacks;
         }

         public void setSubAttacks(Map<Integer, Boolean> subAttacks) {
             this.subAttacks = subAttacks;
         }

        public synchronized boolean isDone() {
            boolean finished = true;

            synchronized(subAttacks){

                for(Boolean subAttackStatus : subAttacks.values()) {
                    finished &= subAttackStatus;
                }
            }

            setDone(finished);

            return finished;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public AttackControl(byte[] cipher, byte[] known)
        {
            this.cipherMessage = cipher;
            this.knownText = known;
            this.startTime = System.nanoTime();
            this.done = false;
            this.subAttacks = new HashMap<>();
        }
    }

    /**
     * Estrutura responsavel por pegar os guesses da fila. 
     */     
    public class GuessesChecker extends Thread{
        
        @Override
        public void run(){
            
            while(true){
            
                Message msg = guessesConsumer.receive();

                if(msg instanceof ObjectMessage){

                    try{
                        
                        ObjectMessage obj = (ObjectMessage) msg;
                        Guess newGuess = (Guess) obj.getObject();

                        int subAttackID = newGuess.getSubAttackID();
                        int attackID = attackMap.get(subAttackID);
                        
                        System.out.println("\nNew message received from " + newGuess.getDiscoverer());
                        System.out.println("Attack: " + attackID);
                        
                        //LIMPA FILA
                        if(attackMap.get(subAttackID) == null)
                            continue;
                        

                        if(!newGuess.isDone()){

                            System.out.println("Guess message. Key founded: " + newGuess.getKey());

                            guessList.get(attackID).add(newGuess);

                            synchronized(attacksList){
                                attacksList.get(attackID).getSubAttacks().put(subAttackID, true);
                            }
                        }
                        else{
                            synchronized(attacksList){
                                attacksList.get(attackID).getSubAttacks().put(subAttackID, true);
                            }
                            
                            AttackControl attack = attacksList.get(attackID);
                            
                            synchronized(attack){
                                attack.notifyAll();
                            }
                            
                            double elapsedTime = (System.nanoTime() - attack.getStartTime())/1000000000;
                            System.out.println("SubAttack " + subAttackID + " finished");
                            System.out.println("Elapsed Time: " + elapsedTime);
                        }
                    }
                    catch(JMSException e){
                        System.err.println("Consume error \n" + e.getMessage());
                    }
                }
            }
        }
    }   
}