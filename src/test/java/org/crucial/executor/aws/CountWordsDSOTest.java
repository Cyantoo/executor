package org.crucial.executor.aws;

import org.crucial.executor.ServerlessExecutorService;
import org.testng.annotations.Test;


import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

// for handling webpages
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

// for interfacing with DSO
import org.crucial.dso.AtomicInteger;
import org.crucial.dso.client.Client;

@SuppressWarnings("all")
public class CountWordsDSOTest {


    @Test
    public void testCountWordsDSO() throws InterruptedException, ExecutionException{
        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(true);

        final String document = getURLContent("https://en.wikipedia.org/wiki/C_(programming_language)");
        final String word = "language";


        int nThreads = 10;
        int sizeOneThread = (document.length() + nThreads -1)/nThreads;
        // Define boundaries for each thread
        int[] bornes_inf = new int[nThreads];
        int[] bornes_sup = new int[nThreads];
        for(int i = 0; i< nThreads; i++){
            bornes_inf[i] = i * sizeOneThread;
            bornes_sup[i] = (i+1) * sizeOneThread;
            if(bornes_sup[i]>document.length()) bornes_sup[i] = document.length();
        }

        long id = System.nanoTime();
        String server = "35.221.41.87:11222";

        // Factory factory = Factory.get( server, id);
        Client client = Client.getClient(server, id);
        
        AtomicInteger counter = client.getAtomicInt("counter"); // This doesn't work with ("counter", 0) as argument
        // org.infinispan.commons.CacheException: class org.crucial.dso.AtomicInteger no constructor with [counter, 0] -> like it tries to run it locally after being marshalled?

        List<Callable<Integer>> myTasks = Collections.synchronizedList(new ArrayList<>()); 

        IntStream.range(0, nThreads).forEach( j ->
            myTasks.add((Serializable & Callable<Integer>) () -> {
            String part = document.substring(bornes_inf[j], bornes_sup[j]);
            int count = 0;
            String[] words = part.split("[ .,?!]+"); 
            for(int i = 0; i < words.length; i++)
            {
                if(word.equals(words[i])) counter.getAndIncrement() ;
            }
            return(1);
            }));
        List<Future<Integer>> futures = es.invokeAll(myTasks);
        for (Future<Integer> future : futures){
            try{
                System.out.println(future.get());
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
            
        }
        Integer sum = (Integer) counter.get();
        System.out.println(sum.toString());
        assert sum == count_words(word, document);

    }

    int count_words(String word, String content)
    {
        int counter = 0;
        String[] words = content.split("[ .,?!]+"); // on peut utiliser la regex "[ .,?!]+" si besoin de regex
        for(int i = 0; i < words.length; i++)
        {
            if(word.equals(words[i])) counter++ ;
        }
        return counter;
        
    }

    String getURLContent(String url_to_read)
    {
        // source : https://stackoverflow.com/questions/11087163/how-to-get-url-html-contents-to-string-in-java
        String document = "";

        try {
            // get URL content
            URL url = new URL(url_to_read);
            URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                               new InputStreamReader(conn.getInputStream()));

            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                    document += inputLine;
            }
            br.close();
            // System.out.println(document);

            // System.out.println("Done");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return(document);

    }
    

  }