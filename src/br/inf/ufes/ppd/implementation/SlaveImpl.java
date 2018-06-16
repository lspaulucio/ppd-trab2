package br.inf.ufes.ppd.implementation;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SubAttackJob;
import br.inf.ufes.ppd.utils.Crypto;
import br.inf.ufes.ppd.utils.FileTools;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.jms.Queue;
import javax.jms.*;

/** Slave implementation
 *
 * @author Leonardo Santos Paulucio
 */

public class SlaveImpl implements Slave {


    private static List<String> keys;
    private UUID uid;
    private String slaveName;
    private Queue guessesQueue;
    private Queue subAttacksQueue;
    private JMSConsumer consumer;
    private JMSProducer producer;
    private JMSContext context;

    public String getSlaveName() {
        return slaveName;
    }

    public void setSlaveName(String slaveName) {
        this.slaveName = slaveName;
    }

    public Queue getGuessesQueue() {
        return guessesQueue;
    }

    public void setGuessesQueue(Queue guessesQueue) {
        this.guessesQueue = guessesQueue;
    }

    public Queue getSubAttacksQueue() {
        return subAttacksQueue;
    }

    public void setSubAttacksQueue(Queue subAttacksQueue) {
        this.subAttacksQueue = subAttacksQueue;
    }

    public JMSConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(JMSConsumer consumer) {
        this.consumer = consumer;
    }

    public JMSProducer getProducer() {
        return producer;
    }

    public void setProducer(JMSProducer producer) {
        this.producer = producer;
    }

    public JMSContext getContext() {
        return context;
    }

    public void setContext(JMSContext context) {
        this.context = context;
    }

    public SlaveImpl() {
        keys = new ArrayList<>();
    }
    
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
     * @param job Novo subataque.
     */
    public void startSubAttack(SubAttackJob job){
            
        long currentIndex = job.getInitialIndex();
        long finalIndex = job.getFinalIndex();
        int attackNumber = job.getAttackID();
        byte[] ciphertext = job.getCypherText();
        byte[] knowntext = job.getKnowText();        
        
        System.out.println("New SubAttack: " + attackNumber);

        //Subattack execution
        for (; currentIndex <= finalIndex; currentIndex++) {

            try {
                String actualKey = keys.get((int) currentIndex); //Get current key

                byte[] decrypted = Crypto.decrypter(actualKey.getBytes(), ciphertext);

                //Checking if known text exists in decrypted text
                if (Crypto.contains(knowntext, decrypted)) {

                    Guess currentGuess = new Guess();
                    currentGuess.setKey(actualKey);
                    currentGuess.setMessage(decrypted);

                    ObjectMessage guessMessage = context.createObjectMessage(currentGuess);
                    guessMessage.setIntProperty("SubAttackID", attackNumber);
                    guessMessage.setStringProperty("Discoverer", slaveName);
                    guessMessage.setBooleanProperty("Done", false);
                    producer.send((Destination) guessesQueue, guessMessage);
                    
                    System.out.println("SubAttack: " + attackNumber + " Key found: " + actualKey);
                }

            } catch (javax.crypto.BadPaddingException e) {
                // essa excecao e jogada quando a senha esta incorreta
                // porem nao quer dizer que a senha esta correta se nao jogar essa excecao
                //System.err.println("Senha " + new String(key) + " invalida.");
            } catch (JMSException e) {
                System.err.println("Error subattack service:\n" + e.getMessage());
            }
        }
        
        //Sending last message for this job
        try{
            ObjectMessage guessMessage = context.createObjectMessage();
            guessMessage.setIntProperty("SubAttackID", attackNumber);
            guessMessage.setStringProperty("Discoverer", slaveName);
            guessMessage.setBooleanProperty("Done", true);
            producer.send((Destination) guessesQueue, guessMessage);
        }
        catch(JMSException e){
            System.err.println("Error");
        }

        System.out.println("End subattack: " + attackNumber);
    }
    
    public void getJob(){

        Message msg = consumer.receive();
        if(msg instanceof ObjectMessage){
            System.out.println("New job received");
            ObjectMessage obj = (ObjectMessage) msg;
            try{
                SubAttackJob job = (SubAttackJob) obj.getObject();
                startSubAttack(job);                
            }
            catch(JMSException e){
                System.err.println("Consume error \n" + e.getMessage());
            }
        }        
    }
}