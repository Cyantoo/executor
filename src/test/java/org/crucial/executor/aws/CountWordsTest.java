package org.crucial.executor.aws;

import org.crucial.executor.IterativeRunnable;
import org.crucial.executor.ServerlessExecutorService;
import org.testng.annotations.Test;


import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.LinkedList;
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

public class CountWordsTest {

    // Exception qui revient souvent : java.io.StreamCorruptedException: invalid stream header: 7ABAE8AC

    
    @Test
    public void testCountWords() throws InterruptedException, ExecutionException{
        // see AdvancedTest to open multiple threads
        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(false);

        final String document = getURLContent("https://en.wikipedia.org/wiki/C_(programming_language)");
        final String word = "language";
        

        @SuppressWarnings("unchecked")
        Future<Integer> future = es.submit((Serializable & Callable<Integer>) (Document document) -> { 
                return( (Integer) countWords(word, document));
        });
        Integer toPrint = future.get();
        System.out.println(toPrint.toString());
        assert toPrint == countWords(word, content);
    }
    

    /@Test
    public void testCountWordsParallel() throws InterruptedException, ExecutionException{
        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(false);

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

        List<Callable<Integer>> myTasks = Collections.synchronizedList(new ArrayList<>()); //peut-être à changer pour utiliser invokeAll
        IntStream.range(0, nThreads).forEach( j ->
            myTasks.add((Serializable & Callable<Integer>) () -> {
            Integer counter = 0;
            String part = document.substring(bornes_inf[j], bornes_sup[j]);
            String[] words = part.split("[ .,?!]+"); 
            for(int i = 0; i < words.length; i++)
            {
                if(word.equals(words[i])) counter++ ;
            }
            return(counter);
            }));
        Integer sum = 0;

        // plutôt utiliser invokeAll;
        List<Future<Integer>> futures = es.invokeAll(myTasks);
        for (Future<Integer> future : futures){
            sum += future.get();
        }
        System.out.println(sum.toString());
        assert sum == countWords(word, document);
    }
    


    
    static int countWords(String word, String content)
    {
        int counter = 0;
        String[] words = content.split("[ .,?!]+"); // on peut utiliser la regex "[ .,?!]+" si besoin de regex
        for(int i = 0; i < words.length; i++)
        {
            if(word.equals(words[i])) counter++ ;
        }
        return counter;
        
    }

    // Essayer à part
    String getURLContent(String urlToRead)
    {
        // source : https://stackoverflow.com/questions/11087163/how-to-get-url-html-contents-to-string-in-java
        String document = "";

        try {
            // get URL content
            URL url = new URL(urlToRead);
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

  