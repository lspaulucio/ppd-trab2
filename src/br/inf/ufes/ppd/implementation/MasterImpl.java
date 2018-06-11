package br.inf.ufes.ppd.implementation;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/** Master Implementation.
 *
 * @author Leonardo Santos Paulucio
 */

public class MasterImpl implements Master {

    private Map<UUID, SlaveControl> slavesList = new HashMap<>();
    private Map<Integer, List<Guess>> guessList = new HashMap<>();
    private Map<Integer, Integer> attackMap = new HashMap<>();
    private Map<Integer, AttackControl> attacksList = new HashMap<>();
    
    private int attackNumber = 0;
    private int subAttackNumber = 0;
    
    public MasterImpl(){
        Timer t = new Timer();
        MonitoringService ms = new MonitoringService(this);
        t.scheduleAtFixedRate(ms, 0, Configurations.TIMEOUT);
    }
    
    public SubAttackControl getSubAttackControl(int subAttackID){
        int mainAttackID;
        SubAttackControl sub;
        
        synchronized(attackMap){
            mainAttackID = attackMap.get(subAttackID);
        }
        
        synchronized(attacksList){
            sub = attacksList.get(mainAttackID).getSubAttacksMap().get(subAttackID);
        }
        return sub;
    }
    
    public boolean hasAttack(){
        int numberAttacks = 0;
        
        synchronized(attacksList){
            for (AttackControl attack : attacksList.values()) {
                if(!attack.isDone())
                    numberAttacks++;
            }
        }
        return numberAttacks > 0;
    }
    
    public synchronized int getAttackNumber(){
        return attackNumber++;
    }
    
    public synchronized int getSubAttackNumber(){
        return subAttackNumber++;
    }
        
    //SlaveManager interfaces
    
    /**
     * Adiciona um escravo na lista.
     * @param s Referência para o escravo.
     * @param slaveName Nome do escravo.
     * @param slavekey  Identificador único do escravo.
     */
    @Override
    public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
        
        //Checking if slave is already registered
        synchronized (slavesList) {
            if (!slavesList.containsKey(slavekey)) {
                SlaveControl sc = new SlaveControl(s, slaveName);
                slavesList.put(slavekey, sc);
                System.out.println("Slave: " + slaveName + " foi adicionado");
            }
        }
//        else{
//            System.out.println("Client already exists!");
//        }
    }

    /**
     * Remove um escravo da lista.
     * @param slaveKey  Identificador único do escravo que sera removido.
     */
    @Override
    public void removeSlave(UUID slaveKey) throws RemoteException {
        synchronized (slavesList) {
            slavesList.remove(slaveKey);
        }
    }
    
    /**
     * Guess encontrado. Chamado pelo escravo ao encontrar um guess.
     * @param slaveKey  Identificador único do escravo.
     * @param subAttackNumber Número do sub ataque.
     * @param currentindex Índice atual do ataque.
     * @param currentguess Guess encontrado.
     */ 
    @Override
    public void foundGuess(UUID slaveKey, int subAttackNumber, long currentindex, Guess currentguess) throws RemoteException {
        
        String slaveName; 
        int attackID;
        
        synchronized(slavesList){
            slaveName = slavesList.get(slaveKey).getName();
            slavesList.get(slaveKey).setTime(System.nanoTime());
        }
        
        synchronized(attackMap){
            attackID = attackMap.get(subAttackNumber);
        }
               
        synchronized(guessList){
            guessList.get(attackID).add(currentguess);
        }
        
        System.out.println("Attack Number: " + attackID);
        System.out.println("Slave: " + slaveName + " found guess: " + currentguess.getKey() + 
                           ". Current index: " + currentindex);
        System.out.println("Message: " + currentguess.getMessage() + "\n");
    }

    /**
     * Checkpoint. Chamado frequentemente pelo escravo
     * informando o andamento do ataque
     * @param slaveKey  Identificador único do escravo.
     * @param subAttackNumber Número do sub ataque.
     * @param currentindex Índice atual do ataque.
     */ 
    @Override
    public void checkpoint(UUID slaveKey, int subAttackNumber, long currentindex) throws RemoteException {
        
        int attackID;
        SlaveControl s;
        AttackControl attack;
        SubAttackControl subAttack;
        
        synchronized(attackMap){
            attackID = attackMap.get(subAttackNumber);
        }
        
        synchronized(attacksList){
            attack = attacksList.get(attackID);
        }
        
        synchronized(slavesList){
            s = slavesList.get(slaveKey);
        }

        s.setTime(System.nanoTime()); //Registering current time of checkpoint of slave
        
        synchronized(attack.getSubAttacksMap()){
            subAttack = attack.getSubAttacksMap().get(subAttackNumber);
        }
        
        subAttack.setCurrentIndex(currentindex);   //Updating currentIndex
        
        if(currentindex == subAttack.getLastIndex()){
            synchronized(attack){
                attack.notifyAll();
            }
        }
        
        double elapsedTime = (System.nanoTime() - attack.getStartTime())/1000000000;
        
        System.out.println("Slave: " + s.getName() + " checkpoint.");
        System.out.println("Attack Number: "+ attackID + " Elapsed Time: " + elapsedTime + "s");
        System.out.println("SubAttack " + subAttackNumber + " Status: " + currentindex + "/" + subAttack.getLastIndex() + "\n");
    }
    
    public void redistributionJobs(Map<UUID, SlaveControl> failedSlaves, SlaveManager smRef){
        
        Map<UUID, SlaveControl> slavesWorking;
        
         //Getting the actual working slaves
        synchronized(slavesList){
            slavesWorking = new HashMap<>(slavesList);
        }
        
        if(slavesWorking.isEmpty()){
            
            System.err.println("No slaves on. Can't redistribute jobs now. "
                                + "When some slave is registered it will receive the job");
            
            for (UUID uuid : failedSlaves.keySet()) {
                
                synchronized(slavesList){
                    slavesList.put(uuid, failedSlaves.get(uuid));
                }
            }
            
            return;
        }
        
        System.out.println("Starting redistribution");
        
        for (UUID failedSlaveID : failedSlaves.keySet()) {
            int subAttackID;
            SlaveControl s = failedSlaves.get(failedSlaveID);
            List<Integer> subAttacks = s.getSubAttackNumbersList();

            //Checking if some job of this slave didnt finish
            for (Integer subID : subAttacks) {
                
                int mainAttackID;
                AttackControl actualAttack;
                SubAttackControl sub;
                
                synchronized(attackMap)
                {
                    mainAttackID = attackMap.get(subID);
                }

                synchronized(attacksList){
                    actualAttack = attacksList.get(mainAttackID);
                }

                synchronized(actualAttack.getSubAttacksMap()){
                    sub = actualAttack.getSubAttacksMap().get(subID);
                }
                
                //if not do redistribution
                if(!sub.isDone()){

                    long indexSize = sub.getLastIndex() - sub.getCurrentIndex();
                    long division = indexSize / slavesWorking.size();
                    long startIndex = sub.getCurrentIndex(); 
                    long endIndex = sub.getCurrentIndex() + division + (indexSize % slavesWorking.size());

                    for (UUID slaveID : slavesWorking.keySet()) {

                        if(startIndex == sub.getLastIndex())
                            break;
                        
                        subAttackID = getSubAttackNumber();
                        SlaveControl sc = slavesWorking.get(slaveID);
                        Slave slRef = sc.getSlaveRef();

                        try{
                            SubAttackControl newSub = new SubAttackControl(startIndex, endIndex);

                            synchronized(slavesList){
                                slavesList.get(slaveID).getSubAttackNumbersList().add(subAttackID);
                            }

                            synchronized(attacksList){
                                actualAttack.getSubAttacksMap().put(subAttackID, newSub);
                            }

                            synchronized(attackMap){
                                attackMap.put(subAttackID, mainAttackID);
                            }

                            slRef.startSubAttack(actualAttack.getCipherMessage(), actualAttack.getKnownText(), 
                                                 startIndex, endIndex, subAttackID, smRef);

                            System.out.println("SubAttack " + subAttackID + " created. Index range: " + 
                                               startIndex + "/" + endIndex);

                            startIndex = endIndex;
                            endIndex += division;

                            if(endIndex > sub.getLastIndex())
                                endIndex = sub.getLastIndex();            
                        }
                        catch(RemoteException e){
                            System.err.println("Redistribution failed:\n" + e.getMessage());
                        }
                    }   
                }//end of jobs redistribution

                //Set done failed subattack after redistributing
                sub.setDone(true);
            }
        }
        System.out.println("End redistribution.");
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
        
        synchronized(slavesList){
            if(slavesList.isEmpty())
                throw new RemoteException("There aren't registered slaves at moment. Try again later.");
        }
        
        int attackID = getAttackNumber();
        AttackControl newAttack = new AttackControl(ciphertext, knowntext);
        
        System.out.println("New attack request. Attack number: " + attackID);
        
        //Creating a guess list for this attack
        synchronized(guessList){
            guessList.put(attackID, new ArrayList<>());
        }

        //Creating an attackControl for this attack
        synchronized(attacksList){
            attacksList.put(attackID, newAttack);
        }
        
        Thread attack = new AttackTask(this, attackID, ciphertext, knowntext);
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
                System.err.println("Sleep done job error:\n" + e.getMessage());
            }
        }
        
        System.out.println("End attack. Attack number: " + attackID);
        //Return guess vector
        Guess[] guess;
        
        synchronized(guessList)
        {
            guess = getGuessVector(guessList.get(attackID));
        }
                               
        return guess;
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

        private final SlaveManager smRef;
        private final int attackID;
        private final byte[] encriptedText;
        private final byte[] knownText;
        Map<UUID, SlaveControl> slavesWorking;
 
        public AttackTask(SlaveManager callback, int attackNumber, byte[] ciphertext, byte[] knowntext){
            
            this.smRef = callback;
            this.attackID = attackNumber;
            this.encriptedText = ciphertext;
            this.knownText = knowntext;
            
            synchronized(slavesList){
                this.slavesWorking = new HashMap<>(slavesList);
            }
        }
        
        @Override
        public void run() {
            
            Map<UUID, SlaveControl> failedSlaves = new HashMap<>();
            long dictionarySize = Configurations.DICTIONARY_SIZE; 
            long indexDivision = dictionarySize / slavesWorking.size();
            long initialIndex = 0; 
            long finalIndex = indexDivision + (dictionarySize % slavesWorking.size());
            int subAttackID;
            
            for (UUID slaveID : slavesWorking.keySet()) {
                
                SlaveControl sc = slavesWorking.get(slaveID);
                Slave slRef = sc.getSlaveRef();

                subAttackID = getSubAttackNumber();

                synchronized(attackMap){
                    attackMap.put(subAttackID, attackID); //Adding new map subattack -> attack
                }

                try{

                    SubAttackControl currentSubAttack = new SubAttackControl(initialIndex, finalIndex);

                    synchronized(attacksList){
                        //Inserting new SubAttackControl to Attack
                        attacksList.get(attackID).getSubAttacksMap().put(subAttackID, currentSubAttack);
                    }

                    synchronized(slavesList){
                        //Inserting SubAttack ID to Slave
                        slavesList.get(slaveID).getSubAttackNumbersList().add(subAttackID);
                    }

                    slRef.startSubAttack(encriptedText, knownText, initialIndex, finalIndex, subAttackID, smRef);

                    System.out.println("SubAttack " + subAttackID + " created. " + 
                                       "Index range: " + initialIndex + "/" + finalIndex);

                    initialIndex = finalIndex;
                    finalIndex += indexDivision;

                    if(finalIndex > dictionarySize)
                        finalIndex = dictionarySize;            

                }
                catch(RemoteException e){
                    //Adding slave that fail to attack
                    failedSlaves.put(slaveID, sc);

                    System.err.println("Slave failed: " + slaveID + " Name: " + sc.getName());

                    //Adjusting index for next slave
                    initialIndex = finalIndex;
                    finalIndex += indexDivision;

                    if(finalIndex > dictionarySize)
                        finalIndex = dictionarySize;            
                }
            }//end of jobs distribuition
            
            //Check if some slave had fail
            if(!failedSlaves.isEmpty()){
                
                for (UUID uid : failedSlaves.keySet()) {
                    
                    //Removing slaves that had fail
                    try{ 
                        removeSlave(uid); 
                    }
                    catch(RemoteException e){ 
                        System.err.println("Attack Task can't remove failed slave. Error:\n" + e.getMessage());
                    }
                }
                
                redistributionJobs(failedSlaves, smRef);
            }
        }
    }
    
    /**
     * Monitora os escravos.
     * Responsável por verificar se escravos ainda estão on.
     */ 
    
    public class MonitoringService extends TimerTask{
        
        SlaveManager callback;
        
        public MonitoringService(SlaveManager sm){
            this.callback = sm;
        }
        
        @Override
        public void run() {

            System.out.println("Monitoring slaves");
            
            if(hasAttack()){
                Map<UUID, SlaveControl> downSlaves = new HashMap<>();
                Map<UUID, SlaveControl> slavesCopy;
            
                long currentTime = System.nanoTime();
            
                synchronized(slavesList){
                        slavesCopy = new HashMap<>(slavesList);
                    }
                
                for (UUID id : slavesCopy.keySet()) {
                    SlaveControl slave = slavesCopy.get(id);
                    
                    double elapsedTime = (currentTime - slave.getTime())/1000000000;

                    //Checking if slave didn't send some message within 20 seg
                    if(elapsedTime > 20.0){
                        
                        List<Integer> subAttacksNumbers = slave.getSubAttackNumbersList();
                        
                        //Check if slave has some attack, maybe it's a new one
                        if(!subAttacksNumbers.isEmpty()){ 
                            
                            boolean finished = true;
                            
                            //Checking if some subAttack didn't finish, maybe it have already finished its job
                            //So it can receive the redistribution
                            for (Integer subAttackNumber : subAttacksNumbers) {
                                finished &= getSubAttackControl(subAttackNumber).isDone();
                            }
                            
                            if(!finished){
                                //If some job didn't finish, so it fall
                                downSlaves.put(id, slave);
                            
                                try{
                                    removeSlave(id);
                                }
                                catch(RemoteException e){
                                    System.err.println("Monitoring error:\n" + e.getMessage());
                                }
                            }
                        }
                    }
                }
            
                if(!downSlaves.isEmpty()){
                    redistributionJobs(downSlaves, callback);
                }
            }
        }
    }
    
    /**
     * Estrutura de Controle de Ataque. 
     * Responsável por gerenciar um ataque.
     */ 
    
    public class AttackControl {

        private Map<Integer, SubAttackControl> subAttacksMap;
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

        public Map<Integer, SubAttackControl> getSubAttacksMap() {
            return subAttacksMap;
        }

        public void setSubAttacksMap(Map<Integer, SubAttackControl> subAttacksMap) {
            this.subAttacksMap = subAttacksMap;
        }

        public synchronized boolean isDone() {
            boolean finished = true;

            synchronized(subAttacksMap){

                for (SubAttackControl subAttack : subAttacksMap.values()) {
                    finished &= subAttack.isDone();
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
            this.subAttacksMap = new HashMap<>();
        }
    }

    /**
     * Estrutura de controle de subataque. 
     * Responsável por armazenar informações de cada subataque.
     */ 
    public class SubAttackControl {

        private final long lastIndex;
        private long currentIndex;
        private boolean done;

        public long getCurrentIndex() {
            return currentIndex;
        }

        public void setCurrentIndex(long currentCheck) {
            this.currentIndex = (currentCheck > currentIndex) ? currentCheck : this.currentIndex;

            if(currentCheck == lastIndex){
                setDone(true);
            }  
        }

        public long getLastIndex() {
            return lastIndex;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        SubAttackControl(long curr, long lc)
        {
            this.currentIndex = curr;
            this.lastIndex = lc;
            this.done = false;     
        }
    }
    
    /**
     * Estrutura de controle de escravo. 
     * Responsável por armazenar informações de cada escravo.
     */ 
    public class SlaveControl {

        private final Slave slaveRef;
        private final String name;
        private double time;
        private List<Integer> subAttackNumbersList;

        public Slave getSlaveRef() {
            return slaveRef;
        }

        public String getName() {
            return name;
        }

        public double getTime() {
            return time;
        }

        public synchronized void setTime(double t) {
            this.time = (t > this.time) ? t : this.time;
        }

        public List<Integer> getSubAttackNumbersList() {
            return subAttackNumbersList;
        }

        SlaveControl(Slave s, String n) {
            this.slaveRef = s;
            this.name = n;
            this.time = System.nanoTime();
            this.subAttackNumbersList = new ArrayList<>();
        }
    }
}