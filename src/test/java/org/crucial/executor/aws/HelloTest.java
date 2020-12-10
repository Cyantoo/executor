package org.crucial.executor.aws;

import org.crucial.executor.IterativeRunnable;
import org.crucial.executor.ServerlessExecutorService;
import org.testng.annotations.Test;


import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HelloTest {
    @Test
    public void testHello() throws InterruptedException, ExecutionException{
        final String helloWorld = "Hello world";
        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(false);

        
        Future<String> future = es.submit((Serializable & Callable<String>) () -> {
            return helloWorld;
        });

        String toPrint = future.get();
        System.out.println(toPrint);
        assert toPrint.equals(helloWorld);
    } 

  }